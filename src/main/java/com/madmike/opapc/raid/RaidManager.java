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

package com.madmike.opapc.raid;

import com.madmike.opapc.partyclaim.data.PartyClaim;
import com.madmike.opapc.war.War;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RaidManager {
    public static final RaidManager INSTANCE = new RaidManager();

    private final List<Raid> activeRaids = new ArrayList<>();

    public void startRaid() {

    }

    public void tickAll() {
        Iterator<Raid> it = activeRaids.iterator();
        while (it.hasNext()) {
            Raid raid = it.next();
            raid.tick();

            if (raid.getState() instanceof RaidEndedState) {
                it.remove();
            }
        }
    }

    public void handlePlayerDeath(ServerPlayer player, Raid raid) {

    }

    public void handleBlockBroken() {

    }

    public Raid findRaidByParty(IServerPartyAPI party) {
        for (Raid raid : activeRaids) {
            if (raid.isPartyParticipant(party)) {
                return raid;
            }
        }
        return null;
    }

    public Raid findRaidByClaim(PartyClaim claim) {
        
    }

    public Raid findRaidByPlayer(ServerPlayer player) {

    }

    public boolean hasRaids() {
        return !activeRaids.isEmpty();
    }
}
