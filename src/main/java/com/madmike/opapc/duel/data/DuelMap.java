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

package com.madmike.opapc.duel.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public class DuelMap {
    private String name;
    private final List<BlockPos> player1Spawns;
    private final List<BlockPos> player2Spawns;

    public DuelMap(String name) {
        this.name = name;
        this.player1Spawns = new ArrayList<>();
        this.player2Spawns = new ArrayList<>();
    }

    public DuelMap(String name, List<BlockPos> player1Spawns, List<BlockPos> player2Spawns) {
        this.name = name;
        this.player1Spawns = player1Spawns;
        this.player2Spawns = player2Spawns;
    }

    public String getName() {
        return name;
    }

    public List<BlockPos> getPlayer1Spawns() {
        return player1Spawns;
    }

    public List<BlockPos> getPlayer2Spawns() {
        return player2Spawns;
    }

    public void addPlayer1Spawn(BlockPos pos) {
        player1Spawns.add(pos);
    }

    public void addPlayer2Spawn(BlockPos pos) {
        player2Spawns.add(pos);
    }

    // --- NBT Serialization ---
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);

        ListTag p1List = new ListTag();
        for (BlockPos pos : player1Spawns) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            p1List.add(posTag);
        }
        tag.put("Player1Spawns", p1List);

        ListTag p2List = new ListTag();
        for (BlockPos pos : player2Spawns) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            p2List.add(posTag);
        }
        tag.put("Player2Spawns", p2List);

        return tag;
    }

    public static DuelMap fromNbt(CompoundTag tag) {
        String name = tag.getString("Name");

        List<BlockPos> p1Spawns = new ArrayList<>();
        ListTag p1List = tag.getList("Player1Spawns", Tag.TAG_COMPOUND);
        for (Tag t : p1List) {
            if (t instanceof CompoundTag posTag) {
                p1Spawns.add(new BlockPos(
                        posTag.getInt("x"),
                        posTag.getInt("y"),
                        posTag.getInt("z")
                ));
            }
        }

        List<BlockPos> p2Spawns = new ArrayList<>();
        ListTag p2List = tag.getList("Player2Spawns", Tag.TAG_COMPOUND);
        for (Tag t : p2List) {
            if (t instanceof CompoundTag posTag) {
                p2Spawns.add(new BlockPos(
                        posTag.getInt("x"),
                        posTag.getInt("y"),
                        posTag.getInt("z")
                ));
            }
        }

        return new DuelMap(name, p1Spawns, p2Spawns);
    }
}
