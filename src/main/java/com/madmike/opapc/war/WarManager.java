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

import com.madmike.opapc.partyclaim.data.PartyClaim;
import com.madmike.opapc.warp.util.SafeWarpHelper;
import com.madmike.opapc.war.data.WarData;
import com.madmike.opapc.war.event.bus.WarEventBus;
import com.madmike.opapc.war.event.events.WarDeclaredEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class WarManager {
    private final List<War> activeWars = new ArrayList<>();

    public static WarManager INSTANCE = new WarManager();

    public List<War> getActiveWars() {
        return activeWars;
    }

    public void declareWar(IServerPartyAPI attackingParty, IServerPartyAPI defendingParty, PartyClaim attackingClaim, PartyClaim defendingClaim, BlockPos ownersPos) {
        WarData data = new WarData(attackingParty, defendingParty, attackingClaim, defendingClaim);
        War war = new War(data);  // starts in PreparingState
        activeWars.add(war);
        WarEventBus.post(new WarDeclaredEvent(war));
        for (ServerPlayer player : attackingParty.getOnlineMemberStream().toList()) {
            SafeWarpHelper.warpPlayerToOverworldPos(player, ownersPos);
        }
    }

    public void tickAll() {
        Iterator<War> it = activeWars.iterator();
        while (it.hasNext()) {
            War war = it.next();
            war.tick();

            if (war.getData().isExpired()) {
                it.remove();
            }
        }
    }

    public void handlePlayerDeath(ServerPlayer player, War war) {
            WarData data = war.getData();
            if (data.getAttackerIds().contains(player.getUUID())) {
                war.onAttackerDeath(player);
            }
            else {
                war.onDefenderDeath(player);
            }
    }

    public void handleWarBlockBroken(BlockPos pos) {
        for (War war : activeWars) {
            if (war.getData().getWarBlockPosition().equals(pos)) {
                war.onBlockBroken(pos);
                break;
            }
        }
    }

    public War findWarByPlayer(ServerPlayer player) {
        for (War war : activeWars) {
            if (war.isPlayerParticipant(player)) {
                return war;
            }
        }
        return null;
    }

    public War findWarByParty(IServerPartyAPI party) {
        for (War war : activeWars) {
            if (war.isPartyParticipant(party)) {
                return war;
            }
        }
        return null;
    }

    public boolean isWarActive() {
        return !activeWars.isEmpty();
    }
}
