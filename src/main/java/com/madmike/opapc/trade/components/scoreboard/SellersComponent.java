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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    public SellerInfo getSellerInfo(UUID playerId) {
        return sellers.get(playerId);
    }

    public void addSale(UUID playerId, long saleAmount) {
        sellers.merge(playerId, new SellerInfo(saleAmount), (oldVal, add) -> oldVal.addSales(saleAmount));
    }

    public Collection<SellerInfo> getAllSellers() {
        return sellers.values();
    }

    @Override
    public void readFromNbt(CompoundTag nbt) {
        sellers.clear();

        if (nbt.contains("Sellers", Tag.TAG_COMPOUND)) {
            CompoundTag sellersTag = nbt.getCompound("Sellers");
            for (String key : sellersTag.getAllKeys()) {
                try {
                    UUID id = UUID.fromString(key);
                    CompoundTag data = sellersTag.getCompound(key);
                    SellerInfo info = SellerInfo.fromNbt(data);
                    sellers.put(id, info);
                } catch (IllegalArgumentException e) {
                    // skip invalid UUID keys
                }
            }
        }
    }

    @Override
    public void writeToNbt(@NotNull CompoundTag nbt) {
        CompoundTag sellersTag = new CompoundTag();
        for (Map.Entry<UUID, SellerInfo> e : sellers.entrySet()) {
            sellersTag.put(e.getKey().toString(), e.getValue().toNbt());
        }
        nbt.put("Sellers", sellersTag);
    }
}


