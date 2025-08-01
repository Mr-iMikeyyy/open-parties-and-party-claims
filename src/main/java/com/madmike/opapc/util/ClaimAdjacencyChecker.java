package com.madmike.opapc.util;

import net.minecraft.world.level.ChunkPos;

import java.util.*;

public class ClaimAdjacencyChecker {

    public static boolean wouldBreakAdjacency(
            List<ChunkPos> currentClaims,
            ChunkPos toUnclaim
    ) {

        currentClaims.remove(toUnclaim);
        Set<ChunkPos> visited = new HashSet<>();
        Queue<ChunkPos> toVisit = new ArrayDeque<>();
        ChunkPos start = currentClaims.iterator().next();
        toVisit.add(start);

        while (!toVisit.isEmpty()) {
            ChunkPos current = toVisit.poll();
            if (!visited.add(current)) continue;

            for (ChunkPos neighbor : getNeighbors(current)) {
                if (currentClaims.contains(neighbor) && !visited.contains(neighbor)) {
                    toVisit.add(neighbor);
                }
            }
        }

        return visited.size() != currentClaims.size(); // True if disconnected after removal
    }

    public static boolean isNotAdjacentToExistingClaim(
            List<ChunkPos> currentClaims,
            ChunkPos targetChunk
    ) {

        for (ChunkPos neighbor : getNeighbors(targetChunk)) {
            if (currentClaims.contains(neighbor)) {
                return false;
            }
        }

        return true;
    }

    private static List<ChunkPos> getNeighbors(ChunkPos pos) {
        return List.of(
                new ChunkPos(pos.x + 1, pos.z),
                new ChunkPos(pos.x - 1, pos.z),
                new ChunkPos(pos.x, pos.z + 1),
                new ChunkPos(pos.x, pos.z - 1)
        );
    }
}
