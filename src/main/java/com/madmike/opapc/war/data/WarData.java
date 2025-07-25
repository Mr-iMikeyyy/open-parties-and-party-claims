package com.madmike.opapc.war.data;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.party.data.PartyClaim;
import com.madmike.opapc.war.WarManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.stream.Stream;

public class WarData {

    private final IServerPartyAPI attackingParty;
    private final IServerPartyAPI defendingParty;
    private final long startTime;
    private final int durationSeconds;
    private int attackerLivesRemaining;
    private int warBlocksLeft;
    private BlockPos warBlockPosition;
    private boolean shouldTeleport;

    public WarData(IServerPartyAPI attackingParty, IServerPartyAPI defendingParty, BlockPos warBlockPosition, boolean shouldTeleport) {
        this.attackingParty = attackingParty;
        this.defendingParty = defendingParty;
        this.startTime = System.currentTimeMillis();
        this.warBlockPosition = warBlockPosition;
        this.shouldTeleport = shouldTeleport;

        int defenderCount = (int) defendingParty.getOnlineMemberStream().count();
        int attackerCount = (int) attackingParty.getOnlineMemberStream().count();

        this.attackerLivesRemaining = defenderCount * 3;
        this.warBlocksLeft = defenderCount * 3;
        this.durationSeconds = defenderCount * 60;

        applyBuffs(defenderCount, attackerCount);
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

    public IServerPartyAPI getDefendingParty() {
        return defendingParty;
    }

    public Stream<ServerPlayer> getAttackingPlayers() {
        return attackingParty.getOnlineMemberStream();
    }

    public Stream<ServerPlayer> getDefendingPlayers() {
        return defendingParty.getOnlineMemberStream();
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
        if (this.warBlocksLeft <= 0) {
            WarManager.INSTANCE.endWar(this, WarManager.EndOfWarType.ALL_BLOCKS_BROKEN);
        }
        else {
            getAttackingPlayers().forEach(p -> p.sendSystemMessage(Component.literal("War Blocks left to find: " + getWarBlocksLeft())));
            getDefendingPlayers().forEach(p -> p.sendSystemMessage(Component.literal("War Blocks left to defend: " + getWarBlocksLeft())));
        }
    }

    public boolean isExpired() {
        long elapsed = System.currentTimeMillis() - startTime;
        return elapsed >= (durationSeconds * 1000L);
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
}
