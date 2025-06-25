package com.madmike.opapc.util.claim;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public class BlockInChunkDetector {
    public static boolean chunkHasBlock(World world, ChunkPos chunkPos, Block targetBlock) {
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();

        // Y range depends on world height. You can optimize if you know where the block might exist.
        int minY = world.getBottomY();
        int maxY = world.getTopY();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                    BlockState state = world.getBlockState(pos);
                    if (state.getBlock().equals(targetBlock)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
