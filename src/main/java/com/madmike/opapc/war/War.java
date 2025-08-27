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

package com.madmike.opapc.war;

import com.madmike.opapc.war.data.WarData;
import com.madmike.opapc.war.state.IWarState;
import com.madmike.opapc.war.state.WarDeclaredState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

public class War {
    private IWarState state;
    private final WarData data;

    public IWarState getState() {
        return state;
    }

    public War(WarData data) {
        this.data = data;
        this.state = new WarDeclaredState();
    }

    public void setState(IWarState state) {
        this.state = state;
        this.state.enter(this);
    }

    public WarData getData() {
        return data;
    }

    public void tick() {
        state.tick(this);
    }

    public void onAttackerDeath(ServerPlayer player) {
        state.onAttackerDeath(player, this);
    }

    public void onDefenderDeath(ServerPlayer player) {
        state.onDefenderDeath(player, this);
    }

    public void onAttackerQuit(ServerPlayer player) { state.onAttackerQuit(player, this);}

    public void onDefenderQuit(ServerPlayer player) { state.onDefenderQuit(player, this);}

    public void onBlockBroken(BlockPos pos) {
        state.onWarBlockBroken(pos, this);
    }

    public boolean isPlayerParticipant(ServerPlayer player) {
        return data.isPlayerParticipant(player);
    }

    public boolean isPartyParticipant(IServerPartyAPI party) {
        return data.isPartyParticipant(party);
    }

    public void onRequestInfo(ServerPlayer player) {
        player.sendSystemMessage(data.getInfo());
    }
}
