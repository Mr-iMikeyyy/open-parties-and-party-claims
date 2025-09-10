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

package com.madmike.opapc.player.component.player;

import com.madmike.opapc.OPAPCConfig;
import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyRejoinCooldownComponent implements ComponentV3 {

    private final Map<UUID, Long> partyLeaveTimes = new HashMap<>();
    private final Player player;

    public PartyRejoinCooldownComponent(Player player) {
        this.player = player;
    }

    public void onPartyLeave(UUID partyId) {
        partyLeaveTimes.put(partyId, System.currentTimeMillis());
    }

    public boolean canPlayerRejoinParty(UUID partyId) {
        if (!partyLeaveTimes.containsKey(partyId)) {
            return true; // no cooldown set
        }

        long leaveTime = partyLeaveTimes.get(partyId);
        long cooldown = getCooldownDurationMs();
        return System.currentTimeMillis() - leaveTime >= cooldown;
    }

    public long getRemainingTimeMs(UUID partyId) {
        if (!partyLeaveTimes.containsKey(partyId)) {
            return 0;
        }
        long leaveTime = partyLeaveTimes.get(partyId);
        long cooldown = getCooldownDurationMs();
        long remaining = cooldown - (System.currentTimeMillis() - leaveTime);
        return Math.max(remaining, 0);
    }

    public String getRemainingTimeFormatted(UUID partyId) {
        long remainingMs = getRemainingTimeMs(partyId);

        if (remainingMs <= 0) {
            return "0s";
        }

        long totalSeconds = remainingMs / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0 || days > 0) sb.append(hours).append("h ");
        if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");

        return sb.toString().trim();
    }

    private long getCooldownDurationMs() {
        // Use hours instead of days
        long hours = OPAPCConfig.partyRejoinCooldownHours;
        if (hours <= 0) hours = 72; // default 72 hours = 3 days
        return hours * 60L * 60L * 1000L;
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        partyLeaveTimes.clear();
        ListTag list = tag.getList("PartyLeaveTimes", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag entry = (CompoundTag) t;
            UUID id = entry.getUUID("PartyId");
            long time = entry.getLong("LeaveTime");
            partyLeaveTimes.put(id, time);
        }
    }

    @Override
    public void writeToNbt(@NotNull CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Long> e : partyLeaveTimes.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("PartyId", e.getKey());
            entry.putLong("LeaveTime", e.getValue());
            list.add(entry);
        }
        tag.put("PartyLeaveTimes", list);
    }
}
