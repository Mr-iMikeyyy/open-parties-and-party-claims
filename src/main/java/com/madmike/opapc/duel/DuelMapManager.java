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

package com.madmike.opapc.duel;

import com.ibm.icu.impl.Pair;
import com.madmike.opapc.duel.data.DuelMap;
import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Stores all duel maps globally (dimension-agnostic).
 * NBT schema:
 *  Version: int
 *  Maps: ListTag of CompoundTag (DuelMap#toNbt())
 */
public class DuelMapManager implements ComponentV3 {

    public static DuelMapManager INSTANCE = new DuelMapManager();

    private static final String NBT_VERSION_KEY = "Version";
    private static final String NBT_MAPS_KEY = "Maps";
    private static final int NBT_VERSION = 1;

    // Keep insertion order stable (nice for UIs), and O(1) name lookups.
    private final List<DuelMap> duelMaps = new ArrayList<>();
    private final Map<String, Integer> nameToIndex = new HashMap<>(); // lower(name) -> index in duelMaps

    /* ----------------------- Basic API ----------------------- */

    public List<DuelMap> getAll() {
        return Collections.unmodifiableList(duelMaps);
    }

    public int size() {
        return duelMaps.size();
    }

    public boolean isEmpty() {
        return duelMaps.isEmpty();
    }

    public Optional<DuelMap> get(String name) {
        int idx = indexOfName(name);
        return idx >= 0 ? Optional.of(duelMaps.get(idx)) : Optional.empty();
    }

    public boolean exists(String name) {
        return indexOfName(name) >= 0;
    }

    /**
     * Add a map. If a map with the same (case-insensitive) name exists:
     * - replaceIfExists=true: replaces it in-place (preserving order)
     * - replaceIfExists=false: returns false (no changes)
     */
    public boolean add(DuelMap map, boolean replaceIfExists) {
        Objects.requireNonNull(map, "map");
        validate(map);

        String key = norm(map.getName());
        int idx = nameToIndex.getOrDefault(key, -1);

        if (idx >= 0) {
            if (!replaceIfExists) {
                return false;
            }
            duelMaps.set(idx, map);
            // index unchanged
            return true;
        } else {
            duelMaps.add(map);
            nameToIndex.put(key, duelMaps.size() - 1);
            return true;
        }
    }

    /** Remove by name (case-insensitive). Returns true if removed. */
    public boolean remove(String name) {
        int idx = indexOfName(name);
        if (idx < 0) return false;

        duelMaps.remove(idx);
        rebuildIndex();
        return true;
    }

    /** Clear all maps. */
    public void clear() {
        duelMaps.clear();
        nameToIndex.clear();
    }

    /* ----------------------- Validation ----------------------- */

    /** Validates a map: non-empty name, at least 1 spawn per side. */
    public static void validate(DuelMap map) {
        if (map.getName() == null || map.getName().isBlank()) {
            throw new IllegalArgumentException("DuelMap name must not be empty");
        }
        if (map.getPlayer1Spawns().isEmpty()) {
            throw new IllegalArgumentException("DuelMap must have at least one Player 1 spawn");
        }
        if (map.getPlayer2Spawns().isEmpty()) {
            throw new IllegalArgumentException("DuelMap must have at least one Player 2 spawn");
        }
    }

    /* ----------------------- Random Selection Helpers ----------------------- */

    /** Pick a random map, or empty if none exist. */
    public Optional<DuelMap> getRandom(RandomSource random) {
        if (duelMaps.isEmpty()) return Optional.empty();
        return Optional.of(duelMaps.get(random.nextInt(duelMaps.size())));
    }

    /**
     * Returns a spawn pair from the given map:
     * - If lists are same size and > 0, use the same index for both (random, unless fixedIndex provided).
     * - If sizes differ, pick independently from each list.
     */
    public Optional<Pair<BlockPos, BlockPos>> pickSpawnPair(String mapName, RandomSource random, @Nullable Integer fixedIndex) {
        Optional<DuelMap> opt = get(mapName);
        if (opt.isEmpty()) return Optional.empty();
        DuelMap m = opt.get();

        List<BlockPos> p1 = m.getPlayer1Spawns();
        List<BlockPos> p2 = m.getPlayer2Spawns();
        if (p1.isEmpty() || p2.isEmpty()) return Optional.empty();

        int i1, i2;
        if (fixedIndex != null && fixedIndex >= 0 && fixedIndex < Math.min(p1.size(), p2.size())) {
            i1 = i2 = fixedIndex;
        } else if (p1.size() == p2.size()) {
            int idx = random.nextInt(p1.size());
            i1 = i2 = idx;
        } else {
            i1 = random.nextInt(p1.size());
            i2 = random.nextInt(p2.size());
        }

        return Optional.of(Pair.of(p1.get(i1), p2.get(i2)));
    }

    /* ----------------------- NBT Persistence ----------------------- */

    @Override
    public void readFromNbt(CompoundTag tag) {
        clear();

        // Version is here for future migrations (currently unused)
        int version = tag.contains(NBT_VERSION_KEY, Tag.TAG_INT) ? tag.getInt(NBT_VERSION_KEY) : 0;

        ListTag list = tag.getList(NBT_MAPS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag mapTag = list.getCompound(i);
            DuelMap map = DuelMap.fromNbt(mapTag);
            // Be lenient: skip invalid maps rather than crash
            try {
                validate(map);
                duelMaps.add(map);
            } catch (IllegalArgumentException ignored) {
                // You might want to log this
            }
        }
        rebuildIndex();

        // If you ever bump versions, run migrations here based on `version`
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.putInt(NBT_VERSION_KEY, NBT_VERSION);
        ListTag list = new ListTag();
        for (DuelMap map : duelMaps) {
            list.add(map.toNbt());
        }
        tag.put(NBT_MAPS_KEY, list);
    }

    /* ----------------------- Internals ----------------------- */

    private int indexOfName(String name) {
        if (name == null) return -1;
        Integer idx = nameToIndex.get(norm(name));
        return idx == null ? -1 : idx;
    }

    private void rebuildIndex() {
        nameToIndex.clear();
        for (int i = 0; i < duelMaps.size(); i++) {
            nameToIndex.put(norm(duelMaps.get(i).getName()), i);
        }
    }

    private static String norm(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}
