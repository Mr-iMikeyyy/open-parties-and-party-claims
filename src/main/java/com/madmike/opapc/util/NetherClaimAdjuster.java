package com.madmike.opapc.util;

import com.madmike.opapc.OPAPC;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.claims.result.api.ClaimResult;

import java.util.*;

public class NetherClaimAdjuster {

    public static void mirrorOverworldClaimsToNether(
            UUID playerId
    ) {
        ResourceLocation netherId = Level.NETHER.location();

        List<ChunkPos> overworldClaims = new ArrayList<>();
        OPAPC.getClaimsManager().getPlayerInfo(playerId)
                .getDimension(Level.OVERWORLD.location())
                .getStream()
                .forEach(e -> e.getStream().forEach(overworldClaims::add));


        Set<ChunkPos> netherChunksToClaim = new HashSet<>();
        for (ChunkPos owChunk : overworldClaims) {
            int netherX = owChunk.x / 8;
            int netherZ = owChunk.z / 8;
            netherChunksToClaim.add(new ChunkPos(netherX, netherZ));
        }
        List<ChunkPos> netherClaims = new ArrayList<>();
        var dimensionClaims = OPAPC.getClaimsManager().getPlayerInfo(playerId).getDimension(Level.NETHER.location());
        if (dimensionClaims != null) {
            dimensionClaims.getStream().forEach(e -> e.getStream().forEach(netherClaims::add));
        }

        Set<ChunkPos> currentNetherClaimsSet = new HashSet<>(netherClaims);

        // Remove all nether claims if overworld claims are empty
        if (overworldClaims.isEmpty()) {
            if (!netherClaims.isEmpty()) {
                for (ChunkPos pos : netherClaims) {
                    OPAPC.getClaimsManager().tryToUnclaim(netherId, playerId, pos.x, pos.z, pos.x, pos.z, false);
                }
            }
            return;
        }

        // Add missing nether claims
        for (ChunkPos chunk : netherChunksToClaim) {
            if (!currentNetherClaimsSet.contains(chunk)) {
                OPAPC.getClaimsManager().tryToClaim(netherId, playerId, 0, chunk.x, chunk.z, chunk.x, chunk.z, false);
            }
        }

        // Remove stale nether claims
        for (ChunkPos chunk : currentNetherClaimsSet) {
            if (!netherChunksToClaim.contains(chunk)) {
                OPAPC.getClaimsManager().tryToUnclaim(netherId, playerId, chunk.x, chunk.z, chunk.x, chunk.z, false);
            }
        }
    }
}
