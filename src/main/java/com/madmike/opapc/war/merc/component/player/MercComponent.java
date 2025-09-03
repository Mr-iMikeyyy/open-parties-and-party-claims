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

package com.madmike.opapc.war.merc.component.player;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * Component that tracks whether a player is a mercenary
 * and their current hiring fee.
 */
public class MercComponent implements ComponentV3 {

    private final Player provider;

    private boolean isMerc = false;
    private long hiringFee = 0L;

    public MercComponent(Player player) {
        this.provider = player;
    }

    /* ----------------- Getters / Setters ----------------- */

    public boolean isMerc() {
        return isMerc;
    }

    public void setMerc(boolean merc) {
        this.isMerc = merc;
    }

    public long getHiringFee() {
        return hiringFee;
    }

    public void setHiringFee(long hiringFee) {
        this.hiringFee = hiringFee;
    }

    /* ----------------- NBT Persistence ----------------- */

    @Override
    public void readFromNbt(CompoundTag tag) {
        if (tag.contains("IsMerc")) {
            this.isMerc = tag.getBoolean("IsMerc");
        }
        if (tag.contains("HiringFee")) {
            this.hiringFee = tag.getLong("HiringFee");
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.putBoolean("IsMerc", this.isMerc);
        tag.putLong("HiringFee", this.hiringFee);
    }
}
