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

package com.madmike.opapc.war.features.block;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import com.madmike.opapc.util.ClaimAdjacencyChecker;
import com.madmike.opapc.war.data.WarData;
import com.madmike.opapc.war.features.WarFeatures;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class WarBlockSpawner {

    /* ===================== PUBLIC: ASYNC SAFE-SPAWN ===================== */

    /**
     * Asynchronously finds a safe spawn for the war block.
     * - Will request FULL chunks asynchronously as needed.
     * - All world reads happen on the server thread.
     * - Returns a future that completes with a valid BlockPos or null.
     */
    public static CompletableFuture<BlockPos> findSafeSpawnAsync(WarData war, boolean allowWaterSurface) {
        MinecraftServer server = OPAPC.getServer();
        ServerLevel world = server.overworld();

        PartyClaim claim = OPAPCComponents.PARTY_CLAIMS
                .get(server.getScoreboard())
                .getClaim(war.getDefendingParty().getId());
        if (claim == null) return CompletableFuture.completedFuture(null);

        List<ChunkPos> ownedChunks = new ArrayList<>(claim.getClaimedChunksList());
        if (ownedChunks.isEmpty()) return CompletableFuture.completedFuture(null);

        // Avoid mutating claim data: optionally exclude warp chunk
        BlockPos warpPos = claim.getWarpPos();
        if (warpPos != null) {
            ownedChunks.remove(new ChunkPos(warpPos));
            if (ownedChunks.isEmpty()) {
                ownedChunks.add(new ChunkPos(warpPos));
            }
        }

        // Prebuild a randomized list of candidate columns (x,z) across owned chunks.
        List<BlockPos> candidates = preselectCandidateColumns(ownedChunks, /*samplesPerChunk=*/8, world.getRandom());
        // Try at most N candidates to avoid long tails
        final int MAX_ATTEMPTS = Math.min(2000, candidates.size());

        // Walk candidates in order, loading chunks asynchronously as needed.
        return tryCandidateIndex(world, claim, candidates, 0, MAX_ATTEMPTS, allowWaterSurface);
    }

    /**
     * Example usage: place the block when the async search finishes.
     * Make sure to hop to server thread for the actual placement.
     */
    public static void findAndSpawnWarBlockAsync(WarData data, boolean allowWaterSurface) {
        MinecraftServer server = OPAPC.getServer();
        findSafeSpawnAsync(data, allowWaterSurface).thenAccept(pos -> {
            if (pos == null) return;
            server.execute(() -> spawnWarBlock(pos, data)); // server-thread placement
        });
    }

    /* ===================== ORIGINAL SYNC PLACE ===================== */

    public static void spawnWarBlock(BlockPos pos, WarData data) {
        if (pos == null || data == null) return;

        ServerLevel world = OPAPC.getServer().overworld();
        BlockState state = WarFeatures.WAR_BLOCK.defaultBlockState();

        boolean placed = world.setBlock(pos, state, 3);
        if (placed) {
            // store an immutable copy to avoid accidental mutation
            data.setWarBlockPosition(pos.immutable());
        }
    }

    /* ===================== INTERNAL: ASYNC DRIVER ===================== */

    private static CompletableFuture<BlockPos> tryCandidateIndex(
            ServerLevel world,
            PartyClaim claim,
            List<BlockPos> candidates,
            int index,
            int maxAttempts,
            boolean allowWaterSurface
    ) {
        if (index >= maxAttempts) {
            return CompletableFuture.completedFuture(null);
        }

        final BlockPos col = candidates.get(index);
        final ChunkPos cp = new ChunkPos(col);

        // If picking this chunk would fragment the claim, skip it early (pure calc).
        if (ClaimAdjacencyChecker.wouldBreakAdjacency(claim.getClaimedChunksList(), cp)) {
            return tryCandidateIndex(world, claim, candidates, index + 1, maxAttempts, allowWaterSurface);
        }

        // Ensure the chunk is FULL asynchronously, then hop to server thread to evaluate the column.
        return ensureChunkFullAsync(world, cp).thenCompose(chunk -> {
            if (chunk == null) {
                // couldn't load -> try next
                return tryCandidateIndex(world, claim, candidates, index + 1, maxAttempts, allowWaterSurface);
            }
            // Evaluate this column on server thread (block states, heightmaps, etc.)
            return callOnServerThread(world.getServer(), () -> findInColumnTopDown(world, col.getX(), col.getZ(), allowWaterSurface)).thenCompose(found -> {
                if (found != null) {
                    return CompletableFuture.completedFuture(found);
                }
                // Not suitable; move on
                return tryCandidateIndex(world, claim, candidates, index + 1, maxAttempts, allowWaterSurface);
            });
        });
    }

    /* ===================== INTERNAL: CANDIDATES ===================== */

    private static List<BlockPos> preselectCandidateColumns(List<ChunkPos> chunks, int samplesPerChunk, RandomSource rng) {
        List<BlockPos> out = new ArrayList<>(chunks.size() * samplesPerChunk);
        for (ChunkPos cp : chunks) {
            int baseX = cp.x << 4;
            int baseZ = cp.z << 4;
            for (int i = 0; i < samplesPerChunk; i++) {
                int x = baseX + rng.nextInt(16);
                int z = baseZ + rng.nextInt(16);
                out.add(new BlockPos(x, 0, z)); // y resolved later
            }
        }
        // Shuffle so we don't bias early chunks
        Collections.shuffle(out, new Random(rng.nextLong()));
        return out;
    }

    /* ===================== INTERNAL: CHUNK LOADING ===================== */

    /**
     * Returns a future that completes with a FULL LevelChunk, loading it if necessary.
     * If load fails, completes with null.
     */
    private static CompletableFuture<LevelChunk> ensureChunkFullAsync(ServerLevel world, ChunkPos cp) {
        ServerChunkCache cache = world.getChunkSource();

        // If already present at FULL, return immediately
        LevelChunk existing = cache.getChunkNow(cp.x, cp.z);
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }

        // Request FULL chunk asynchronously (creates if needed).
        // getChunkFuture returns Either<ChunkAccess, ChunkLoadingFailure>.
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> fut =
                cache.getChunkFuture(cp.x, cp.z, net.minecraft.world.level.chunk.ChunkStatus.FULL, true);

        return fut.thenApply(either -> {
            ChunkAccess access = either.left().orElse(null);
            if (access instanceof LevelChunk lc) {
                return lc;
            }
            return null;
        });
    }

    /* ===================== INTERNAL: SERVER-THREAD HOP ===================== */

    private static <T> CompletableFuture<T> callOnServerThread(MinecraftServer server, Supplier<T> task) {
        CompletableFuture<T> cf = new CompletableFuture<>();
        server.execute(() -> {
            try {
                cf.complete(task.get());
            } catch (Throwable t) {
                cf.completeExceptionally(t);
            }
        });
        return cf;
    }

    /* ===================== INTERNAL: COLUMN SCAN (server-thread only) ===================== */

    private static BlockPos findInColumnTopDown(ServerLevel world, int x, int z, boolean allowWaterSurface) {
        // 1) Solid ground above MOTION_BLOCKING
        int startY = world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos solid = scanUpForSolidGround(world, x, z, startY);
        if (solid != null) return solid;

        // 2) Optional: water surface from WORLD_SURFACE
        if (allowWaterSurface) {
            int surfaceY = world.getHeight(Heightmap.Types.WORLD_SURFACE, x ,z);
            if (surfaceY > world.getMinBuildHeight()) {
                BlockPos surfacePos = new BlockPos(x, surfaceY - 1, z);
                if (isWater(world, surfacePos) && hasClearance(world, surfacePos.above(), 1)) {
                    return surfacePos; // replace top water block
                }
                BlockPos down = scanDownToWaterSurface(world, x, z, surfaceY - 1, 6);
                if (down != null && hasClearance(world, down.above(), 1)) {
                    return down;
                }
            }
        }
        return null;
    }

    private static BlockPos scanUpForSolidGround(ServerLevel world, int x, int z, int startY) {
        int maxY = world.getMaxBuildHeight() - 2;
        for (int y = Math.max(startY, world.getMinBuildHeight()); y <= maxY; y++) {
            BlockPos groundPos = new BlockPos(x, y, z);
            BlockState ground = world.getBlockState(groundPos);
            if (!ground.isFaceSturdy(world, groundPos, Direction.UP)) continue;
            if (!world.getFluidState(groundPos).isEmpty()) continue;
            if (!hasClearance(world, groundPos.above(), 2)) continue;
            return groundPos.above();
        }
        return null;
    }

    private static BlockPos scanDownToWaterSurface(ServerLevel world, int x, int z, int startY, int maxDepth) {
        int minY = Math.max(world.getMinBuildHeight(), startY - maxDepth);
        for (int y = startY; y >= minY; y--) {
            BlockPos p = new BlockPos(x, y, z);
            if (isWater(world, p) && world.isEmptyBlock(p.above())) {
                return p;
            }
        }
        return null;
    }

    private static boolean isWater(ServerLevel world, BlockPos pos) {
        var fs = world.getFluidState(pos);
        return fs.isSource() && fs.is(FluidTags.WATER);
    }

    private static boolean hasClearance(ServerLevel world, BlockPos start, int height) {
        for (int i = 0; i < height; i++) {
            if (!world.isEmptyBlock(start.above(i))) return false;
        }
        return true;
    }
}

