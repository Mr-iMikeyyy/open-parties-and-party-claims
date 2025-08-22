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

package com.madmike.opapc.duel.data;


import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Holds all relevant runtime + persistent info about a duel.
 */
public class DuelData {

    /* --- Core players --- */
    private final UUID challengerId;
    private final UUID opponentId;

    private final String challengerName;
    private final String opponentName;

    private final List<UUID> challengerSpectators = new ArrayList<>();
    private final List<UUID> opponentSpectators = new ArrayList<>();

    /* --- Map / Spawns --- */
    private final DuelMap map;

    /* --- Wager / stakes --- */
    private final long wager; // in your gold units (Numismatic Overhaul)

    /* --- State / progress --- */
    private int challengerKills;
    private int opponentKills;

    private final int requiredKills; // default 3, "first to X kills"

    private final long startTimeMs;
    private final long maxDurationMs; // if you want duels to timeout, else Long.MAX_VALUE

    private boolean ended;
    private UUID winnerId;

    /* --- Construction --- */
    public DuelData(ServerPlayer challenger, ServerPlayer opponent, DuelMap map, long wager, int requiredKills, long maxDurationMs) {
        this.challengerId = challenger.getUUID();
        this.opponentId = opponent.getUUID();
        this.challengerName = challenger.getGameProfile().getName();
        this.opponentName = opponent.getGameProfile().getName();

        this.map = map;

        this.wager = wager;
        this.requiredKills = requiredKills;

        this.startTimeMs = System.currentTimeMillis();
        this.maxDurationMs = maxDurationMs;

        this.ended = false;
    }

    /* --- Getters --- */
    public UUID getChallengerId() { return challengerId; }
    public UUID getOpponentId() { return opponentId; }
    public String getChallengerName() { return challengerName; }
    public String getOpponentName() { return opponentName; }
    public DuelMap getMap() { return map; }
    public long getWager() { return wager; }
    public int getChallengerKills() { return challengerKills; }
    public int getOpponentKills() { return opponentKills; }
    public boolean isEnded() { return ended; }
    public UUID getWinnerId() { return winnerId; }

    /* --- Logic helpers --- */

    public void addKillForChallenger() {
        challengerKills++;
        checkForEnd();
    }

    public void addKillForOpponent() {
        opponentKills++;
        checkForEnd();
    }

    private void checkForEnd() {
        if (challengerKills >= requiredKills) {
            end(challengerId);
        } else if (opponentKills >= requiredKills) {
            end(opponentId);
        } else if (System.currentTimeMillis() - startTimeMs >= maxDurationMs) {
            // decide tie-breaker, here: higher kills wins, else null
            if (challengerKills > opponentKills) end(challengerId);
            else if (opponentKills > challengerKills) end(opponentId);
            else end(null); // tie, maybe refund wager
        }
    }

    public void end(UUID winnerId) {
        this.ended = true;
        this.winnerId = winnerId;
    }

    public long getElapsedMs() {
        return System.currentTimeMillis() - startTimeMs;
    }

    /* --- Spectators --- */
    public void addSpectator(UUID playerId, boolean forChallenger) {
        if (forChallenger) challengerSpectators.add(playerId);
        else opponentSpectators.add(playerId);
    }

    public List<UUID> getChallengerSpectators() { return challengerSpectators; }
    public List<UUID> getOpponentSpectators() { return opponentSpectators; }

}
