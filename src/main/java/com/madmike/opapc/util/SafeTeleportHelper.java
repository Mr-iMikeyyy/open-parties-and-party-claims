package com.madmike.opapc.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
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
}
