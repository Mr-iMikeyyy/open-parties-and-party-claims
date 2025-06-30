package com.madmike.opapc.util.claim;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class NetherClaimAdjuster {

    public static void mirrorOverworldClaimsToNether(
            IServerClaimsManagerAPI cm,
            UUID playerId,
            List<ChunkPos> overworldChunks,
            List<ChunkPos> currentNetherClaims
    ) {
        ResourceLocation netherId = Level.NETHER.location();

        Set<ChunkPos> netherChunksToClaim = new HashSet<>();
        Set<ChunkPos> currentClaimsSet = new HashSet<>(currentNetherClaims);

        for (ChunkPos owChunk : overworldChunks) {
            int netherX = owChunk.x >> 3;
            int netherZ = owChunk.z >> 3;
            netherChunksToClaim.add(new ChunkPos(netherX, netherZ));
        }

        for (ChunkPos chunk : netherChunksToClaim) {
            if (!currentClaimsSet.contains(chunk)) {
                cm.claim(netherId, playerId, 0, chunk.x, chunk.z, false);
            }
        }

        for (ChunkPos chunk : currentClaimsSet) {
            if (!netherChunksToClaim.contains(chunk)) {
                cm.unclaim(netherId, chunk.x, chunk.z);
            }
        }
    }
}
