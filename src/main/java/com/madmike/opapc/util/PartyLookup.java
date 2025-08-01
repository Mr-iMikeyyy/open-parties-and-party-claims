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
        IPartyManagerAPI pm = OPAPC.getPartyManager();
        IPlayerConfigManagerAPI cm = OPAPC.getPlayerConfigs();

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
        return OPAPC.getPartyManager().getPartyByOwner(player.getUUID());
    }

    /**
     * Get the party a player is currently a member of.
     */
    public static IServerPartyAPI getMemberParty(ServerPlayer player) {
        return OPAPC.getPartyManager().getPartyByMember(player.getUUID());
    }
}
