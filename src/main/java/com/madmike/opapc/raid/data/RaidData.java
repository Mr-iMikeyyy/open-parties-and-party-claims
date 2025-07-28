package com.madmike.opapc.raid.data;

import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

public class RaidData {
    private final long startTime;
    private final IServerPartyAPI defendingParty;

    public RaidData(IServerPartyAPI defendingParty) {
        this.startTime = System.currentTimeMillis();
        this.defendingParty = defendingParty;
    }
}
