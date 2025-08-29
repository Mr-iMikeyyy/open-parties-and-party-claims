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

package com.madmike.opapc.util;

import com.madmike.opapc.OPAPC;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;
import xaero.pac.common.server.player.config.api.IPlayerConfigAPI;
import xaero.pac.common.server.player.config.api.IPlayerConfigManagerAPI;
import xaero.pac.common.server.player.config.api.PlayerConfigOptions;

import java.util.UUID;

public class PartyLookup {
    /**
     * Find a party by its name (case-insensitive).
     */
    public static IServerPartyAPI findByName(String inputName) {
        IPartyManagerAPI pm = OPAPC.parties();
        IPlayerConfigManagerAPI cm = OPAPC.playerConfigs();

        return pm.getAllStream()
                .filter(party -> {
                    UUID ownerId = party.getOwner().getUUID();
                    IPlayerConfigAPI ownerConfig = cm.getLoadedConfig(ownerId);

                    String partyName = ownerConfig.getEffective(PlayerConfigOptions.PARTY_NAME);
                    return partyName.equalsIgnoreCase(inputName);
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * Get the party owned by the given player or null if none exist.
     */
    public static IServerPartyAPI getOwnerParty(ServerPlayer player) {
        return OPAPC.parties().getPartyByOwner(player.getUUID());
    }

    /**
     * Get the party a player is currently a member of.
     */
    public static IServerPartyAPI getMemberParty(ServerPlayer player) {
        return OPAPC.parties().getPartyByMember(player.getUUID());
    }
}
