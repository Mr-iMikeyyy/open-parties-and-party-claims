package com.madmike.opapc.war.data;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;
import xaero.pac.common.server.player.config.api.PlayerConfigOptions;

import java.util.List;

public class WarData {

    private final IServerPartyAPI attackingParty;
    private final IServerPartyAPI defendingParty;

    private final List<ServerPlayer> attackers;
    private final List<ServerPlayer> defenders;

    private final long startTime;
    private BlockPos warBlockPosition;
    private boolean warp;

    private int attackerLivesRemaining;
    private int warBlocksLeft;

    private final int durationSeconds;
    private final long durationMilli;


    public WarData(IServerPartyAPI attackingParty, IServerPartyAPI defendingParty) {
        this.attackingParty = attackingParty;
        this.defendingParty = defendingParty;
        this.attackers = attackingParty.getOnlineMemberStream().toList();
        this.defenders = defendingParty.getOnlineMemberStream().toList();
        this.startTime = System.currentTimeMillis();

        int defenderCount = this.defenders.size();
        int attackerCount = this.attackers.size();

        this.attackerLivesRemaining = defenderCount * 3;
        this.warBlocksLeft = defenderCount * 3;
        this.durationSeconds = defenderCount * 3 * 60;
        this.durationMilli = this.durationSeconds * 1000L;

        applyBuffs(defenderCount, attackerCount);
    }

    public boolean getWarp() {
        return this.warp;
    }

    public void setWarp(boolean b) {
        this.warp = b;
    }

    public BlockPos getWarBlockPosition() {
        return warBlockPosition;
    }

    public void setWarBlockPosition(BlockPos newPos) {
        this.warBlockPosition = newPos;
    }

    public IServerPartyAPI getAttackingParty() {
        return attackingParty;
    }

    public String getAttackingPartyName() {
        return OPAPC.getPlayerConfigs().getLoadedConfig(attackingParty.getOwner().getUUID()).getFromEffectiveConfig(PlayerConfigOptions.PARTY_NAME);
    }

    public IServerPartyAPI getDefendingParty() {
        return defendingParty;
    }

    public String getDefendingPartyName() {
        return OPAPC.getPlayerConfigs().getLoadedConfig(defendingParty.getOwner().getUUID()).getFromEffectiveConfig(PlayerConfigOptions.PARTY_NAME);
    }

    public List<ServerPlayer> getAttackingPlayers() {
        return attackers;
    }

    public List<ServerPlayer> getDefendingPlayers() {
        return defenders;
    }

    public PartyClaim getDefendingClaim() {
        return OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard()).getClaim(defendingParty.getId());
    }

    public PartyClaim getAttackingClaim() {
        return OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard()).getClaim(attackingParty.getId());
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
    }

    public boolean isExpired() {
        long elapsed = System.currentTimeMillis() - startTime;
        return elapsed >= durationMilli;
    }

    public void applyBuffs(int defenderCount, int attackerCount) {
        if (defenderCount < attackerCount) {
            int amp = attackerCount - defenderCount;
            getDefendingPlayers().forEach(e -> {
                e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, durationSeconds, amp, true, true));
                e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, durationSeconds, amp, true, true));
            });
        }
        else if (defenderCount > attackerCount) {
            int amp = defenderCount - attackerCount;
            getAttackingPlayers().forEach(e -> {
                e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, durationSeconds, amp, true, true));
                e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, durationSeconds, amp, true, true));
            });
        }
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }
}
