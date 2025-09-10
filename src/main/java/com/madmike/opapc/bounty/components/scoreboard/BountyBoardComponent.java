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

package com.madmike.opapc.bounty.components.scoreboard;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BountyBoardComponent implements ComponentV3 {
    private final Scoreboard provider;
    private final MinecraftServer server;

    // Active bounties: target -> reward
    private final Map<UUID, Long> bounties = new HashMap<>();

    public BountyBoardComponent(Scoreboard sb, MinecraftServer sv) {
        this.provider = sb;
        this.server = sv;
    }

    /* ---------------- Public API ---------------- */

    /** Get the reward value for a target. Returns null if none exists. */
    public long getBounty(UUID target) {
        return bounties.get(target);
    }

    /** Set or update a bounty. Overwrites if one already exists. */
    public void setBounty(UUID target, long reward) {
        if (reward <= 0) {
            bounties.remove(target);
        } else {
            bounties.put(target, reward);
        }
    }

    /** Increase an existing bounty by amount. Creates a new one if missing. */
    public void addToBounty(UUID target, long amount) {
        if (amount <= 0) return;
        bounties.merge(target, amount, Long::sum);
    }

    /** Remove a bounty completely. */
    public void removeBounty(UUID target) {
        bounties.remove(target);
    }

    /** Get all current bounties. */
    public Map<UUID, Long> getAllBounties() {
        return bounties;
    }

    /** Clear all bounties. */
    public void clear() {
        bounties.clear();
    }

    /* ---------------- Persistence ---------------- */

    @Override
    public void readFromNbt(CompoundTag tag) {
        bounties.clear();

        ListTag list = tag.getList("Bounties", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag bountyTag = list.getCompound(i);
            if (!bountyTag.hasUUID("Target")) continue;

            UUID target = bountyTag.getUUID("Target");
            long reward = bountyTag.getLong("Reward");
            bounties.put(target, reward);
        }
    }

    @Override
    public void writeToNbt(@NotNull CompoundTag tag) {
        ListTag list = new ListTag();

        for (Map.Entry<UUID, Long> entry : bounties.entrySet()) {
            CompoundTag bountyTag = new CompoundTag();
            bountyTag.putUUID("Target", entry.getKey());
            bountyTag.putLong("Reward", entry.getValue());
            list.add(bountyTag);
        }

        tag.put("Bounties", list);
    }
}
