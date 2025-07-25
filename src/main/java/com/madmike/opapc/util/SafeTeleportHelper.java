package com.madmike.opapc.util;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

public class SafeTeleportHelper {
    /**
     * Safely teleports the player to the target chunk by:
     * - Force-loading the chunk
     * - Finding the top Y at (chunk center) using Heightmap
     * - Ensuring 2 blocks of air above
     * - Teleporting the player safely
     *
     * @param player the player to teleport
     * @param targetChunk the chunk to teleport to
     * @return true if teleported, false if teleport failed (chunk not available)
     */
    public static boolean teleportPlayerToChunk(ServerPlayer player, ChunkPos targetChunk) {
        ServerLevel level = player.serverLevel();
        int x = targetChunk.getMiddleBlockX();
        int z = targetChunk.getMiddleBlockZ();

        // Force-load the chunk, generating if needed
        ChunkAccess chunk = level.getChunk(targetChunk.x, targetChunk.z, ChunkStatus.FULL, true);
        if (chunk == null) {
            return false; // Could not load the chunk
        }

        // Get surface Y at the position
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);

        // Check safety: ensure 2 air blocks above
        if (!level.isEmptyBlock(pos.above()) || !level.isEmptyBlock(pos.above(2))) {
            // Try to scan upward for the next safe spot, up to build height
            boolean found = false;
            for (int newY = y + 1; newY < level.getMaxBuildHeight() - 2; newY++) {
                BlockPos checkPos = new BlockPos(x, newY, z);
                if (level.getBlockState(checkPos).isSolid() &&
                        level.isEmptyBlock(checkPos.above()) &&
                        level.isEmptyBlock(checkPos.above(2))) {
                    pos = checkPos.above();
                    found = true;
                    break;
                }
            }
            if (!found) {
                // Failed to find a safe spot, abort
                return false;
            }
        }

        // Teleport the player
        player.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), player.getXRot());
        return true;
    }

    public static void teleportPlayer(ServerPlayer player) {
        ServerLevel world = OPAPC.getServer().overworld();
        RandomSource rand = world.getRandom();

        // Player's current chunk coords
        BlockPos playerPos = player.blockPosition();
        int baseChunkX = playerPos.getX() >> 4;
        int baseChunkZ = playerPos.getZ() >> 4;

        for (int i = 0; i < OPAPCConfig.attemptLimit; i++) {
            // pick a random chunk offset in the donut [2–4]
            int dx = rand.nextInt(MAX_CHUNK_RADIUS * 2 + 1) - MAX_CHUNK_RADIUS;
            int dz = rand.nextInt(MAX_CHUNK_RADIUS * 2 + 1) - MAX_CHUNK_RADIUS;
            int chebyshev = Math.max(Math.abs(dx), Math.abs(dz));
            if (chebyshev < MIN_CHUNK_RADIUS || chebyshev > MAX_CHUNK_RADIUS) {
                continue; // outside our 2–4 ring
            }

            int targetChunkX = baseChunkX + dx;
            int targetChunkZ = baseChunkZ + dz;

            // pick a random block within that chunk
            int x = (targetChunkX << 4) + rand.nextInt(16);
            int z = (targetChunkZ << 4) + rand.nextInt(16);

            // find the surface y (first block that blocks motion)
            BlockPos surface = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos spawnPos = surface.above();

            // must be able to see sky
            if (!world.canSeeSky(spawnPos)) continue;

            // ground must be solid and not liquid
            BlockState ground = world.getBlockState(surface);
            if (!ground.getValue().isSolid() || ground.getMaterial().isLiquid()) continue;

            // need two blocks of air at spawnPos and spawnPos.above()
            if (!world.isEmptyBlock(spawnPos) || !world.isEmptyBlock(spawnPos.above())) continue;

            // looks good — do the teleport
            // teleportTo(ServerLevel, x, y, z, yaw, pitch)
            player.teleportTo(
                    world,
                    spawnPos.getX() + 0.5,
                    spawnPos.getY(),
                    spawnPos.getZ() + 0.5,
                    player.getYRot(),
                    player.getXRot()
            );
            return;
        }

        // if we get here, no safe spot found after many tries
        player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("Could not find a safe random spot. Try again!")
        );
    }

    public void teleportPlayerOutsideClaim
}
