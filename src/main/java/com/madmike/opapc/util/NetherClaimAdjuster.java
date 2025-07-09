package com.madmike.opapc.util;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.claims.result.api.ClaimResult;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;

import java.util.*;

public class NetherClaimAdjuster {

    public static void mirrorOverworldClaimsToNether(
            IServerClaimsManagerAPI cm,
            ServerPlayer player
    ) {
        player.sendSystemMessage(Component.literal("[Mirror] Starting mirrorOverworldClaimsToNether"));

        ResourceLocation netherId = Level.NETHER.location();
        player.sendSystemMessage(Component.literal("[Mirror] Obtained nether dimension ID"));

        List<ChunkPos> overworldClaims = new ArrayList<>();
        cm.getPlayerInfo(player.getUUID())
                .getDimension(Level.OVERWORLD.location())
                .getStream()
                .forEach(e -> e.getStream().forEach(overworldClaims::add));

        player.sendSystemMessage(Component.literal("[Mirror] Found " + overworldClaims.size() + " overworld claims"));

        Set<ChunkPos> netherChunksToClaim = new HashSet<>();
        for (ChunkPos owChunk : overworldClaims) {
            int netherX = owChunk.x / 8;
            int netherZ = owChunk.z / 8;
            netherChunksToClaim.add(new ChunkPos(netherX, netherZ));
            player.sendSystemMessage(Component.literal("[Mirror] Mapped Overworld chunk " + owChunk + " -> Nether chunk (" + netherX + ", " + netherZ + ")"));
        }
        List<ChunkPos> netherClaims = new ArrayList<>();
        var dimensionClaims = cm.getPlayerInfo(player.getUUID()).getDimension(Level.NETHER.location());
        if (dimensionClaims != null) {
            dimensionClaims.getStream().forEach(e -> e.getStream().forEach(netherClaims::add));
            player.sendSystemMessage(Component.literal("[Mirror] Found " + netherClaims.size() + " current nether claims"));
        } else {
            player.sendSystemMessage(Component.literal("[Mirror] No current nether claims (null returned)."));
        }

        Set<ChunkPos> currentNetherClaimsSet = new HashSet<>(netherClaims);

        // Remove all nether claims if overworld claims are empty
        if (overworldClaims.isEmpty()) {
            player.sendSystemMessage(Component.literal("[Mirror] Overworld claims empty, removing nether claims"));

            if (netherClaims.isEmpty()) {
                player.sendSystemMessage(Component.literal("[Mirror] Nether claims already empty, nothing to remove"));
                return;
            } else {
                for (ChunkPos pos : netherClaims) {
                    player.sendSystemMessage(Component.literal("[Mirror] Unclaiming nether chunk: " + pos));
                    ClaimResult<IPlayerChunkClaimAPI> result = cm.tryToUnclaim(netherId, player.getUUID(), pos.x, pos.z, pos.x, pos.z, false);
                    player.sendSystemMessage(Component.literal("[Mirror] Unclaim result: " + result.getResultType().message.getString()));
                }
                return;
            }
        }

        // Add missing nether claims
        player.sendSystemMessage(Component.literal("[Mirror] Adding missing nether claims..."));
        for (ChunkPos chunk : netherChunksToClaim) {
            if (!currentNetherClaimsSet.contains(chunk)) {
                player.sendSystemMessage(Component.literal("[Mirror] Attempting to claim nether chunk: " + chunk));
                ClaimResult<IPlayerChunkClaimAPI> result = cm.tryToClaim(netherId, player.getUUID(), 0, chunk.x, chunk.z, chunk.x, chunk.z, false);
                player.sendSystemMessage(Component.literal("[Mirror] Claim result: " + result.getResultType().message.getString()));
            }
        }

        // Remove stale nether claims
        player.sendSystemMessage(Component.literal("[Mirror] Removing stale nether claims..."));
        for (ChunkPos chunk : currentNetherClaimsSet) {
            if (!netherChunksToClaim.contains(chunk)) {
                player.sendSystemMessage(Component.literal("[Mirror] Attempting to unclaim stale nether chunk: " + chunk));
                ClaimResult<IPlayerChunkClaimAPI> result = cm.tryToUnclaim(netherId, player.getUUID(), chunk.x, chunk.z, chunk.x, chunk.z, false);
                player.sendSystemMessage(Component.literal("[Mirror] Unclaim result: " + result.getResultType().message.getString()));
            }
        }

        player.sendSystemMessage(Component.literal("[Mirror] Finished mirrorOverworldClaimsToNether"));
    }
}
