package com.madmike.opapc.util.claim;


import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockInChunkDetector {

    public static boolean chunkHasBlock(Level level, ChunkPos chunkPos, Block targetBlock) {
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();

        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock().equals(targetBlock)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
