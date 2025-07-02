package com.madmike.opapc.command.commands.claims;

import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.data.parties.claims.PartyClaim;
import com.madmike.opapc.util.claim.ClaimAdjacencyChecker;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.claims.result.api.ClaimResult;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;

import static com.madmike.opapc.util.claim.NetherClaimAdjuster.mirrorOverworldClaimsToNether;

public class PartyUnclaimCommandHandler {
    public static int handlePartyUnclaim(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }

        if (!player.level().dimension().location().equals(Level.OVERWORLD.location())) {
            context.getSource().sendFailure(Component.literal("You are only allowed to claim in the Overworld"));
            return 0;
        }

        MinecraftServer server = context.getSource().getServer();
        OpenPACServerAPI api = OpenPACServerAPI.get(server);

        IServerClaimsManagerAPI cm = api.getServerClaimsManager();

        var party = api.getPartyManager().getPartyByMember(player.getUUID());
        if (party == null) {
            context.getSource().sendFailure(Component.literal("Must be in a party and its leader to unclaim chunks."));
            return 0;
        }
        if (!party.getOwner().getUUID().equals(player.getUUID())) {
            context.getSource().sendFailure(Component.literal("Only the party leader can claim or unclaim chunks."));
            return 0;
        }

        ChunkPos target = player.chunkPosition();

        // Unclaim logic: ensure unclaim does not break adjacency/contiguity
        if (!ClaimAdjacencyChecker.wouldBreakAdjacency(party, player.level().dimension(), target, api)) {
            context.getSource().sendFailure(Component.literal("You cannot unclaim a chunk that would split your party's territory."));
            return 0;
        }

        PartyClaim partyClaim = OPAPCComponents.PARTY_CLAIMS.get(server.getScoreboard()).getClaim(party.getId());
        if (partyClaim == null) {
            context.getSource().sendFailure(Component.literal("You must have an existing claim to unclaim!"));
            return 0;
        }

        // All checks passed, try to unclaim!
        ClaimResult<IPlayerChunkClaimAPI> result = cm.tryToUnclaim(Level.OVERWORLD.location(), player.getUUID(), player.chunkPosition().x, player.chunkPosition().z, player.chunkPosition().x, player.chunkPosition().z, false);

        context.getSource().sendSuccess(() ->
                result.getResultType().message, true
        );

        if (result.getResultType().success) {

            mirrorOverworldClaimsToNether(cm, player);

            return 1;
        }

        return 0;
    }
}
