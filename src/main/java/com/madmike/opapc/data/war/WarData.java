package com.madmike.opapc.data.war;

import com.madmike.opapc.config.OPAPCConfig;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.UUID;

public class WarData {
    private UUID id;
    private IServerPartyAPI attackingParty;
    private IServerPartyAPI defendingParty;
    private long startTime;
    private int attackerLivesRemaining;

    public WarData(UUID id, IServerPartyAPI attackingParty, IServerPartyAPI defendingParty) {
        this.id = id;
        this.attackingParty =attackingParty;
        this.defendingParty = defendingParty;
        this.startTime = System.currentTimeMillis();
        this.attackerLivesRemaining = OPAPCConfig.maxAttackerLives;
    }
}
