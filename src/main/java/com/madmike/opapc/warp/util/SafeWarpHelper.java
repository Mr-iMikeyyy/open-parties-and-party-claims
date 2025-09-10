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

package com.madmike.opapc.warp.util;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.pioneer.data.PartyClaim;
import com.madmike.opapc.util.SimpleTickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SafeWarpHelper {

    private SafeWarpHelper() {}

    /**
     * Find a safe wilderness spot up to maxDistanceChunks (1..3) away from the claim’s border.
     * - Does heavy candidate *generation* off-thread
     * - Validates candidates *on server thread* a few per tick
     */
    public static CompletableFuture<BlockPos> findAmbushSpotAsync(PartyClaim targetClaim,
                                                                  int maxDistanceChunks,
                                                                  boolean allowWaterSurface,
                                                                  int attempts,
                                                                  int checksPerTick) {
        MinecraftServer server = OPAPC.getServer();
        ServerLevel world = server.overworld();

        // 1) Off-thread: build a shuffled list of candidate ChunkPos around the claim border
        return CompletableFuture.supplyAsync(() -> {
            List<ChunkPos> border = findBorderChunks(targetClaim.getClaimedChunksList());
            if (border.isEmpty()) return Collections.<ChunkPos>emptyList();

            Random rand = new Random();
            List<ChunkPos> ring = new ArrayList<>();

            for (ChunkPos edge : border) {
                for (int d = 1; d <= maxDistanceChunks; d++) {
                    // Sample 8 directions around this edge chunk
                    ring.add(new ChunkPos(edge.x + d, edge.z));         // +X
                    ring.add(new ChunkPos(edge.x - d, edge.z));         // -X
                    ring.add(new ChunkPos(edge.x, edge.z + d));         // +Z
                    ring.add(new ChunkPos(edge.x, edge.z - d));         // -Z
                    ring.add(new ChunkPos(edge.x + d, edge.z + d));     // diag +
                    ring.add(new ChunkPos(edge.x + d, edge.z - d));
                    ring.add(new ChunkPos(edge.x - d, edge.z + d));
                    ring.add(new ChunkPos(edge.x - d, edge.z - d));
                }
            }

            // Remove duplicates & any chunks that are still inside the claim
            Set<ChunkPos> dedup = new HashSet<>(ring);
            dedup.removeAll(targetClaim.getClaimedChunksList());

            // Shuffle for randomness
            List<ChunkPos> candidates = new ArrayList<>(dedup);
            Collections.shuffle(candidates, rand);
            return candidates;
        }).thenCompose(candidates -> {
            // 2) On-thread incremental validation
            CompletableFuture<BlockPos> result = new CompletableFuture<>();
            if (candidates.isEmpty()) {
                result.complete(null);
                return result;
            }

            // We'll validate up to `attempts` total spots
            final Iterator<ChunkPos> it = candidates.iterator();
            final int[] tried = {0};

            Runnable tickWork = new Runnable() {
                @Override public void run() {
                    if (result.isDone()) return;

                    int checks = 0;
                    while (checks < checksPerTick && it.hasNext() && tried[0] < attempts) {
                        ChunkPos cp = it.next();
                        tried[0]++;

                        // Must be wilderness: no party claim on this chunk
                        if (OPAPC.claims().get(world.dimension().location(), cp) != null) {
                            checks++;
                            continue;
                        }

                        // Pick a random (x,z) inside this chunk
                        Random r = new Random();
                        int x = (cp.x << 4) + r.nextInt(16);
                        int z = (cp.z << 4) + r.nextInt(16);

                        // Make sure chunk is present; bounded sync load—only a few per tick
                        var holder = world.getChunkSource().getChunk(cp.x, cp.z, true);
                        if (holder == null) { checks++; continue; }

                        int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                        BlockPos pos = new BlockPos(x, y, z);

                        if (isSafeTeleportSpot(world, pos, allowWaterSurface)) {
                            result.complete(pos);
                            return;
                        }

                        checks++;
                    }

                    if (!it.hasNext() || tried[0] >= attempts) {
                        result.complete(null);
                    } else {
                        // Try more next tick
                        SimpleTickScheduler.runLater(1, this);
                    }
                }
            };

            // Kick off on server thread
            server.execute(() -> {
                // ensure scheduler initialized
                SimpleTickScheduler.init();
                SimpleTickScheduler.runLater(1, tickWork);
            });

            return result;
        });
    }

    /** Find chunks that have at least one 4-neighbor outside the claim set. */
    private static List<ChunkPos> findBorderChunks(List<ChunkPos> claimed) {
        Set<ChunkPos> set = new HashSet<>(claimed);
        List<ChunkPos> border = new ArrayList<>();
        for (ChunkPos c : set) {
            if (!set.contains(new ChunkPos(c.x + 1, c.z)) ||
                    !set.contains(new ChunkPos(c.x - 1, c.z)) ||
                    !set.contains(new ChunkPos(c.x, c.z + 1)) ||
                    !set.contains(new ChunkPos(c.x, c.z - 1))) {
                border.add(c);
            }
        }
        return border;
    }

    public static boolean isSafeTeleportSpot(ServerLevel world, BlockPos pos, boolean allowWaterSurface) {
        // Basic world bounds (head must also be valid)
        if (!world.isInWorldBounds(pos) || !world.isInWorldBounds(pos.above())) return false;
        int minY = world.getMinBuildHeight();
        int maxY = world.getMaxBuildHeight() - 1; // head room needs pos+1
        if (pos.getY() < minY + 1 || pos.getY() > maxY - 1) return false;

        BlockPos belowPos = pos.below();
        if (!world.isInWorldBounds(belowPos)) return false;

        BlockState below = world.getBlockState(belowPos);
        BlockState feet  = world.getBlockState(pos);
        BlockState head  = world.getBlockState(pos.above());

        // Reject obvious hazards anywhere in the stack
        if (isHazard(below) || isHazard(feet) || isHazard(head)) return false;

        // Feet & head must be free of hard collisions
        if (!isPassable(world, pos, feet)) return false;
        if (!isPassable(world, pos.above(), head)) return false;

        // No standing inside liquids at feet (unless it's water surface logic)
        FluidState feetFluid = feet.getFluidState();
        boolean feetInLiquid = !feetFluid.isEmpty();
        if (feetInLiquid) {
            // Only allow if it's literally standing ON water surface (feet = air, below = water)
            if (!(allowWaterSurface && feetFluid.isEmpty() && world.getFluidState(belowPos).is(FluidTags.WATER))) {
                return false;
            }
        }

        // Standing surface options:
        // 1) Solid top surface
        boolean solidBelow = below.isFaceSturdy(world, belowPos, Direction.UP);

        // 2) Water surface (below = water, feet/head air) if allowed
        boolean waterSurfaceOk = false;
        if (allowWaterSurface) {
            FluidState belowFluid = below.getFluidState();
            if (belowFluid.is(FluidTags.WATER)) {
                // Require no collision at feet & head (already checked) and not a whirlpool/cave nonsense
                waterSurfaceOk = true;
            }
        }

        return solidBelow || waterSurfaceOk;
    }

    /* ---------- helpers ---------- */

    private static boolean isPassable(ServerLevel world, BlockPos at, BlockState state) {
        // No solid collision
        VoxelShape shape = state.getCollisionShape(world, at);
        if (!shape.isEmpty()) return false;

        // Avoid tricky blocks that have no collision but still trap/hurt
        Block b = state.getBlock();
        if (b == Blocks.COBWEB) return false;
        if (b == Blocks.SWEET_BERRY_BUSH) return false;
        if (b == Blocks.POWDER_SNOW) return false;

        // Fluids at feet/head are disallowed by caller logic (except water surface case)
        return true;
    }

    private static boolean isHazard(BlockState state) {
        Block b = state.getBlock();

        // Obvious bad blocks to stand in/near
        if (b == Blocks.LAVA || b == Blocks.MAGMA_BLOCK) return true;
        if (b == Blocks.CAMPFIRE || b == Blocks.SOUL_CAMPFIRE) return true;
        if (b == Blocks.FIRE || b == Blocks.SOUL_FIRE) return true;
        if (b == Blocks.CACTUS) return true;

        // Cobweb, berry bush, powdered snow are handled in isPassable() for feet/head,
        // but catching here too makes below/around safety slightly stricter.
        if (b == Blocks.COBWEB || b == Blocks.SWEET_BERRY_BUSH || b == Blocks.POWDER_SNOW) return true;

        // Avoid lava fluids even if block isn’t LAVA (e.g., cauldrons not needed here but safe)
        if (state.getFluidState().is(FluidTags.LAVA)) return true;

        return false;
    }

    public static void countdownAndTeleport(ServerPlayer player, BlockPos pos) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        for (int i = 0; i < 3; i++) {
            int count = 3 - i;
            server.execute(() -> player.sendSystemMessage(Component.literal("Teleporting in " + count + "...")));
            int delay = i * 20; // ticks (1 sec each)
            server.getScheduler().schedule(() -> {
                player.sendSystemMessage(Component.literal("Teleporting in " + count + "..."));
            }, delay, TimeUnit.TICKS);
        }

        // Do the teleport after 3 seconds
        server.getScheduler().schedule(() -> {
            player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            player.sendSystemMessage(Component.literal("Ambush begins!"));
        }, 60, TimeUnit.TICKS);
    }

    public static void warpPlayerToOverworldPos(ServerPlayer player, BlockPos warpPos) {
        player.teleportTo(OPAPC.getServer().overworld(), warpPos.getX() + 0.5, warpPos.getY(), warpPos.getZ() + 0.5, player.getYRot(), player.getXRot());
    }

    public static void warpPlayerToWorldSpawn(ServerPlayer player) {
        BlockPos pos = OPAPC.getServer().overworld().getSharedSpawnPos();
        player.teleportTo(OPAPC.getServer().overworld(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), player.getXRot());
    }
}
