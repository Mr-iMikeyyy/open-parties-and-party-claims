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

package com.madmike.opapc.war.command.util;

import com.madmike.opapc.pioneer.data.PartyClaim;
import net.minecraft.server.level.ServerPlayer;

public final class WarProximity {

    /** Returns true if the player is close enough to the claim to declare war. */
    public static boolean isWithinLoadingDistance(ServerPlayer player, PartyClaim claim) {
        return isWithinLoadingDistance(player, claim, 0, /*requireLoadedEdge*/ false);
    }

    /**
     * Proximity check in chunk-space against the claim's bounding box.
     *
     * @param marginChunks extra slack on top of view/simulation distance (e.g. 1–2 if you want a tiny buffer)
     * @param requireLoadedEdge if true, also require at least one edge chunk at the nearest point to be currently loaded
     */
    public static boolean isWithinLoadingDistance(
            ServerPlayer player,
            PartyClaim claim,
            int marginChunks,
            boolean requireLoadedEdge
    ) {
        // 1) Dimension check (adjust if your claims can be in other dimensions)
        if (!player.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            return false;
        }

        // 2) Empty claim guard
        java.util.List<net.minecraft.world.level.ChunkPos> chunks = claim.getClaimedChunksList();
        if (chunks == null || chunks.isEmpty()) return false;

        // 3) Compute claim bounds in chunk coords
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (net.minecraft.world.level.ChunkPos cp : chunks) {
            if (cp.x < minX) minX = cp.x;
            if (cp.z < minZ) minZ = cp.z;
            if (cp.x > maxX) maxX = cp.x;
            if (cp.z > maxZ) maxZ = cp.z;
        }

        // 4) Player chunk position
        net.minecraft.world.level.ChunkPos p = player.chunkPosition();

        // 5) Chebyshev distance from a point to an AABB in chunk-space
        //    Distance is 0 if we're inside the box.
        int dx = 0;
        if (p.x < minX) dx = minX - p.x;
        else if (p.x > maxX) dx = p.x - maxX;

        int dz = 0;
        if (p.z < minZ) dz = minZ - p.z;
        else if (p.z > maxZ) dz = p.z - maxZ;

        int chebyshevDistChunks = Math.max(dx, dz);

        // 6) Effective loading radius = max(view, simulation) + margin
        int view = player.server.getPlayerList().getViewDistance();
        int sim  = player.server.getPlayerList().getSimulationDistance();
        int radius = Math.max(view, sim) + Math.max(0, marginChunks);

        if (chebyshevDistChunks > radius) return false;

        // 7) Optional: require that the nearest edge chunk is actually loaded "now" (no loading!)
        if (requireLoadedEdge) {
            int nearestCx = clamp(p.x, minX, maxX);
            int nearestCz = clamp(p.z, minZ, maxZ);
            net.minecraft.server.level.ServerLevel level = player.serverLevel();
            var chunkNow = level.getChunkSource().getChunkNow(nearestCx, nearestCz); // null if not loaded
            if (chunkNow == null) return false;
        }

        return true;
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
