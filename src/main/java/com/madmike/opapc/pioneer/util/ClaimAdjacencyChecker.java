/*
 * Copyright (C) 2025 Mr-iMikeyyy (and contributors)
 *
 * This file is part of OPAPC (Open Parties and Party Claims).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of version 3 of the GNU Lesser General Public License
 * (LGPL-3.0-only) as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.madmike.opapc.pioneer.util;

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
