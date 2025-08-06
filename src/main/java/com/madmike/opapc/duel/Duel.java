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

package com.madmike.opapc.duel;

import com.madmike.opapc.duel.data.DuelData;
import com.madmike.opapc.duel.state.DuelChallengedState;
import com.madmike.opapc.duel.state.IDuelState;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class Duel {
    private IDuelState state;
    private final DuelData data;

    public Duel(DuelData data) {
        this.data = data;
        this.state = new DuelChallengedState();
    }

    public void setState(IDuelState state) {
        this.state = state;
    }

    public DuelData getData() {
        return data;
    }

    public void tick() {
        state.tick(this);
    }

    public boolean isChallenger(UUID id) {
        return data.getChallengerId().equals(id);
    }

    public boolean isOpponent(UUID id) {
        return data.getOpponentId().equals(id);
    }

    public void handleChallengerDeath(ServerPlayer player) {
        state.onChallengerDeath(player, this);
    }

    public void handleOpponentDeath(ServerPlayer player) {
        state.onOpponentDeath(player, this);
    }

    public void end(EndOfDuelType type) {
        state.end(this, type);
    }
}
