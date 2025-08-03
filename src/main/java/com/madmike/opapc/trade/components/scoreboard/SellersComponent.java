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

package com.madmike.opapc.trade.components.scoreboard;

import com.madmike.opapc.trade.data.SellerInfo;
import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Scoreboard;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SellersComponent implements Component {

    private final Map<UUID, SellerInfo> sellers = new HashMap<>();
    private final Scoreboard scoreboard;
    private final MinecraftServer server;

    public SellersComponent(Scoreboard scoreboard, MinecraftServer server) {
        this.scoreboard = scoreboard;
        this.server = server;
    }

    public SellerInfo getSellerInfo(UUID playerId) {
        return sellers.get(playerId);
    }

    public void setSellerName(UUID playerId, String name) {
        SellerInfo current = sellers.get(playerId);
        if (current != null) {
            sellers.put(playerId, new SellerInfo(playerId, name, current.totalSales()));
        } else {
            sellers.put(playerId, new SellerInfo(playerId, name, 0L));
        }
    }

    public void addSale(UUID playerId, long saleAmount) {
        SellerInfo current = sellers.get(playerId);
        if (current != null) {
            sellers.put(playerId, current.addSales(saleAmount));
        } else {
            sellers.put(playerId, new SellerInfo(playerId, "Unknown", saleAmount));
        }
    }

    public Collection<SellerInfo> getAllSellers() {
        return sellers.values();
    }

    public void updateSellerNameIfChanged(UUID playerId, String currentName) {
        SellerInfo current = sellers.get(playerId);
        if (current != null && !current.name().equals(currentName)) {
            sellers.put(playerId, current.withName(currentName));
        }
    }

    @Override
    public void readFromNbt(CompoundTag nbt) {
        sellers.clear();
        ListTag sellerList = nbt.getList("Sellers", Tag.TAG_COMPOUND);
        for (Tag element : sellerList) {
            CompoundTag sellerNbt = (CompoundTag) element;
            SellerInfo info = SellerInfo.fromNbt(sellerNbt);
            sellers.put(info.id(), info);
        }
    }

    @Override
    public void writeToNbt(CompoundTag nbt) {
        ListTag sellerList = new ListTag();
        for (SellerInfo info : sellers.values()) {
            sellerList.add(info.toNbt());
        }
        nbt.put("Sellers", sellerList);
    }
}
