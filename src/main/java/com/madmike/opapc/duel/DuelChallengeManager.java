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

package com.madmike.opapc.duel;

import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.OPAPCConfig;
import com.madmike.opapc.duel.components.scoreboard.DuelMapsComponent;
import com.madmike.opapc.duel.data.DuelMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DuelChallengeManager {

    public static final DuelChallengeManager INSTANCE = new DuelChallengeManager();

    private DuelChallengeManager() {}

    /* ================= Pending challenges ================= */
    private final Map<UUID, DuelChallenge> challenges = new HashMap<>();

    private static final long CHALLENGE_TIMEOUT_MS = OPAPCConfig.duelChallengeMaxTime * 1000L;

    /* ================= Queries ================= */

    public DuelChallenge getChallengeFor(UUID playerId) {
        return challenges.get(playerId);
    }

    /* ================= Create / Cancel / Deny ================= */

    /** Create a challenge; many may exist at once. */
    public DuelManager.ChallengeResult requestChallenge(ServerPlayer challenger, ServerPlayer opponent, @Nullable String desiredMapName, long wager) {
        if (isPlayerInDuel(challenger.getUUID()) || isPlayerInDuel(opponent.getUUID())) {
            return DuelManager.ChallengeResult.playersBusy();
        }
        long now = System.currentTimeMillis();
        UUID id = UUID.randomUUID();
        DuelChallenge pc = new DuelChallenge(
                id,
                challenger.getUUID(), opponent.getUUID(),
                challenger.getGameProfile().getName(), opponent.getGameProfile().getName(),
                desiredMapName, wager,
                now, now + CHALLENGE_TIMEOUT_MS
        );
        challenges.put(id, pc);
        challengesByPlayer.computeIfAbsent(pc.challengerId, k -> new HashSet<>()).add(id);
        challengesByPlayer.computeIfAbsent(pc.opponentId, k -> new HashSet<>()).add(id);
        return DuelManager.ChallengeResult.ok(id);
    }

    public boolean cancelChallenge(UUID id) {
        DuelChallenge pc = challenges.remove(id);
        if (pc == null) return false;
        removeChallengeIdForPlayer(pc.challengerId, id);
        removeChallengeIdForPlayer(pc.opponentId, id);
        return true;
    }

    public int cancelOutgoingChallenges(UUID challengerId) {
        var ids = new HashSet<>(challengesByPlayer.getOrDefault(challengerId, Collections.emptySet()));
        int removed = 0;
        for (UUID id : ids) {
            DuelChallenge pc = challenges.get(id);
            if (pc != null && pc.challengerId.equals(challengerId)) {
                if (cancelChallenge(id)) removed++;
            }
        }
        return removed;
    }

    public int denyIncomingChallenges(UUID opponentId) {
        var ids = new HashSet<>(challengesByPlayer.getOrDefault(opponentId, Collections.emptySet()));
        int removed = 0;
        for (UUID id : ids) {
            DuelChallenge pc = challenges.get(id);
            if (pc != null && pc.opponentId.equals(opponentId)) {
                if (cancelChallenge(id)) removed++;
            }
        }
        return removed;
    }

    public void cancelAllChallengesInvolving(UUID playerId) {
        Set<UUID> ids = new HashSet<>(challengesByPlayer.getOrDefault(playerId, Collections.emptySet()));
        for (UUID id : ids) cancelChallenge(id);
    }

    private void removeChallengeIdForPlayer(UUID playerId, UUID id) {
        Set<UUID> set = challengesByPlayer.get(playerId);
        if (set == null) return;
        set.remove(id);
        if (set.isEmpty()) challengesByPlayer.remove(playerId);
    }

    /* ================= Accept ================= */

    public DuelManager.AcceptResult acceptLatestFor(ServerPlayer accepter, MinecraftServer server, RandomSource random) {
        Optional<DuelChallenge> opt = getLatestIncomingFor(accepter.getUUID());
        if (opt.isEmpty()) return DuelManager.AcceptResult.noSuchChallenge();
        return acceptById(opt.get().id, accepter, server, random);
    }

    public DuelManager.AcceptResult acceptById(UUID challengeId, ServerPlayer accepter, MinecraftServer server, RandomSource random) {
        DuelChallenge pc = challenges.get(challengeId);
        if (pc == null) return DuelManager.AcceptResult.noSuchChallenge();
        if (!pc.opponentId.equals(accepter.getUUID())) return DuelManager.AcceptResult.notYourChallenge();
        if (isPlayerInDuel(pc.challengerId) || isPlayerInDuel(pc.opponentId)) {
            cancelChallenge(pc.id);
            return DuelManager.AcceptResult.playersBusy();
        }

        ServerPlayer challenger = server.getPlayerList().getPlayer(pc.challengerId);
        ServerPlayer opponent = server.getPlayerList().getPlayer(pc.opponentId);
        if (challenger == null || opponent == null) {
            cancelChallenge(pc.id);
            return DuelManager.AcceptResult.playersMissing();
        }

        DuelMapsComponent comp = OPAPCComponents.DUEL_MAPS.get(server.getScoreboard());
        if (comp == null || comp.isEmpty()) {
            cancelChallenge(pc.id);
            return DuelManager.AcceptResult.noMapsDefined();
        }

        DuelMap map;
        if (pc.desiredMapName != null && !"Random".equalsIgnoreCase(pc.desiredMapName) && comp.exists(pc.desiredMapName)) {
            map = comp.get(pc.desiredMapName).get();
        } else {
            var opt = comp.getRandom(random);
            if (opt.isEmpty()) {
                cancelChallenge(pc.id);
                return DuelManager.AcceptResult.noMapsDefined();
            }
            map = opt.get();
        }

        var spawnPairOpt = comp.pickSpawnPair(map.getName(), random, null);
        if (spawnPairOpt.isEmpty()) {
            cancelChallenge(pc.id);
            return DuelManager.AcceptResult.mapInvalid();
        }
        BlockPos p1Spawn = spawnPairOpt.get().first;
        BlockPos p2Spawn = spawnPairOpt.get().second;

        Duel duel = startDuel(server, challenger, opponent, map, p1Spawn, p2Spawn, pc.wager);

        // Clean up challenges for both players
        cancelAllChallengesInvolving(pc.challengerId);
        cancelAllChallengesInvolving(pc.opponentId);

        return DuelManager.AcceptResult.started(duel);
    }

    /* ================= Tick + Events ================= */

    public void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        List<UUID> toRemove = new ArrayList<>();
        for (var e : challenges.entrySet()) {
            if (now >= e.getValue().expiresAtMs) toRemove.add(e.getKey());
        }
        for (UUID id : toRemove) cancelChallenge(id);

        for (Duel d : activeDuels) d.tick(server);
        gcEnded();
    }

    public void handlePlayerQuit(ServerPlayer player) {
        cancelAllChallengesInvolving(player.getUUID());
    }
}
