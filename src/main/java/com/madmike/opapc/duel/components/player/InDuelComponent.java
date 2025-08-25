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
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class InDuelComponent implements ComponentV3, AutoSyncedComponent {
    private final Player owner;
    private boolean inDuel;

    public InDuelComponent(Player owner) {
        this.owner = owner;
    }

    public boolean isInDuel() {
        return inDuel;
    }

    /**
     * SERVER ONLY: flips the flag and syncs to the owner client.
     * Pass the ServerPlayer owner (same entity) to trigger sync.
     */
    public void setInDuel(boolean value) {
        if (this.inDuel != value) {
            this.inDuel = value;
        }
    }

    // ---- Persistence ----
    @Override
    public void readFromNbt(CompoundTag tag) {
        this.inDuel = tag.getBoolean("InDuel");
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.putBoolean("InDuel", this.inDuel);
    }

    // ---- Autosync behavior ----
    /**
     * Restrict who receives autosync packets. We only send to the owner.
     * (If you prefer default behavior, you can delete this override.)
     */
    @Override
    public boolean shouldSyncWith(ServerPlayer recipient) {
        return recipient.getUUID().equals(owner.getUUID());
    }
}
