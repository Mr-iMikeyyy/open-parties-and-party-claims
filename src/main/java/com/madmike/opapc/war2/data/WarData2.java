package com.madmike.opapc.war2.data;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;
import xaero.pac.common.server.player.config.api.PlayerConfigOptions;

import java.util.List;

public class WarData2 {

    private final IServerPartyAPI attackingParty;
    private final IServerPartyAPI defendingParty;
    private final List<ServerPlayer> attackers;
    private final List<ServerPlayer> defenders;

    private final String attackingPartyName;
    private final String defendingPartyName;

    private final int durationSeconds;
    private final long durationMilli;

    private long startTime;
    private BlockPos warBlockPosition;
    private boolean warp;
    private int attackerLivesRemaining;
    private int warBlocksLeft;

    public WarData2(IServerPartyAPI attackingParty, IServerPartyAPI defendingParty, boolean warp) {
        this.attackingParty = attackingParty;
        this.defendingParty = defendingParty;

        this.attackers = attackingParty.getOnlineMemberStream().toList();
        this.defenders = defendingParty.getOnlineMemberStream().toList();

        this.attackingPartyName = OPAPC.getPlayerConfigs()
                .getLoadedConfig(attackingParty.getOwner().getUUID())
                .getFromEffectiveConfig(PlayerConfigOptions.PARTY_NAME);

        this.defendingPartyName = OPAPC.getPlayerConfigs()
                .getLoadedConfig(defendingParty.getOwner().getUUID())
                .getFromEffectiveConfig(PlayerConfigOptions.PARTY_NAME);

        int defenderCount = this.defenders.size();

        this.attackerLivesRemaining = defenderCount * 3;
        this.warBlocksLeft = defenderCount * 3;
        this.durationSeconds = defenderCount * 3 * 60;
        this.durationMilli = this.durationSeconds * 1000L;
        this.warp = warp;
    }

    // --- Core Getters ---
    public IServerPartyAPI getAttackingParty() { return attackingParty; }
    public IServerPartyAPI getDefendingParty() { return defendingParty; }
    public String getAttackingPartyName() { return attackingPartyName; }
    public String getDefendingPartyName() { return defendingPartyName; }
    public List<ServerPlayer> getAttackingPlayers() { return attackers; }
    public List<ServerPlayer> getDefendingPlayers() { return defenders; }
    public int getAttackerLivesRemaining() { return attackerLivesRemaining; }
    public int getWarBlocksLeft() { return warBlocksLeft; }
    public int getDurationSeconds() { return durationSeconds; }
    public long getDurationMilli() {return durationMilli; }
    public boolean getWarp() { return warp; }
    public BlockPos getWarBlockPosition() { return warBlockPosition; }

    // --- Time Keeping ---
    public long getSecondsRemaining() {
        if (startTime <= 0) return durationSeconds;
        long elapsedMillis = System.currentTimeMillis() - startTime;
        long remainingMillis = durationMilli - elapsedMillis;
        return Math.max(0, remainingMillis / 1000);
    }

    public boolean isExpired() {
        if (startTime <= 0) {
            return false; // war hasn't started yet
        }
        return System.currentTimeMillis() - startTime >= durationMilli;
    }

    // --- Setters / Mutators ---
    public void setWarp(boolean warp) { this.warp = warp; }
    public void setWarBlockPosition(BlockPos newPos) { this.warBlockPosition = newPos; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public void decrementAttackerLivesRemaining() { attackerLivesRemaining--; }
    public void decrementWarBlocksLeft() { warBlocksLeft--; }

    // --- Party Claims ---
    public PartyClaim getDefendingClaim() {
        return OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard())
                .getClaim(defendingParty.getId());
    }

    public PartyClaim getAttackingClaim() {
        return OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard())
                .getClaim(attackingParty.getId());
    }

    // --- Messaging ---
    public void broadcastToAttackers(Component msg) {
        attackers.forEach(p -> p.sendSystemMessage(msg));
    }

    public void broadcastToDefenders(Component msg) {
        defenders.forEach(p -> p.sendSystemMessage(msg));
    }

    public void broadcastToWar(Component msg) {
        broadcastToAttackers(msg);
        broadcastToDefenders(msg);
    }

    // --- Info Builder ---
    public Component getInfo() {
        long elapsedSeconds = startTime > 0
                ? (System.currentTimeMillis() - startTime) / 1000
                : 0;
        long remainingSeconds = Math.max(durationSeconds - elapsedSeconds, 0);

        // Build base info
        MutableComponent info = Component.literal("§6--- War Info ---\n")
                .append(Component.literal("§eAttacking Party: §c" + attackingPartyName + "\n"))
                .append(Component.literal("§eDefending Party: §a" + defendingPartyName + "\n"))
                .append(Component.literal("§eLives Remaining: §c" + attackerLivesRemaining + "\n"))
                .append(Component.literal("§eWar Blocks Left: §c" + warBlocksLeft + "\n"))
                .append(Component.literal("§eDuration: §7" + (durationSeconds / 60) + " min\n"))
                .append(Component.literal("§eTime Remaining: §a"
                        + (remainingSeconds / 60) + " min "
                        + (remainingSeconds % 60) + " sec\n"))
                .append(Component.literal("§eWarp Enabled: §b" + (warp ? "Yes" : "No") + "\n\n"));

        // List attackers
        info.append(Component.literal("§cAttackers (" + attackers.size() + "):\n"));
        for (ServerPlayer player : attackers) {
            info.append(Component.literal(" §7- §c" + player.getName().getString() + "\n"));
        }

        // List defenders
        info.append(Component.literal("§aDefenders (" + defenders.size() + "):\n"));
        for (ServerPlayer player : defenders) {
            info.append(Component.literal(" §7- §a" + player.getName().getString() + "\n"));
        }

        return info;
    }
}
