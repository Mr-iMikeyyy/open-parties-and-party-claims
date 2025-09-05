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
import com.madmike.opapc.OPAPCConfig;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import com.madmike.opapc.war.merc.data.MercContract;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.*;
import java.util.stream.Collectors;


public class WarData {
    // ---- Identity (lookup-safe) ----
    private final UUID attackingPartyId;
    private final UUID defendingPartyId;

    // Names captured at creation time (stable for messaging; party may be renamed later)
    private final String attackingPartyName;
    private final String defendingPartyName;

    // ---- Participants ----
    private final Set<UUID> attackerIds;           // all attackers at start
    private final Set<UUID> defendersIds;          // all defenders at start
    private final Set<UUID> attackersLeftToKill;   // remaining attackers to eliminate

    private Set<UUID> allyIds = new HashSet<>();
    private Set<UUID> mercenaryIds = new HashSet<>();
    private Set<MercContract> contracts = new HashSet<>();

    // ---- War parameters ----
    private final int durationSeconds;
    private final long durationMilli;
    private final boolean wipe;

    // ---- Runtime state ----
    private long startTime;
    private boolean isExpired;
    private BlockPos warBlockPosition;
    private int warBlocksLeft;

    /* ---------------- Constructor ---------------- */

    public WarData(IServerPartyAPI attackingParty, IServerPartyAPI defendingParty, PartyClaim defendingClaim, PartyClaim attackingClaim) {

        // Identity
        this.attackingPartyId = attackingParty.getId();
        this.defendingPartyId = defendingParty.getId();

        // Names captured now (safe for messaging even if party names change later)
        this.attackingPartyName = attackingClaim.getPartyName();
        this.defendingPartyName = defendingClaim.getPartyName();

        // Participants captured from current online members
        this.attackerIds = attackingParty.getOnlineMemberStream()
                .map(ServerPlayer::getUUID)
                .collect(Collectors.toCollection(HashSet::new));

        this.defendersIds = defendingParty.getOnlineMemberStream()
                .map(ServerPlayer::getUUID)
                .collect(Collectors.toCollection(HashSet::new));

        // Attackers to kill starts as a copy of all attackers
        this.attackersLeftToKill = new HashSet<>(this.attackerIds);

        // Duration scaled by defender count
        int defenderCount = this.defendersIds.size();
        this.durationSeconds = defenderCount * OPAPCConfig.warSecondsPerDefender;
        this.durationMilli = this.durationSeconds * 1000L;

        // War blocks & wipe decision based on defending claim size
        int claimedChunks = defendingClaim.getClaimedChunksList().size();

        if (claimedChunks < defenderCount * 3) {
            this.wipe = true;
            this.warBlocksLeft = claimedChunks;
        } else {
            this.wipe = false;
            this.warBlocksLeft = defenderCount * 3;
        }

        this.isExpired = false;
    }

    /* ---------------- On-demand resolvers (no long-lived refs) ---------------- */

    public IServerPartyAPI getAttackingParty() {
        return OPAPC.parties().getPartyById(attackingPartyId);
    }

    public IServerPartyAPI getDefendingParty() {
        return OPAPC.parties().getPartyById(attackingPartyId);
    }

    public PartyClaim getAttackingClaim() {
        return OPAPCComponents.PARTY_CLAIMS.get(OPAPC.scoreboard()).getClaim(attackingPartyId);
    }

    public PartyClaim getDefendingClaim() {
        return OPAPCComponents.PARTY_CLAIMS.get(OPAPC.scoreboard()).getClaim(defendingPartyId);
    }

    /* ---------------- Core getters ---------------- */

    public UUID getAttackingPartyId() {
        return attackingPartyId;
    }

    public UUID getDefendingPartyId() {
        return defendingPartyId;
    }

    public String getAttackingPartyName() {
        return attackingPartyName;
    }

    public String getDefendingPartyName() {
        return defendingPartyName;
    }

    public Set<UUID> getAttackerIds() {
        return Collections.unmodifiableSet(attackerIds);
    }

    public Set<UUID> getAttackersLeftToKill() {
        return Collections.unmodifiableSet(attackersLeftToKill);
    }

    public Set<UUID> getDefenderIds() {
        return Collections.unmodifiableSet(defendersIds);
    }

    public Set<UUID> getAllyIds() {
        return Collections.unmodifiableSet(allyIds);
    }

    public Set<UUID> getMercenaryIds() {
        return Collections.unmodifiableSet(mercenaryIds);
    }

    public int getWarBlocksLeft() {
        return warBlocksLeft;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public long getDurationMilli() {
        return durationMilli;
    }

    public boolean getWipe() {
        return wipe;
    }

    public BlockPos getWarBlockPosition() {
        return warBlockPosition;
    }

    /* ---------------- Time keeping ---------------- */

    public long getSecondsRemaining() {
        if (startTime <= 0) return durationSeconds;
        long elapsedMillis = System.currentTimeMillis() - startTime;
        long remainingMillis = durationMilli - elapsedMillis;
        return Math.max(0, remainingMillis / 1000);
    }

    public boolean isExpired() {
        return isExpired;
    }

    /* ---------------- Participation / checks ---------------- */

    public boolean isPlayerParticipant(UUID id) {
        return attackerIds.contains(id) || defendersIds.contains(id) || mercenaryIds.contains(id) || allyIds.contains(id);
    }

    public boolean isPartyParticipant(UUID partyId) {
        return attackingPartyId.equals(partyId) || defendingPartyId.equals(partyId);
    }

    /* ---------------- Mutators ---------------- */

    public void setWarBlockPosition(BlockPos newPos) {
        this.warBlockPosition = newPos;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void decrementWarBlocksLeft() {
        warBlocksLeft = Math.max(0, warBlocksLeft - 1);
    }

    public void removeAttacker(UUID id) {
        attackersLeftToKill.remove(id);
    }

    public void removeMercenary(UUID id) {
        mercenaryIds.remove(id);
    }

    public void setIsExpired(boolean t) {
        this.isExpired = t;
    }

    /* ---------------- Messaging ---------------- */

    public void broadcastToAttackers(Component msg) {
        IServerPartyAPI party = getAttackingParty();
        if (party != null) party.getOnlineMemberStream().forEach(p -> p.sendSystemMessage(msg));
    }

    public void broadcastToDefenders(Component msg) {
        IServerPartyAPI party = getDefendingParty();
        if (party != null) party.getOnlineMemberStream().forEach(p -> p.sendSystemMessage(msg));
    }

    public void broadcastToWar(Component msg) {
        broadcastToAttackers(msg);
        broadcastToDefenders(msg);
    }

    /* ---------------- Info builder ---------------- */

    public Component getInfo() {
        long elapsedSeconds = startTime > 0 ? (System.currentTimeMillis() - startTime) / 1000 : 0;
        long remainingSeconds = Math.max(durationSeconds - elapsedSeconds, 0);

        MutableComponent info = Component.literal("§6--- War Info ---\n")
                .append(Component.literal("§eAttacking Party: §c" + attackingPartyName + "\n"))
                .append(Component.literal("§eDefending Party: §a" + defendingPartyName + "\n"))
                .append(Component.literal("§eWar Blocks: §c" + warBlocksLeft + "\n"))
                .append(Component.literal("§eDuration: §7" + (durationSeconds / 60) + " min\n"))
                .append(Component.literal("§eTime Remaining: §a"
                        + (remainingSeconds / 60) + " min "
                        + (remainingSeconds % 60) + " sec\n"))
                .append(Component.literal("§eAttackers Remaining: §c" + attackersLeftToKill.size() + "\n"))
                .append(Component.literal("§eClaim Wipe Possible: §c" + (wipe ? "Yes!" : "No") + "\n\n"));

        var pnc = OPAPCComponents.PLAYER_NAMES.get(OPAPC.scoreboard());

        info.append(Component.literal("§cAttackers (" + attackerIds.size() + "):\n"));
        for (UUID id : attackerIds) {
            info.append(Component.literal(" §7- §c" + pnc.getPlayerNameById(id) + "\n"));
        }

        info.append(Component.literal("§aDefenders (" + defendersIds.size() + "):\n"));
        for (UUID id : defendersIds) {
            info.append(Component.literal(" §7- §a" + pnc.getPlayerNameById(id) + "\n"));
        }

        if (!mercenaryIds.isEmpty()) {
            info.append(Component.literal("§eMercenaries (" + mercenaryIds.size() + "):\n"));
            for (UUID id : mercenaryIds) {
                info.append(Component.literal(" §7- §e" + pnc.getPlayerNameById(id) + "\n"));
            }
        }

        if (!allyIds.isEmpty()) {
            info.append(Component.literal("§bAllies (" + allyIds.size() + "):\n"));
            for (UUID id : allyIds) {
                info.append(Component.literal(" §7- §b" + pnc.getPlayerNameById(id) + "\n"));
            }
        }

        return info;
    }
}
