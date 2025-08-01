package com.madmike.opapc.util;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.*;

public class SafeWarpFinder {
    public static BlockPos findSafeSpawnOutsideClaim(PartyClaim claim) {
        Set<ChunkPos> claimed = new HashSet<>(claim.getClaimedChunksList());
        Random rand = new Random();

        // Pick a random claimed chunk as starting point
        List<ChunkPos> claimedList = new ArrayList<>(claimed);
        if (claimedList.isEmpty()) return null;
        ChunkPos startChunk = claimedList.get(rand.nextInt(claimedList.size()));

        // Directions to search outward
        ChunkPos[] directions = {
                new ChunkPos(1, 0),   // east
                new ChunkPos(-1, 0),  // west
                new ChunkPos(0, 1),   // south
                new ChunkPos(0, -1)   // north
        };

        // Shuffle search directions
        List<ChunkPos> dirList = Arrays.asList(directions);
        Collections.shuffle(dirList);

        for (ChunkPos dir : dirList) {
            ChunkPos check = startChunk;
            for (int distance = 2; distance <= 8; distance++) { // search up to 8 chunks away
                check = new ChunkPos(check.x + dir.x, check.z + dir.z);

                if (!claimed.contains(check)) {
                    // Found an unclaimed chunk
                    int baseX = check.x << 4;
                    int baseZ = check.z << 4;

                    // Try 20 random positions inside the chunk
                    for (int i = 0; i < 20; i++) {
                        int x = baseX + rand.nextInt(16);
                        int z = baseZ + rand.nextInt(16);
                        int startY = OPAPC.getServer().overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

                        BlockPos pos = findSafeY( x, startY, z);
                        if (pos != null) {
                            return pos;
                        }
                    }
                }
            }
        }
        return null; // nothing found
    }

    public static BlockPos findSafeSpawnInsideClaim(PartyClaim claim) {

        Random rand = new Random();
        // Pick a random claimed chunk
        List<ChunkPos> claimedChunks = new ArrayList<>(claim.getClaimedChunksList());
        ChunkPos chosenChunk = claimedChunks.get(rand.nextInt(claimedChunks.size()));

        // Convert chunk to world coordinates
        int baseX = chosenChunk.x << 4;
        int baseZ = chosenChunk.z << 4;

        BlockPos safePos = null;

        // Try up to 50 times to find a safe spot
        for (int attempt = 0; attempt < 50 && safePos == null; attempt++) {
            int x = baseX + rand.nextInt(16);
            int z = baseZ + rand.nextInt(16);

            // Start at terrain surface height
            int startY = OPAPC.getServer().overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            safePos = findSafeY(x, startY, z);
        }
        return null; // nothing found
    }

    public static BlockPos findSafeY(int x, int startY, int z) {
        // Check downward from startY
        ServerLevel level = OPAPC.getServer().overworld();
        for (int y = startY; y > level.getMaxBuildHeight(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState below = level.getBlockState(pos.below());
            BlockState block = level.getBlockState(pos);
            BlockState above = level.getBlockState(pos.above());

            if (below.isSolid() && block.isAir() && above.isAir() && level.canSeeSky(pos)) {
                return pos;
            }
        }
        return null;
    }
}
