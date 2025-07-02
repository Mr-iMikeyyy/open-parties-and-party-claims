package com.madmike.opapc.util.claim;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;

import java.util.*;

public class NetherClaimAdjuster {

    public static void mirrorOverworldClaimsToNether(
            IServerClaimsManagerAPI cm,
            ServerPlayer player
    ) {
        ResourceLocation netherId = Level.NETHER.location();

        List<ChunkPos> overworldClaims = new ArrayList<>();
        cm.getPlayerInfo(player.getUUID())
                .getDimension(Level.OVERWORLD.location())
                .getStream()
                .forEach(e -> e.getStream().forEach(overworldClaims::add));

        Set<ChunkPos> netherChunksToClaim = new HashSet<>();
        for (ChunkPos owChunk : overworldClaims) {
            int netherX = owChunk.x >> 3;
            int netherZ = owChunk.z >> 3;
            netherChunksToClaim.add(new ChunkPos(netherX, netherZ));
        }

        List<ChunkPos> netherClaims = new ArrayList<>();
        cm.getPlayerInfo(player.getUUID())
                .getDimension(Level.NETHER.location())
                .getStream()
                .forEach(e -> e.getStream().forEach(netherClaims::add));
        Set<ChunkPos> currentNetherClaimsSet = new HashSet<>(netherClaims);

        // Add new claims
        for (ChunkPos chunk : netherChunksToClaim) {
            if (!currentNetherClaimsSet.contains(chunk)) {
                cm.claim(netherId, player.getUUID(), 0, chunk.x, chunk.z, false);
            }
        }
        // Remove stale claims
        for (ChunkPos chunk : currentNetherClaimsSet) {
            if (!netherChunksToClaim.contains(chunk)) {
                cm.unclaim(netherId, chunk.x, chunk.z);
            }
        }
    }
}
