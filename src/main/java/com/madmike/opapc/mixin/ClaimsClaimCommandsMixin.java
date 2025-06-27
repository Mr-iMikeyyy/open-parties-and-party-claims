package com.madmike.opapc.mixin;

import com.madmike.opapc.data.parties.claims.PartyClaim;
import com.madmike.opapc.features.block.PartyClaimBlock;
import com.madmike.opapc.util.claim.ClaimAdjacencyChecker;
import com.madmike.opapc.util.claim.ClaimContextHolder;
import com.madmike.opapc.components.OPAPCComponents;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.pac.common.claims.player.IPlayerChunkClaim;
import xaero.pac.common.claims.player.IPlayerClaimInfo;
import xaero.pac.common.claims.player.IPlayerClaimPosList;
import xaero.pac.common.claims.player.IPlayerDimensionClaims;
import xaero.pac.common.claims.player.api.IPlayerClaimPosListAPI;
import xaero.pac.common.parties.party.IPartyPlayerInfo;
import xaero.pac.common.parties.party.ally.IPartyAlly;
import xaero.pac.common.parties.party.member.IPartyMember;
import xaero.pac.common.server.IServerData;
import xaero.pac.common.server.ServerData;
import xaero.pac.common.server.claims.IServerClaimsManager;
import xaero.pac.common.server.claims.IServerDimensionClaimsManager;
import xaero.pac.common.server.claims.IServerRegionClaims;
import xaero.pac.common.server.claims.command.ClaimsClaimCommands;
import xaero.pac.common.server.claims.player.IServerPlayerClaimInfo;
import xaero.pac.common.server.parties.party.IServerParty;
import xaero.pac.common.server.player.data.ServerPlayerData;
import xaero.pac.common.server.player.data.api.ServerPlayerDataAPI;

import java.util.ArrayList;
import java.util.List;

import static com.madmike.opapc.util.claim.NetherClaimAdjuster.mirrorOverworldClaimsToNether;

@Mixin(ClaimsClaimCommands.class)
public abstract class ClaimsClaimCommandsMixin {

    @Inject(method = "createClaimCommand", at = @At("HEAD"))
    private static void captureClaimContext(ArgumentBuilder<ServerCommandSource, ?> builder, boolean shouldClaim, boolean serverClaim, boolean opReplaceCurrent, CallbackInfoReturnable<ArgumentBuilder<ServerCommandSource, ?>> cir) {
        ClaimContextHolder.SHOULD_CLAIM.set(shouldClaim);
    }

    @Inject(method = "createClaimCommand", at = @At("RETURN"))
    private static void clearClaimContext(CallbackInfoReturnable<?> cir) {
        ClaimContextHolder.SHOULD_CLAIM.remove();
    }

    @Redirect(
            method = "createClaimCommand",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/builder/ArgumentBuilder;executes(Lcom/mojang/brigadier/Command;)Lcom/mojang/brigadier/builder/ArgumentBuilder;"
            )
    )
    private static ArgumentBuilder<ServerCommandSource, ?> redirectClaimExecutes(
            ArgumentBuilder<ServerCommandSource, ?> builder,
            Command<ServerCommandSource> originalCommand
    ) {
        return builder.executes(context -> {
            //get whether it's claim or unclaim
            boolean isClaim = ClaimContextHolder.SHOULD_CLAIM.get();
            //get the player
            ServerPlayerEntity player = context.getSource().getPlayer();
            //get the server
            MinecraftServer server = context.getSource().getServer();
            //get opac server data
            IServerData<IServerClaimsManager<IPlayerChunkClaim, IServerPlayerClaimInfo<IPlayerDimensionClaims<IPlayerClaimPosList>>, IServerDimensionClaimsManager<IServerRegionClaims>>, IServerParty<IPartyMember, IPartyPlayerInfo, IPartyAlly>> serverData = ServerData.from(server);
            //get player data
            ServerPlayerData playerData;
            if (player != null) {
                playerData = (ServerPlayerData) ServerPlayerDataAPI.from(player);
            }
            else {
                return 0;
            }


            if (playerData.isClaimsServerMode() || playerData.isClaimsAdminMode()) {
                return originalCommand.run(context);
            }

            IServerClaimsManager<IPlayerChunkClaim, IServerPlayerClaimInfo<IPlayerDimensionClaims<IPlayerClaimPosList>>, IServerDimensionClaimsManager<IServerRegionClaims>> cm = serverData.getServerClaimsManager();
            var party = serverData.getPartyManager().getPartyByMember(player.getUuid());

            if (party == null) {
                context.getSource().sendError(Text.literal("Must be in a party and it's leader to claim chunks."));
                return 0;
            }

            if (!party.getOwner().getUUID().equals(player.getUuid())) {
                context.getSource().sendError(Text.literal("Only the party leader can claim or unclaim chunks."));
                return 0;
            }

            ChunkPos target = player.getChunkPos();

            if (isClaim) {
                if (!ClaimAdjacencyChecker.isAdjacentToExistingClaim(party, player.getWorld().getRegistryKey(), target, cm)) {
                    context.getSource().sendError(Text.literal("Claim must be adjacent to an existing party claim."));
                    return 0;
                }
            } else {
                if (!ClaimAdjacencyChecker.wouldBreakAdjacency(party, player.getWorld().getRegistryKey(), target, cm)) {
                    context.getSource().sendError(Text.literal("You cannot unclaim a chunk that would split your party's territory."));
                    return 0;
                }
            }

            PartyClaim partyClaim = OPAPCComponents.PARTY_CLAIMS.get(server.getScoreboard()).getClaim(party.getId());
            if (partyClaim == null) {
                context.getSource().sendError(Text.literal("Your first claim is made by placing a Party Claim Block. Careful where you place it! It's position is semi permanent!"));
                return 0;
            }
            int unlockedPartyClaims = partyClaim.getBoughtClaims();
            IPlayerClaimInfo<IPlayerDimensionClaims<IPlayerClaimPosList>> info = cm.getPlayerInfo(player.getUuid());
            int totalOverworldClaims = info.getDimension(World.OVERWORLD.getValue())
                    .getStream()
                    .mapToInt(IPlayerClaimPosListAPI::getCount)
                    .sum();

            if (totalOverworldClaims >= unlockedPartyClaims) {
                context.getSource().sendError(Text.literal("You've ran out of party claims"));
                return 0;
            }

            int result = originalCommand.run(context);

            //  Inject post-success logic only if the claim went through
            if (result > 0) {
                IPlayerClaimInfo<IPlayerDimensionClaims<IPlayerClaimPosList>> newInfo = cm.getPlayerInfo(player.getUuid());
                List<ChunkPos> overworldChunks = new ArrayList<>();
                newInfo.getDimension(World.OVERWORLD.getValue()).getStream().forEach(e -> e.getStream().forEach(overworldChunks::add));
                List<ChunkPos> currentNetherChunks = new ArrayList<>();
                newInfo.getDimension(World.NETHER.getValue()).getStream().forEach(e -> e.getStream().forEach(overworldChunks::add));

                mirrorOverworldClaimsToNether(cm, party.getId(), overworldChunks, currentNetherChunks);
            }

            return result;
        });
    }
}
