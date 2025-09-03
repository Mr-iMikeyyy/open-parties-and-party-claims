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

package com.madmike.opapc.war.merc.component.scoreboard;

import com.glisco.numismaticoverhaul.ModComponents;
import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MercOfferRefundComponent implements ComponentV3 {
    private final Map<UUID, List<Long>> refunds = new HashMap<>();
    private final Scoreboard provider;
    private final MinecraftServer server;

    public MercOfferRefundComponent(Scoreboard sb, MinecraftServer sv) {
        this.provider = sb;
        this.server = sv;
    }

    /* ---------------- Runtime API ---------------- */

    /** Queue a refund for a player. */
    public void addRefund(UUID playerId, long amount) {
        refunds.computeIfAbsent(playerId, k -> new ArrayList<>()).add(amount);
    }

    /** True if the player has any pending refunds. */
    public boolean hasRefunds(UUID playerId) {
        List<Long> list = refunds.get(playerId);
        return list != null && !list.isEmpty();
    }

    /** Pays out and clears any pending refunds for this player. Call on join/login. */
    public void onPlayerJoin(@NotNull ServerPlayer player) {
        UUID id = player.getUUID();
        List<Long> pending = refunds.remove(id);  // remove to prevent double‐pay if this runs again
        if (pending == null || pending.isEmpty()) return;

        // Apply each refund to the player's currency component
        pending.forEach(amount -> ModComponents.CURRENCY.get(player).modify(amount));
    }

    /** Optional: expose a read-only view (useful for admin commands or debugging). */
    public Map<UUID, List<Long>> getAllRefundsView() {
        Map<UUID, List<Long>> copy = new HashMap<>();
        for (var e : refunds.entrySet()) copy.put(e.getKey(), List.copyOf(e.getValue()));
        return Collections.unmodifiableMap(copy);
    }

    /* ---------------- NBT Persistence ---------------- */

    @Override
    public void readFromNbt(CompoundTag tag) {
        refunds.clear();

        ListTag entries = tag.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);

            if (!entry.hasUUID("Player")) continue;
            UUID playerId = entry.getUUID("Player");

            long[] arr = Optional.of(entry.getLongArray("Refunds")).orElse(new long[0]);
            if (arr.length == 0) continue;

            List<Long> list = new ArrayList<>(arr.length);
            for (long v : arr) list.add(v);
            refunds.put(playerId, list);
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        ListTag entries = new ListTag();

        for (Map.Entry<UUID, List<Long>> e : refunds.entrySet()) {
            if (e.getValue() == null || e.getValue().isEmpty()) continue;

            CompoundTag entry = new CompoundTag();
            entry.putUUID("Player", e.getKey());

            long[] arr = e.getValue().stream().mapToLong(Long::longValue).toArray();
            entry.put("Refunds", new LongArrayTag(arr));

            entries.add(entry);
        }

        tag.put("Entries", entries);
    }
}
