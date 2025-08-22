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
import com.madmike.opapc.duel.components.scoreboard.DuelMapsComponent;
import com.madmike.opapc.duel.data.DuelMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Supports multiple concurrent challenges and multiple concurrent duels.
 *
 * - Players may have multiple pending challenges (incoming and outgoing).
 * - A player may participate in at most one ACTIVE duel at a time.
 * - Accepting one challenge auto-cancels all other challenges involving either player.
 * - Each challenge has a UUID id to accept/cancel explicitly.
 */
public class DuelManager {

    public static final DuelManager INSTANCE = new DuelManager();

    /* ================= Active duels ================= */
    private final List<Duel> activeDuels = new ArrayList<>();



    private DuelManager() {}

    /* ================= Queries ================= */
    public boolean isDuelOngoing() { return !activeDuels.isEmpty(); }

    public boolean isPlayerInDuel(UUID playerId) {
        for (Duel d : activeDuels) if (d.contains(playerId)) return true;
        return false;
    }





    /* ================= Duel lifecycle ================= */

    public Duel startDuel(MinecraftServer server,
                          ServerPlayer challenger,
                          ServerPlayer opponent,
                          DuelMap map,
                          BlockPos challengerSpawn,
                          BlockPos opponentSpawn,
                          long wager) {

        // Adjust ctor/signature for your Duel class:
        Duel duel = new Duel(challenger, opponent, map, challengerSpawn, opponentSpawn, wager);
        activeDuels.add(duel);
        return duel;
    }

    public void endDuel(Duel duel, EndOfDuelType reason) {
        try { duel.end(reason); }
        finally { activeDuels.remove(duel); }
    }

    public void endAll(EndOfDuelType reason) {
        List<Duel> copy = new ArrayList<>(activeDuels);
        for (Duel d : copy) endDuel(d, reason);
        activeDuels.clear();
    }

    private void gcEnded() { activeDuels.removeIf(Duel::isEnded); }

    /* ================= Tick + Events ================= */

    public void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        List<UUID> toRemove = new ArrayList<>();
        for (var e : challengesById.entrySet()) {
            if (now >= e.getValue().expiresAtMs) toRemove.add(e.getKey());
        }
        for (UUID id : toRemove) cancelChallenge(id);

        for (Duel d : activeDuels) d.tick(server);
        gcEnded();
    }

    public boolean handlePlayerDeath(ServerPlayer player) {
        for (Duel duel : activeDuels) {
            if (duel.isChallenger(player.getUUID())) { duel.handleChallengerDeath(player); return false; }
            if (duel.isOpponent(player.getUUID()))   { duel.handleOpponentDeath(player);   return false; }
        }
        return true;
    }

    public void handlePlayerQuit(ServerPlayer player) {
        for (Duel duel : new ArrayList<>(activeDuels)) {
            if (duel.contains(player.getUUID())) duel.onPlayerQuit(player);
        }
        cancelAllChallengesInvolving(player.getUUID());
    }

    /* ================= DTOs ================= */

    public static final class AcceptResult {
        public final boolean started;
        public final String message;
        @Nullable public final Duel duel;

        private AcceptResult(boolean started, String message, @Nullable Duel duel) {
            this.started = started; this.message = message; this.duel = duel;
        }

        public static AcceptResult started(Duel duel) { return new AcceptResult(true, "Duel started!", duel); }
        public static AcceptResult noSuchChallenge() { return new AcceptResult(false, "No matching challenge found.", null); }
        public static AcceptResult notYourChallenge() { return new AcceptResult(false, "You are not the challenged player.", null); }
        public static AcceptResult playersBusy() { return new AcceptResult(false, "Either player is already in a duel.", null); }
        public static AcceptResult playersMissing() { return new AcceptResult(false, "One or both players are no longer online.", null); }
        public static AcceptResult noMapsDefined() { return new AcceptResult(false, "No valid duel maps are defined.", null); }
        public static AcceptResult mapInvalid() { return new AcceptResult(false, "Selected map has invalid spawns.", null); }
    }
}
