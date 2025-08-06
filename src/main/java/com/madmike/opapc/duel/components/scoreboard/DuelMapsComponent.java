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

package com.madmike.opapc.duel.components.scoreboard;

import com.madmike.opapc.duel.data.DuelMap;
import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DuelMapsComponent implements ComponentV3 {

    private final List<DuelMap> duelMaps = new ArrayList<>();

    public List<DuelMap> getDuelMaps() {
        return duelMaps;
    }

    public void addMap(DuelMap map) {
        duelMaps.add(map);
    }

    public void removeMap(String name) {
        duelMaps.removeIf(map -> map.getName().equalsIgnoreCase(name));
    }

    public DuelMap getMapByName(String name) {
        return duelMaps.stream()
                .filter(map -> map.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        duelMaps.clear();
        ListTag listTag = tag.getList("DuelMaps", Tag.TAG_COMPOUND);
        for (Tag t : listTag) {
            if (t instanceof CompoundTag mapTag) {
                duelMaps.add(DuelMap.fromNbt(mapTag));
            }
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        ListTag listTag = new ListTag();
        for (DuelMap map : duelMaps) {
            listTag.add(map.toNbt());
        }
        tag.put("DuelMaps", listTag);
    }
}
