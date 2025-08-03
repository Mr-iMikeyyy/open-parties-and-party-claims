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
import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class WarpCombatCooldownComponent implements Component {
    private long lastDamageTime = 0;
    private final Player player;

    public WarpCombatCooldownComponent(Player player) {
        this.player = player;
    }

    /** Call this when the player is damaged */
    public void onDamaged() {
        lastDamageTime = System.currentTimeMillis();
    }

    /** Call this when you want to check if the player is still in combat */
    public boolean isInCombat() {
        long durationMs = OPAPCConfig.warpCooldownCombatSeconds * 1000L;
        return System.currentTimeMillis() - lastDamageTime < durationMs;
    }

    public long getRemainingTimeMs() {
        long durationMs = OPAPCConfig.warpCooldownCombatSeconds * 1000L;
        long remaining = durationMs - (System.currentTimeMillis() - lastDamageTime);
        return Math.max(remaining, 0);
    }

    public int getRemainingTimeSeconds() {
        long remainingMs = getRemainingTimeMs();
        return (int) Math.ceil(remainingMs / 1000.0);
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        this.lastDamageTime = tag.getLong("LastDamageTime");
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.putLong("LastDamageTime", this.lastDamageTime);
    }
}
