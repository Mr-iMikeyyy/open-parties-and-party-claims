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

package com.madmike.opapc.trade.data;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record SellerInfo(UUID id, String name, long totalSales) {

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("Id", id);
        nbt.putString("Name", name);
        nbt.putLong("Sales", totalSales);
        return nbt;
    }

    public static SellerInfo fromNbt(CompoundTag nbt) {
        UUID id = nbt.getUUID("Id");
        String name = nbt.getString("Name");
        long totalSales = nbt.getLong("Sales");
        return new SellerInfo(id, name, totalSales);
    }

    public SellerInfo addSales(long amount) {
        return new SellerInfo(id, name, totalSales + amount);
    }

    public SellerInfo withName(String newName) {
        return new SellerInfo(id, newName, totalSales);
    }
}
