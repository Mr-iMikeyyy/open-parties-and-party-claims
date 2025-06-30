package com.madmike.opapc.mixin;

import com.madmike.opapc.data.parties.claims.PartyClaim;
import com.madmike.opapc.util.claim.ClaimAdjacencyChecker;
import com.madmike.opapc.util.claim.ClaimContextHolder;
import com.madmike.opapc.components.OPAPCComponents;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.pac.common.claims.player.api.IPlayerClaimPosListAPI;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;
import xaero.pac.common.server.claims.command.ClaimsClaimCommands;
import xaero.pac.common.server.claims.player.api.IServerPlayerClaimInfoAPI;
import xaero.pac.common.server.player.data.ServerPlayerData;
import xaero.pac.common.server.player.data.api.ServerPlayerDataAPI;

import java.util.ArrayList;
import java.util.List;

import static com.madmike.opapc.util.claim.NetherClaimAdjuster.mirrorOverworldClaimsToNether;

@Mixin(ClaimsClaimCommands.class)
public abstract class ClaimsClaimCommandsMixin {

    @Inject(method = "createClaimCommand", at = @At("HEAD"), remap = false)
    private static void captureClaimContext(ArgumentBuilder<CommandSourceStack, ?> builder,
                                            boolean shouldClaim, boolean serverClaim, boolean opReplaceCurrent,
                                            CallbackInfoReturnable<ArgumentBuilder<CommandSourceStack, ?>> cir) {
        ClaimContextHolder.SHOULD_CLAIM.set(shouldClaim);
    }

    @Inject(method = "createClaimCommand", at = @At("RETURN"), remap = false)
    private static void clearClaimContext(CallbackInfoReturnable<?> cir) {
        ClaimContextHolder.SHOULD_CLAIM.remove();
    }

    @Redirect(
            method = "createClaimCommand",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/brigadier/builder/ArgumentBuilder;executes(Lcom/mojang/brigadier/Command;)Lcom/mojang/brigadier/builder/ArgumentBuilder;"
            ),
            remap = false
    )
    private static ArgumentBuilder<CommandSourceStack, ?> redirectClaimExecutes(
            ArgumentBuilder<CommandSourceStack, ?> builder,
            Command<CommandSourceStack> originalCommand
    ) {
        return builder.executes(context -> {
            boolean isClaim = ClaimContextHolder.SHOULD_CLAIM.get();
            ServerPlayer player = context.getSource().getPlayer();
            MinecraftServer server = context.getSource().getServer();
            OpenPACServerAPI api = OpenPACServerAPI.get(server);

            ServerPlayerData playerData = player != null ? (ServerPlayerData) ServerPlayerDataAPI.from(player) : null;

            if (playerData != null) {
                if (playerData.isClaimsServerMode() || playerData.isClaimsAdminMode()) {
                    return originalCommand.run(context);
                }
            }


            IServerClaimsManagerAPI cm = api.getServerClaimsManager();

            var party = api.getPartyManager().getPartyByMember(player.getUUID());
            if (party == null) {
                context.getSource().sendFailure(Component.literal("Must be in a party and its leader to claim chunks."));
                return 0;
            }
            if (!party.getOwner().getUUID().equals(player.getUUID())) {
                context.getSource().sendFailure(Component.literal("Only the party leader can claim or unclaim chunks."));
                return 0;
            }

            ChunkPos target = player.chunkPosition();

            if (isClaim) {
                if (!ClaimAdjacencyChecker.isAdjacentToExistingClaim(party, player.level().dimension(), target, api)) {
                    context.getSource().sendFailure(Component.literal("Claim must be adjacent to an existing party claim."));
                    return 0;
                }
            } else {
                if (!ClaimAdjacencyChecker.wouldBreakAdjacency(party, player.level().dimension(), target, api)) {
                    context.getSource().sendFailure(Component.literal("You cannot unclaim a chunk that would split your party's territory."));
                    return 0;
                }
            }

            PartyClaim partyClaim = OPAPCComponents.PARTY_CLAIMS.get(server.getScoreboard()).getClaim(party.getId());
            if (partyClaim == null) {
                context.getSource().sendFailure(Component.literal("Your first claim is made by placing a Party Claim Block. Careful where you place it! Its position is semi-permanent!"));
                return 0;
            }

            int unlockedPartyClaims = partyClaim.getBoughtClaims();
            IServerPlayerClaimInfoAPI info = cm.getPlayerInfo(player.getUUID());
            int totalOverworldClaims = info.getDimension(Level.OVERWORLD.location())
                    .getStream()
                    .mapToInt(IPlayerClaimPosListAPI::getCount)
                    .sum();

            if (totalOverworldClaims >= unlockedPartyClaims) {
                context.getSource().sendFailure(Component.literal("You've run out of party claims."));
                return 0;
            }

            int result = originalCommand.run(context);

            if (result > 0) {
                IServerPlayerClaimInfoAPI newInfo = cm.getPlayerInfo(player.getUUID());
                List<ChunkPos> overworldChunks = new ArrayList<>();
                newInfo.getDimension(Level.OVERWORLD.location()).getStream()
                        .forEach(e -> e.getStream().forEach(overworldChunks::add));
                List<ChunkPos> currentNetherChunks = new ArrayList<>();
                newInfo.getDimension(Level.NETHER.location()).getStream()
                        .forEach(e -> e.getStream().forEach(currentNetherChunks::add));

                mirrorOverworldClaimsToNether(cm, party.getId(), overworldChunks, currentNetherChunks);
            }

            return result;
        });
    }
}
