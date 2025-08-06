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

package com.madmike.opapc.duel.components.player;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class DuelStatsComponent implements ComponentV3 {
    private int duelsWon;
    private int duelsLost;
    private long wagersWon;
    private long wagersLost;

    private Player player;

    public DuelStatsComponent(Player player) {
        this.player = player;
    }

    // Getters
    public int getDuelsWon() {
        return duelsWon;
    }

    public int getDuelsLost() {
        return duelsLost;
    }

    public long getWagersWon() {
        return wagersWon;
    }

    public long getWagersLost() {
        return wagersLost;
    }

    // Setters
    public void setDuelsWon(int duelsWon) {
        this.duelsWon = duelsWon;
    }

    public void setDuelsLost(int duelsLost) {
        this.duelsLost = duelsLost;
    }

    public void setWagersWon(long wagersWon) {
        this.wagersWon = wagersWon;
    }

    public void setWagersLost(long wagersLost) {
        this.wagersLost = wagersLost;
    }

    // Increment helpers
    public void addDuelWon() {
        this.duelsWon++;
    }

    public void addDuelLost() {
        this.duelsLost++;
    }

    public void addWagersWon(long amount) {
        this.wagersWon += amount;
    }

    public void addWagersLost(long amount) {
        this.wagersLost += amount;
    }

    // NBT Persistence
    @Override
    public void readFromNbt(CompoundTag tag) {
        if (tag.contains("DuelsWon")) {
            this.duelsWon = tag.getInt("DuelsWon");
        }
        if (tag.contains("DuelsLost")) {
            this.duelsLost = tag.getInt("DuelsLost");
        }
        if (tag.contains("WagersWon")) {
            this.wagersWon = tag.getLong("WagersWon");
        }
        if (tag.contains("WagersLost")) {
            this.wagersLost = tag.getLong("WagersLost");
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.putInt("DuelsWon", this.duelsWon);
        tag.putInt("DuelsLost", this.duelsLost);
        tag.putLong("WagersWon", this.wagersWon);
        tag.putLong("WagersLost", this.wagersLost);
    }
}
