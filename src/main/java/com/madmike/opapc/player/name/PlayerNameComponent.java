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

package com.madmike.opapc.player.name;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerNameComponent implements ComponentV3, AutoSyncedComponent {
    private final Map<UUID, String> idToNameMap = new HashMap<>();
    private Scoreboard provider;

    public PlayerNameComponent(Scoreboard sb, MinecraftServer server) {
        this.provider = sb;
    }

    public void addOrSet(ServerPlayer player) {
        idToNameMap.put(player.getUUID(), player.getGameProfile().getName());
    }

    public String getPlayerNameById(UUID id) {
        return idToNameMap.getOrDefault(id, "Unknown");
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        idToNameMap.clear();

        if (tag.contains("PlayerNames", Tag.TAG_LIST)) {
            ListTag list = tag.getList("PlayerNames", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                if (entry.hasUUID("UUID") && entry.contains("Name", Tag.TAG_STRING)) {
                    UUID uuid = entry.getUUID("UUID");
                    String name = entry.getString("Name");
                    idToNameMap.put(uuid, name);
                }
            }
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        ListTag list = new ListTag();

        for (Map.Entry<UUID, String> entry : idToNameMap.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID("UUID", entry.getKey());
            entryTag.putString("Name", entry.getValue());
            list.add(entryTag);
        }

        tag.put("PlayerNames", list);
    }
}
