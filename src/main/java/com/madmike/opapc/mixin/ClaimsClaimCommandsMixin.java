package com.madmike.opapc.mixin;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.madmike.opapc.claim.ClaimAdjacencyChecker;
import com.madmike.opapc.claim.ClaimContextHolder;
import com.madmike.opapc.components.OPAPCComponents;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.pac.common.claims.player.IPlayerChunkClaim;
import xaero.pac.common.claims.player.IPlayerClaimPosList;
import xaero.pac.common.claims.player.IPlayerDimensionClaims;
import xaero.pac.common.parties.party.IPartyPlayerInfo;
import xaero.pac.common.parties.party.ally.IPartyAlly;
import xaero.pac.common.parties.party.member.IPartyMember;
import xaero.pac.common.server.IServerData;
import xaero.pac.common.server.ServerData;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.IServerClaimsManager;
import xaero.pac.common.server.claims.IServerDimensionClaimsManager;
import xaero.pac.common.server.claims.IServerRegionClaims;
import xaero.pac.common.server.claims.command.ClaimsClaimCommands;
import xaero.pac.common.server.claims.player.IServerPlayerClaimInfo;
import xaero.pac.common.server.parties.party.IServerParty;
import xaero.pac.common.server.player.config.api.IPlayerConfigAPI;
import xaero.pac.common.server.player.config.api.PlayerConfigOptions;
import xaero.pac.common.server.player.data.ServerPlayerData;
import xaero.pac.common.server.player.data.api.ServerPlayerDataAPI;

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
            boolean isClaim = ClaimContextHolder.SHOULD_CLAIM.get();
            ServerPlayerEntity player = context.getSource().getPlayer();
            MinecraftServer server = context.getSource().getServer();
            IServerData<IServerClaimsManager<IPlayerChunkClaim, IServerPlayerClaimInfo<IPlayerDimensionClaims<IPlayerClaimPosList>>, IServerDimensionClaimsManager<IServerRegionClaims>>, IServerParty<IPartyMember, IPartyPlayerInfo, IPartyAlly>> serverData = ServerData.from(server);
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
                context.getSource().sendError(Text.literal("Must be in a party to claim chunks."));
                return 0;
            }

            if (!party.getOwner().getUUID().equals(player.getUuid())) {
                context.getSource().sendError(Text.literal("Only the party leader can claim or unclaim chunks."));
                return 0;
            }

            // Assign party claims to party leader
            IPlayerConfigAPI iPlayerConfigAPI = OpenPACServerAPI.get(server).getPlayerConfigs().getLoadedConfig(player.getUuid());
            int partyClaims = OPAPCComponents.KNOWN_PARTIES.get(server.getScoreboard()).get(party.getId()).getClaims();
            iPlayerConfigAPI.tryToSet(PlayerConfigOptions.BONUS_CHUNK_CLAIMS, partyClaims); // optional if you're increasing cap

            ChunkPos target = player.getChunkPos();

            // You can re-enable the claim count check if desired
            /*
            if (cm.getPlayerInfo(player.getUuid()).getClaimCount() >= SOME_LIMIT) {
                context.getSource().sendError(Text.literal("Your party has reached its maximum allowed claims."));
                return 0;
            }
            */

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

            return originalCommand.run(context);
        });
    }
}
