package com.madmike.opapc.war2.util;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import com.madmike.opapc.util.ClaimAdjacencyChecker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

public class WarBlockSpawner {
    public static BlockPos spawnWarBlock(PartyClaim claim) {
        List<ChunkPos> ownedChunks = claim.getClaimedChunksList();


        if (ownedChunks.size() > 1) {
            ownedChunks.remove(new ChunkPos(claim.getWarpPos()));
        }

        RandomSource rand = OPAPC.getServer().overworld().getRandom();
        Level world = OPAPC.getServer().overworld();

        BlockPos result = null;
        int attempts = 0;

        while (result == null && attempts < 2000) {
            attempts++;

            // 1) Pick random owned chunk and random x/z in that chunk
            ChunkPos chunk = ownedChunks.get(rand.nextInt(ownedChunks.size()));
            if (ClaimAdjacencyChecker.wouldBreakAdjacency(ownedChunks, chunk)) {
                continue;
            }
            int baseX = chunk.x << 4;
            int baseZ = chunk.z << 4;
            int x = baseX + rand.nextInt(16);
            int z = baseZ + rand.nextInt(16);

            // 2) Start Y-search from terrain gen height
            BlockPos columnStart = new BlockPos(x, 0, z);
            int startY = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, columnStart).getY();

            for (int y = startY; y < world.getMaxBuildHeight() - 2; y++) {
                BlockPos groundPos = new BlockPos(x, y, z);
                BlockState ground = world.getBlockState(groundPos);

                // Check ground solidity and fluid
                if (!ground.isFaceSturdy(world, groundPos, Direction.UP) || !world.getFluidState(groundPos).isEmpty()) continue;

                // Check required air blocks above
                boolean spaceClear = true;
                for (int i = 1; i <= 2; i++) {
                    if (!world.isEmptyBlock(groundPos.above(i))) {
                        spaceClear = false;
                        break;
                    }
                }
                if (!spaceClear) continue;

                BlockPos spawnPos = groundPos.above(1);

                // Check sky access
                if (!world.canSeeSky(spawnPos)) continue;

                result = spawnPos;
                break; // found a good spot in this column
            }
        }

        return result;
    }
}
