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

package com.madmike.opapc.pioneer.data;


import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record Donor(String name, long amount) {

    public CompoundTag toNbt(UUID playerId) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("PlayerId", playerId);
        tag.putString("Name", name);
        tag.putLong("Amount", amount);
        return tag;
    }

    public static Donor fromNbt(CompoundTag tag) {
        String name = tag.getString("Name");
        long amount = tag.getLong("Amount");
        return new Donor(name, amount);
    }
}
