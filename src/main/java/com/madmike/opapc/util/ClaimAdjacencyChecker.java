package com.madmike.opapc.util;

import com.madmike.opapc.OPAPC;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.*;

public class ClaimAdjacencyChecker {

    public static boolean wouldBreakAdjacency(
            ChunkPos toUnclaim,
            List<ChunkPos> currentClaims
    ) {
        if (!currentClaims.contains(toUnclaim)) {
            return true;
        }
        currentClaims.remove(toUnclaim);

        if (currentClaims.isEmpty()) {
            return false;
        }

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
            UUID playerId,
            ChunkPos targetChunk
    ) {
        List<ChunkPos> claimList = new ArrayList<>();

        OPAPC.getClaimsManager().getPlayerInfo(playerId).getDimension(Level.OVERWORLD.location()).getStream().forEach(e -> e.getStream().forEach(claimList::add));

        for (ChunkPos neighbor : getNeighbors(targetChunk)) {
            if (claimList.contains(neighbor)) {
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
