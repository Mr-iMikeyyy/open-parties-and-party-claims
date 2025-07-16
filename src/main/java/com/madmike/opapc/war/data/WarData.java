package com.madmike.opapc.war.data;

import com.madmike.opapc.config.OPAPCConfig;
import com.madmike.opapc.war.WarManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class WarData {

    private final IServerPartyAPI attackingParty;
    private final IServerPartyAPI defendingParty;
    private final long startTime;
    private int attackerLivesRemaining;
    private int warBlocksLeft;
    private List<BlockPos> spawnedWarBlockPositions = new ArrayList<>();



    public WarData(IServerPartyAPI attackingParty, IServerPartyAPI defendingParty, List<BlockPos> spawnedWarBlockPositions) {
        this.attackingParty = attackingParty;
        this.defendingParty = defendingParty;
        this.startTime = System.currentTimeMillis();
        this.attackerLivesRemaining = OPAPCConfig.maxAttackerLives;
        this.warBlocksLeft = OPAPCConfig.unclaimBlocksPerWar;
        this.spawnedWarBlockPositions = spawnedWarBlockPositions;
    }

    public List<BlockPos> getSpawnedWarBlockPositions() {
        return spawnedWarBlockPositions;
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
        return defendingParty.getOnlineMemberStream();
    }

    public long getStartTime() {
        return startTime;
    }

    public int getAttackerLivesRemaining() {
        return attackerLivesRemaining;
    }

    public void decrementAttackerLivesRemaining() {
        attackerLivesRemaining--;

    }

    public int getWarBlocksLeft() {
        return warBlocksLeft;
    }

    public void decrementWarBlocksLeft() {
        --warBlocksLeft;
        if (this.warBlocksLeft <= 0) {
            WarManager.INSTANCE.endWar(this, WarManager.EndOfWarType.ALL_BLOCKS_BROKEN);
        }
        else {
            getAttackingPlayers().forEach(p -> p.sendSystemMessage(Component.literal("War Blocks left to find: " + getWarBlocksLeft())));
            getDefendingPlayers().forEach(p -> p.sendSystemMessage(Component.literal("War Blocks left to defend: " + getWarBlocksLeft())));
        }
    }
}
