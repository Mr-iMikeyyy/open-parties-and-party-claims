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

package com.madmike.opapc.duel.components.scoreboard;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuelStatsComponent implements ComponentV3 {
    // Single instance attached to the server scoreboard
    private final Scoreboard scoreboard;

    // Per-player stats; works for offline players
    private final Map<UUID, Stats> stats = new HashMap<>();

    public DuelStatsComponent(Scoreboard scoreboard) {
        this.scoreboard = scoreboard;
    }

    // ===== Public API =====

    /** Get or create stats for a player UUID. */
    public Stats get(UUID id) {
        return stats.computeIfAbsent(id, u -> new Stats());
    }

    /** Optional convenience overloads. */
    public Stats get(ServerPlayer player) { return get(player.getUUID()); }

    public void addDuelWon(UUID id) { get(id).duelsWon++; }
    public void addDuelLost(UUID id) { get(id).duelsLost++; }
    public void addWagersWon(UUID id, long amount) { get(id).wagersWon += amount; }
    public void addWagersLost(UUID id, long amount) { get(id).wagersLost += amount; }

    public int getDuelsWon(UUID id) { return stats.getOrDefault(id, Stats.ZERO).duelsWon; }
    public int getDuelsLost(UUID id) { return stats.getOrDefault(id, Stats.ZERO).duelsLost; }
    public long getWagersWon(UUID id) { return stats.getOrDefault(id, Stats.ZERO).wagersWon; }
    public long getWagersLost(UUID id) { return stats.getOrDefault(id, Stats.ZERO).wagersLost; }

    /** Read-only view to iterate/leaderboard. */
    public Map<UUID, Stats> viewAll() {
        return Collections.unmodifiableMap(stats);
    }

    // ===== NBT Persist =====
    // Tag layout:
    // {
    //   Players: [ { Id:"<uuid>", DuelsWon:..., DuelsLost:..., WagersWon:..., WagersLost:... }, ... ]
    // }

    @Override
    public void readFromNbt(CompoundTag tag) {
        stats.clear();
        if (!tag.contains("Players", Tag.TAG_LIST)) return;

        ListTag list = tag.getList("Players", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag p = (CompoundTag) t;
            if (!p.contains("Id", Tag.TAG_STRING)) continue;

            try {
                UUID id = UUID.fromString(p.getString("Id"));
                Stats s = Stats.fromNbt(p);
                stats.put(id, s);
            } catch (IllegalArgumentException ignored) {
                // bad UUID string; skip
            }
        }
    }

    @Override
    public void writeToNbt(@NotNull CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Stats> e : stats.entrySet()) {
            CompoundTag p = e.getValue().toNbt();
            p.putString("Id", e.getKey().toString());
            list.add(p);
        }
        tag.put("Players", list);
    }

    // ===== Data class =====
    public static class Stats {
        int duelsWon;
        int duelsLost;
        long wagersWon;
        long wagersLost;

        public Stats() {}

        /** Immutable zero for defaults/lookups. */
        public static final Stats ZERO = new Stats();

        // Getters (optional if you prefer fields)
        public int duelsWon() { return duelsWon; }
        public int duelsLost() { return duelsLost; }
        public long wagersWon() { return wagersWon; }
        public long wagersLost() { return wagersLost; }

        // Serialization
        public CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("DuelsWon", duelsWon);
            tag.putInt("DuelsLost", duelsLost);
            tag.putLong("WagersWon", wagersWon);
            tag.putLong("WagersLost", wagersLost);
            return tag;
        }

        public static Stats fromNbt(CompoundTag tag) {
            Stats s = new Stats();
            if (tag.contains("DuelsWon", Tag.TAG_INT)) s.duelsWon = tag.getInt("DuelsWon");
            if (tag.contains("DuelsLost", Tag.TAG_INT)) s.duelsLost = tag.getInt("DuelsLost");
            if (tag.contains("WagersWon", Tag.TAG_LONG)) s.wagersWon = tag.getLong("WagersWon");
            if (tag.contains("WagersLost", Tag.TAG_LONG)) s.wagersLost = tag.getLong("WagersLost");
            return s;
        }
    }
}
