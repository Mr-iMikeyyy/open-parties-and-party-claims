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

    // We store the party IDs for claims; resolve via component on demand
    private final UUID attackingClaimPartyId;
    private final UUID defendingClaimPartyId;

    // ---- Participants ----
    private final Set<UUID> attackerIds;           // all attackers at start
    private final Set<UUID> defendersIds;          // all defenders at start
    private final Set<UUID> attackersLeftToKill;   // remaining attackers to eliminate

    private final Set<UUID> allyIds;               // optional: allies
    private final Set<UUID> mercenaryIds;          // optional: mercs

    private final List<MercContract> contracts;    // keep as list unless you need set semantics + equals/hashCode

    // ---- War parameters ----
    private final int durationSeconds;
    private final long durationMilli;
    private final boolean wipe;

    // ---- Runtime state ----
    private long startTime;
    private boolean isExpired;
    private BlockPos warBlockPosition;
    private int warBlocksLeft;

    /* ---------------- Constructors ---------------- */

    /**
     * Preferred constructor: supply party IDs; members will be captured from online streams at creation time.
     */
    public WarData(UUID attackingPartyId, UUID defendingPartyId) {
        this(attackingPartyId, defendingPartyId,
                // Resolve parties once to capture current online members and names
                getParty(attackingPartyId), getParty(defendingPartyId),
                // Resolve claims’ party IDs (usually same as party IDs if your PartyClaim is keyed by party)
                attackingPartyId, defendingPartyId,
                Collections.emptySet(), Collections.emptySet(), Collections.emptyList());
    }

    /**
     * Flexible constructor if you need to pass allies/mercs/contracts up front.
     */
    public WarData(UUID attackingPartyId,
                   UUID defendingPartyId,
                   IServerPartyAPI attackingParty,
                   IServerPartyAPI defendingParty,
                   UUID attackingClaimPartyId,
                   UUID defendingClaimPartyId,
                   Set<UUID> allyIds,
                   Set<UUID> mercenaryIds,
                   List<MercContract> contracts) {

        // Identity
        this.attackingPartyId = Objects.requireNonNull(attackingPartyId, "attackingPartyId");
        this.defendingPartyId = Objects.requireNonNull(defendingPartyId, "defendingPartyId");

        // Names captured now (safe for messaging even if party names change later)
        this.attackingPartyName = getSafePartyName(attackingParty);
        this.defendingPartyName = getSafePartyName(defendingParty);

        // Claims
        this.attackingClaimPartyId = Objects.requireNonNull(attackingClaimPartyId, "attackingClaimPartyId");
        this.defendingClaimPartyId = Objects.requireNonNull(defendingClaimPartyId, "defendingClaimPartyId");

        // Participants captured from current online members
        this.attackerIds = attackingParty.getOnlineMemberStream()
                .map(ServerPlayer::getUUID)
                .collect(Collectors.toCollection(HashSet::new));

        this.defendersIds = defendingParty.getOnlineMemberStream()
                .map(ServerPlayer::getUUID)
                .collect(Collectors.toCollection(HashSet::new));

        // Attackers to kill starts as a copy of all attackers
        this.attackersLeftToKill = new HashSet<>(this.attackerIds);

        // Allies / Mercs / Contracts
        this.allyIds = new HashSet<>(Objects.requireNonNullElse(allyIds, Collections.emptySet()));
        this.mercenaryIds = new HashSet<>(Objects.requireNonNullElse(mercenaryIds, Collections.emptySet()));
        this.contracts = new ArrayList<>(Objects.requireNonNullElse(contracts, Collections.emptyList()));

        // Duration scaled by defender count
        int defenderCount = this.defendersIds.size();
        this.durationSeconds = defenderCount * 3 * 60;
        this.durationMilli = this.durationSeconds * 1000L;

        // War blocks & wipe decision based on defending claim size
        PartyClaim defendingClaim = getDefendingClaim();
        int claimedChunks = defendingClaim != null ? defendingClaim.getClaimedChunksList().size() : 0;

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
        return getParty(attackingPartyId);
    }

    public IServerPartyAPI getDefendingParty() {
        return getParty(defendingPartyId);
    }

    public PartyClaim getAttackingClaim() {
        return OPAPCComponents.PARTY_CLAIMS.get(OPAPC.scoreboard()).getClaim(attackingPartyId);
    }

    public PartyClaim getDefendingClaim() {
        return OPAPCComponents.PARTY_CLAIMS.get(OPAPC.scoreboard()).getClaim(attackingPartyId);
    }

    private static IServerPartyAPI getParty(UUID id) {
        return OPAPC.parties().getPartyById(id);
    }

    private static String getSafePartyName(IServerPartyAPI party) {
        if (party == null) return "(unknown)";
        try {
            return party.getName(); // or your accessor for party name
        } catch (Exception e) {
            return "(unknown)";
        }
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

        var pnc = OPAPCComponents.PLAYER_NAMES.get(OPAPC.getServer().getScoreboard());

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
