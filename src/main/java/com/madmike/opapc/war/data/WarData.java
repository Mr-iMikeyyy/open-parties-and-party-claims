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

package com.madmike.opapc.war.data;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import com.madmike.opapc.player.component.scoreboard.PlayerNameComponent;
import com.madmike.opapc.war.merc.data.MercContract;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.List;
import java.util.UUID;

public class WarData {

    private final IServerPartyAPI attackingParty;
    private final IServerPartyAPI defendingParty;

    private final List<UUID> attackerIds;
    private List<UUID> attackersLeftToKill;

    private final List<UUID> defenderIds;

    private List<UUID> allyIds;

    private List<UUID> mercenaryIds;
    private List<MercContract> mercOffers;
    private List<MercContract> contracts;

    private final String attackingPartyName;
    private final String defendingPartyName;

    private final PartyClaim attackingClaim;
    private final PartyClaim defendingClaim;

    private final int durationSeconds;
    private final long durationMilli;

    private final boolean wipe;

    private long startTime;
    private boolean isExpired;
    private BlockPos warBlockPosition;
    private int warBlocksLeft;

    public WarData(IServerPartyAPI attackingParty, IServerPartyAPI defendingParty, PartyClaim defendingClaim, PartyClaim attackingClaim) {
        this.attackingParty = attackingParty;
        this.defendingParty = defendingParty;

        this.attackerIds = attackingParty.getOnlineMemberStream().map(ServerPlayer::getUUID).toList();
        this.attackersLeftToKill.addAll(attackerIds);

        this.defenderIds = defendingParty.getOnlineMemberStream().map(ServerPlayer::getUUID).toList();

        this.attackingPartyName = attackingClaim.getPartyName();

        this.defendingPartyName = defendingClaim.getPartyName();

        this.attackingClaim = attackingClaim;
        this.defendingClaim = defendingClaim;

        int defenderCount = this.defenderIds.size();

        this.durationSeconds = defenderCount * 3 * 60;
        this.durationMilli = this.durationSeconds * 1000L;
        if (this.defendingClaim.getClaimedChunksList().size() < defenderCount * 3) {
            this.wipe = true;
            this.warBlocksLeft = this.defendingClaim.getClaimedChunksList().size();
        }
        else {
            this.wipe = false;
            this.warBlocksLeft = defenderCount * 3;
        }

        this.isExpired = false;
    }

    // --- Core Getters ---
    public IServerPartyAPI getAttackingParty() { return attackingParty; }
    public IServerPartyAPI getDefendingParty() { return defendingParty; }
    public String getAttackingPartyName() { return attackingPartyName; }
    public String getDefendingPartyName() { return defendingPartyName; }
    public List<UUID> getAttackerIds() { return attackerIds; }
    public List<UUID> getAttackersLeftToKill() { return attackersLeftToKill; }
    public List<UUID> getDefenderIds() { return defenderIds; }
    public PartyClaim getDefendingClaim() { return defendingClaim; }
    public PartyClaim getAttackingClaim() { return attackingClaim; }
    public int getWarBlocksLeft() { return warBlocksLeft; }
    public int getDurationSeconds() { return durationSeconds; }
    public long getDurationMilli() { return durationMilli; }
    public boolean getWipe() { return wipe; }
    public BlockPos getWarBlockPosition() { return warBlockPosition; }


    // --- Time Keeping ---
    public long getSecondsRemaining() {
        if (startTime <= 0) return durationSeconds;
        long elapsedMillis = System.currentTimeMillis() - startTime;
        long remainingMillis = durationMilli - elapsedMillis;
        return Math.max(0, remainingMillis / 1000);
    }

    public boolean isExpired() {
        return isExpired;
    }

    // --- Tracking ---
    public boolean isPlayerParticipant(ServerPlayer player) {
        UUID id = player.getUUID();
        return attackerIds.contains(id) || defenderIds.contains(id) || mercenaryIds.contains(id);
    }

    public boolean isPartyParticipant(IServerPartyAPI party) {
        return attackingParty.equals(party) || defendingParty.equals(party);
    }

    // --- Setters / Mutators ---
    public void setWarBlockPosition(BlockPos newPos) { this.warBlockPosition = newPos; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public void decrementWarBlocksLeft() { warBlocksLeft--; }
    public void removeAttacker(UUID id) { attackersLeftToKill.remove(id); }
    public void removeMercenary(UUID id) { mercenaryIds.remove(id); }
    public void setIsExpired(boolean t) { this.isExpired = t; }

    // --- Messaging ---
    public void broadcastToAttackers(Component msg) {
        attackingParty.getOnlineMemberStream().forEach(p -> p.sendSystemMessage(msg));
    }

    public void broadcastToDefenders(Component msg) {
        defendingParty.getOnlineMemberStream().forEach(p -> p.sendSystemMessage(msg));
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
                .append(Component.literal("§eWar Blocks: §c" + warBlocksLeft + "\n"))
                .append(Component.literal("§eDuration: §7" + (durationSeconds / 60) + " min\n"))
                .append(Component.literal("§eTime Remaining: §a"
                        + (remainingSeconds / 60) + " min "
                        + (remainingSeconds % 60) + " sec\n"))
                .append(Component.literal("§eAttackers Remaining: §c" + attackerIds.size()))
                .append(Component.literal("§eClaim Wipe Possible: §c" + (wipe ? "Yes!" : "No") +"\n\n"));

        // List attackers
        PlayerNameComponent pnc = OPAPCComponents.PLAYER_NAMES.get(OPAPC.scoreboard());
        info.append(Component.literal("§cAttackers (" + attackerIds.size() + "):\n"));
        for (UUID id : attackerIds) {
            info.append(Component.literal(" §7- §c" + pnc.getPlayerNameById(id) + "\n"));
        }

        // List defenders
        info.append(Component.literal("§aDefenders (" + defenderIds.size() + "):\n"));
        for (UUID id : defenderIds) {
            info.append(Component.literal(" §7- §a" + pnc.getPlayerNameById(id) + "\n"));
        }

        return info;
    }
}
