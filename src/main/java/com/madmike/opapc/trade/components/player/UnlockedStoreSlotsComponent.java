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

package com.madmike.opapc.trade.components.player;

import com.madmike.opapc.OPAPCComponents;
import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class UnlockedStoreSlotsComponent implements Component {

    private final Player player;
    private int unlockedSlots = 5;

    public UnlockedStoreSlotsComponent(Player player) {
        this.player = player;
    }

    public int getUnlockedSlots() {
        return unlockedSlots;
    }

    public void setUnlockedSlots(int slots) {
        this.unlockedSlots = slots;
        OPAPCComponents.UNLOCKED_STORE_SLOTS.sync(player);
    }

    public void increment(int amount) {
        this.unlockedSlots += amount;
        OPAPCComponents.UNLOCKED_STORE_SLOTS.sync(player);
    }

    public void reset() {
        this.unlockedSlots = 5;
        OPAPCComponents.UNLOCKED_STORE_SLOTS.sync(player);
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        this.unlockedSlots = tag.getInt("UnlockedSlots");
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.putInt("UnlockedSlots", this.unlockedSlots);
    }

}
