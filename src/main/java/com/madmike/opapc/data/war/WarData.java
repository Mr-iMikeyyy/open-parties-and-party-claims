package com.madmike.opapc.data.war;

import com.madmike.opapc.config.OPAPCConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.stream.Stream;

public class WarData {

    private final IServerPartyAPI attackingParty;
    private final IServerPartyAPI defendingParty;
    private final long startTime;
    private int attackerLivesRemaining;
    private int unclaimBlocksLeft;

    public WarData(IServerPartyAPI attackingParty, IServerPartyAPI defendingParty) {
        this.attackingParty =attackingParty;
        this.defendingParty = defendingParty;
        this.startTime = System.currentTimeMillis();
        this.attackerLivesRemaining = OPAPCConfig.maxAttackerLives;
        this.unclaimBlocksLeft = OPAPCConfig.unclaimBlocksPerWar;
    }

    public IServerPartyAPI getAttackingParty() {
        return attackingParty;
    }

    public IServerPartyAPI getDefendingParty() {
        return defendingParty;
    }

    public Stream<ServerPlayer> getAttackingPlayers() {
        return attackingParty.getOnlineMemberStream();
    }

    public Stream<ServerPlayer> getDefendingPlayers() {
        return attackingParty.getOnlineMemberStream();
    }

    public long getStartTime() {
        return startTime;
    }

    public int getAttackerLivesRemaining() {
        return attackerLivesRemaining;
    }

    public void setAttackerLivesRemaining(int attackerLivesRemaining) {
        this.attackerLivesRemaining = attackerLivesRemaining;
    }
}
