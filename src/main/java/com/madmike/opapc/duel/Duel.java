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

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCConfig;
import com.madmike.opapc.duel.data.DuelData;
import com.madmike.opapc.duel.data.DuelMap;
import com.madmike.opapc.duel.state.duel.DuelAcceptedState;
import com.madmike.opapc.duel.state.DuelChallengedState;
import com.madmike.opapc.duel.state.duel.DuelEndedState;
import com.madmike.opapc.duel.state.duel.DuelStartedState;
import com.madmike.opapc.duel.state.duel.IDuelState;
import com.madmike.opapc.warp.util.SafeWarpHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Duel {
    private IDuelState state;
    private final DuelData data;
    private final RandomSource rng = RandomSource.create();

    public Duel(ServerPlayer challenger, ServerPlayer opponent, DuelMap map, long wager) {
        this.data = new DuelData(challenger, opponent, map, wager, OPAPCConfig.duelMaxLives, OPAPCConfig.duelMaxTime);
        setState(new DuelStartedState());
    }

    /* ---------- routing ---------- */
    public void tick(MinecraftServer server) { state.tick(this); }
    public void handleChallengerDeath(ServerPlayer ch) { state.onChallengerDeath(ch, this); }
    public void handleOpponentDeath(ServerPlayer opp) { state.onOpponentDeath(opp, this); }
    public void end(EndOfDuelType type) { state.end(this, type); }

    /* ---------- membership ---------- */
    public boolean isChallenger(UUID id) { return data.getChallengerId().equals(id); }
    public boolean isOpponent(UUID id)   { return data.getOpponentId().equals(id); }
    public boolean contains(UUID id)     { return isChallenger(id) || isOpponent(id); }
    public boolean isEnded()             { return data.isEnded(); }

    /* ---------- state mgmt ---------- */
    public void setState(IDuelState next) {
        this.state = next;
        this.state.enter(this);
    }

    /* ---------- getters used by states ---------- */
    public DuelData getData() { return data; }

    public @Nullable ServerPlayer getChallengerPlayer() {
        return OPAPC.getServer().getPlayerList().getPlayer(data.getChallengerId());
    }
    public @Nullable ServerPlayer getOpponentPlayer() {
        return OPAPC.getServer().getPlayerList().getPlayer(data.getOpponentId());
    }

    /* ---------- helpers usable by states ---------- */
    public void teleportToSpawns() {
        var ch = getChallengerPlayer();
        var op = getOpponentPlayer();

        var map = data.getMap();

        var cSpawnOpt = map.randomChallengerSpawn(rng);
        var oSpawnOpt = map.randomOpponentSpawn(rng);

        if (cSpawnOpt.isEmpty() || oSpawnOpt.isEmpty()) {
            if (ch != null) ch.sendSystemMessage(Component.literal("§cDuel map has no valid spawns for one side."));
            if (op != null) op.sendSystemMessage(Component.literal("§cDuel map has no valid spawns for one side."));
            finish(EndOfDuelType.DISCONNECT); // or add MAP_INVALID, your call
            return;
        }

        if (ch != null) SafeWarpHelper.warpPlayerToOverworldPos(ch, cSpawnOpt.get());
        if (op != null) SafeWarpHelper.warpPlayerToOverworldPos(op, oSpawnOpt.get());
    }

    public void respawnAtSpawn(ServerPlayer p, boolean challenger) {
        var map = data.getMap();
        var spawnOpt = challenger ? map.randomChallengerSpawn(rng) : map.randomOpponentSpawn(rng);
        if (spawnOpt.isEmpty()) return; // already handled elsewhere / or end duel

        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
        SafeWarpHelper.warpPlayerToOverworldPos(p, spawnOpt.get());
    }

    public void finish(EndOfDuelType type) {
        if (data.isEnded()) return;
        switch (type) {
            case CHALLENGER_NO_LIVES_LEFT -> data.end(data.getOpponentId());
            case OPPONENT_NO_LIVES_LEFT   -> data.end(data.getChallengerId());
            case DUEL_TIMER_RAN_OUT, DISCONNECT -> data.end(null);
        }
        // payout / refund wager here if you want
        setState(new DuelEndedState());
        // Optionally warp players back to saved origin positions if you tracked them
    }
}
