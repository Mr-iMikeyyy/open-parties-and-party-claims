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

import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.trade.data.OfflineSale;
import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Scoreboard;

import java.util.*;

public class OfflineSalesComponent implements Component {

    private final Scoreboard scoreboard;
    private final MinecraftServer server;

    private final Map<UUID, List<OfflineSale>> offlineSales = new HashMap<>();

    public OfflineSalesComponent(Scoreboard scoreboard, MinecraftServer server) {
        this.scoreboard = scoreboard;
        this.server = server;
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        offlineSales.clear();

        if (tag.contains("OfflineSales", Tag.TAG_COMPOUND)) {
            CompoundTag salesTag = tag.getCompound("OfflineSales");
            for (String key : salesTag.getAllKeys()) {
                try {
                    UUID id = UUID.fromString(key);
                    ListTag saleList = salesTag.getList(key, Tag.TAG_COMPOUND);

                    List<OfflineSale> parsed = new ArrayList<>();
                    for (int i = 0; i < saleList.size(); i++) {
                        parsed.add(OfflineSale.fromNbt(saleList.getCompound(i)));
                    }
                    if (!parsed.isEmpty()) {
                        offlineSales.put(id, parsed);
                    }
                } catch (IllegalArgumentException ignored) {
                    // skip invalid UUID keys
                }
            }
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        CompoundTag salesTag = new CompoundTag();
        for (Map.Entry<UUID, List<OfflineSale>> entry : offlineSales.entrySet()) {
            ListTag saleList = new ListTag();
            for (OfflineSale sale : entry.getValue()) {
                saleList.add(sale.toNbt());
            }
            salesTag.put(entry.getKey().toString(), saleList);
        }
        tag.put("OfflineSales", salesTag);
    }

    /* ---------------- Public API ---------------- */

    public void addSale(UUID sellerID, long profit) {
        offlineSales
                .computeIfAbsent(sellerID, k -> new ArrayList<>())
                .add(new OfflineSale(profit));
    }

    public List<OfflineSale> getSales(UUID sellerID) {
        return offlineSales.getOrDefault(sellerID, Collections.emptyList());
    }

    public boolean hasSales(UUID sellerID) {
        return offlineSales.containsKey(sellerID);
    }

    public void clearSales(UUID sellerID) {
        if (offlineSales.remove(sellerID) != null) {
            OPAPCComponents.OFFLINE_SALES.sync(scoreboard);
        }
    }

    public Map<UUID, List<OfflineSale>> getAllOfflineSales() {
        return offlineSales;
    }
}
