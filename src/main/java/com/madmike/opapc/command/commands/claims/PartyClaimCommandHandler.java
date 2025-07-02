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
import xaero.pac.common.claims.player.api.IPlayerClaimPosListAPI;
import xaero.pac.common.claims.result.api.ClaimResult;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;
import xaero.pac.common.server.claims.player.api.IServerPlayerClaimInfoAPI;

import static com.madmike.opapc.util.claim.NetherClaimAdjuster.mirrorOverworldClaimsToNether;

public class PartyClaimCommandHandler {
    public static int handlePartyClaim(CommandContext<CommandSourceStack> context) {
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
            context.getSource().sendFailure(Component.literal("Must be in a party and its leader to claim chunks."));
            return 0;
        }
        if (!party.getOwner().getUUID().equals(player.getUUID())) {
            context.getSource().sendFailure(Component.literal("Only the party leader can claim or unclaim chunks."));
            return 0;
        }

        ChunkPos target = player.chunkPosition();


            if (!ClaimAdjacencyChecker.isAdjacentToExistingClaim(party, player.level().dimension(), target, api)) {
                context.getSource().sendFailure(Component.literal("Claim must be adjacent to an existing party claim."));
                return 0;
            }


        PartyClaim partyClaim = OPAPCComponents.PARTY_CLAIMS.get(server.getScoreboard()).getClaim(party.getId());
        if (partyClaim == null) {
            context.getSource().sendFailure(Component.literal("Your first claim is made by placing a Party Claim Block. Careful where you place it! Its position is permanent!"));
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

        // All checks passed, try to claim!
        ClaimResult<IPlayerChunkClaimAPI> result = cm.tryToClaim(Level.OVERWORLD.location(), player.getUUID(), 0, player.chunkPosition().x, player.chunkPosition().z, player.chunkPosition().x, player.chunkPosition().z, false);

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
