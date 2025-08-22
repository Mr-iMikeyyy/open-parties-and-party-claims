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

package com.madmike.opapc.duel.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DuelMap {
    // --- NBT keys ---
    private static final String NBT_NAME = "Name";
    private static final String NBT_READY = "Ready";
    private static final String NBT_P1 = "Player1Spawns";
    private static final String NBT_P2 = "Player2Spawns";
    private static final String NBT_X = "x";
    private static final String NBT_Y = "y";
    private static final String NBT_Z = "z";

    private String name;
    private final List<BlockPos> player1Spawns;
    private final List<BlockPos> player2Spawns;

    // Optional: filter in suggestions/admin
    private boolean ready = true;

    public DuelMap(String name) {
        this(name, new ArrayList<>(), new ArrayList<>());
    }

    public DuelMap(String name, List<BlockPos> player1Spawns, List<BlockPos> player2Spawns) {
        this.name = name;
        this.player1Spawns = player1Spawns;
        this.player2Spawns = player2Spawns;
    }

    // ---------- basic ----------
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public List<BlockPos> getPlayer1Spawns() { return player1Spawns; }
    public List<BlockPos> getPlayer2Spawns() { return player2Spawns; }

    // Read‑only views (nice for UI code so it can’t mutate by accident)
    public List<BlockPos> viewPlayer1Spawns() { return Collections.unmodifiableList(player1Spawns); }
    public List<BlockPos> viewPlayer2Spawns() { return Collections.unmodifiableList(player2Spawns); }

    public void addPlayer1Spawn(BlockPos pos) { player1Spawns.add(pos); }
    public void addPlayer2Spawn(BlockPos pos) { player2Spawns.add(pos); }

    public boolean hasValidSpawns() {
        return !player1Spawns.isEmpty() && !player2Spawns.isEmpty();
    }

    // ---------- random pick helpers (side-specific) ----------
    public Optional<BlockPos> randomChallengerSpawn(RandomSource random) {
        if (player1Spawns.isEmpty()) return Optional.empty();
        return Optional.of(player1Spawns.get(random.nextInt(player1Spawns.size())));
    }

    public Optional<BlockPos> randomOpponentSpawn(RandomSource random) {
        if (player2Spawns.isEmpty()) return Optional.empty();
        return Optional.of(player2Spawns.get(random.nextInt(player2Spawns.size())));
    }

    // ---------- NBT ----------
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString(NBT_NAME, name);
        tag.putBoolean(NBT_READY, ready);

        ListTag p1List = new ListTag();
        for (BlockPos pos : player1Spawns) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt(NBT_X, pos.getX());
            posTag.putInt(NBT_Y, pos.getY());
            posTag.putInt(NBT_Z, pos.getZ());
            p1List.add(posTag);
        }
        tag.put(NBT_P1, p1List);

        ListTag p2List = new ListTag();
        for (BlockPos pos : player2Spawns) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt(NBT_X, pos.getX());
            posTag.putInt(NBT_Y, pos.getY());
            posTag.putInt(NBT_Z, pos.getZ());
            p2List.add(posTag);
        }
        tag.put(NBT_P2, p2List);

        return tag;
    }

    public static DuelMap fromNbt(CompoundTag tag) {
        String name = tag.getString(NBT_NAME);
        boolean ready = !tag.contains(NBT_READY, Tag.TAG_BYTE) || tag.getBoolean(NBT_READY);

        List<BlockPos> p1Spawns = new ArrayList<>();
        ListTag p1List = tag.getList(NBT_P1, Tag.TAG_COMPOUND);
        for (Tag t : p1List) {
            if (t instanceof CompoundTag posTag) {
                p1Spawns.add(new BlockPos(posTag.getInt(NBT_X), posTag.getInt(NBT_Y), posTag.getInt(NBT_Z)));
            }
        }

        List<BlockPos> p2Spawns = new ArrayList<>();
        ListTag p2List = tag.getList(NBT_P2, Tag.TAG_COMPOUND);
        for (Tag t : p2List) {
            if (t instanceof CompoundTag posTag) {
                p2Spawns.add(new BlockPos(posTag.getInt(NBT_X), posTag.getInt(NBT_Y), posTag.getInt(NBT_Z)));
            }
        }

        DuelMap map = new DuelMap(name, p1Spawns, p2Spawns);
        map.setReady(ready);
        return map;
    }
}
