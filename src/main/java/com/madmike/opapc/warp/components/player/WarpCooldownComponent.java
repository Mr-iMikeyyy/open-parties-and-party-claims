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

package com.madmike.opapc.warp.components.player;

import com.madmike.opapc.OPAPCConfig;
import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.time.Duration;

public class WarpCooldownComponent implements ComponentV3 {
    private long lastTeleportTime = 0;
    private final Player player;

    public WarpCooldownComponent(Player player) {
        this.player = player;
    }

    /** Call this when the player teleports */
    public void onWarp() {
        lastTeleportTime = System.currentTimeMillis();
    }

    /** Check if player still has a teleport cooldown */
    public boolean hasCooldown() {
        long durationMs = OPAPCConfig.warpCooldownSeconds * 1000L;
        return System.currentTimeMillis() - lastTeleportTime < durationMs;
    }

    /** Get remaining cooldown as Duration for minutes/seconds display */
    public Duration getRemainingTime() {
        long durationMs = OPAPCConfig.warpCooldownSeconds * 1000L;
        long remainingMs = durationMs - (System.currentTimeMillis() - lastTeleportTime);
        if (remainingMs < 0) remainingMs = 0;
        return Duration.ofMillis(remainingMs);
    }

    /** Get formatted minutes/seconds string for chat/overlay */
    public String getFormattedRemainingTime() {
        Duration remaining = getRemainingTime();
        long minutes = remaining.toMinutes();
        long seconds = remaining.minusMinutes(minutes).getSeconds();
        return String.format("%d min %d sec", minutes, seconds);
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        this.lastTeleportTime = tag.getLong("LastTeleportTime");
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.putLong("LastTeleportTime", this.lastTeleportTime);
    }
}