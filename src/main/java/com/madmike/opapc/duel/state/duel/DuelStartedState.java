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

package com.madmike.opapc.duel.state.duel;

import com.madmike.opapc.OPAPCConfig;
import com.madmike.opapc.duel.Duel;
import com.madmike.opapc.duel.EndOfDuelType;
import com.madmike.opapc.duel.data.DuelData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class DuelStartedState implements IDuelState {

    @Override
    public void enter(Duel duel) {
        // Announce start, clear effects, give kits, etc.
        var ch = duel.getChallengerPlayer();
        var op = duel.getOpponentPlayer();
        if (ch != null) ch.sendSystemMessage(Component.literal("§aDuel start! First to 3 kills wins."));
        if (op != null) op.sendSystemMessage(Component.literal("§aDuel start! First to 3 kills wins."));
    }

    @Override public void tick(Duel duel) {
        // Optional: time limit handling if you use DuelData.maxDurationMs
        if (OPAPCConfig.duelMaxTime > )
        DuelData data = duel.getData();
        if ()
        if (System.currentTimeMillis() - data.getElapsedMs() >= 0 && data.getElapsedMs() >= data.getElapsedMs()) {
            // No-op; you can add timed win/ tie-break here if using finite maxDurationMs
        }
    }

    @Override
    public void onChallengerDeath(ServerPlayer ch, Duel duel) {
        var data = duel.getData();
        data.addKillForOpponent(); // opponent scores
        // Check for win
        if (data.getOpponentKills() >=  data.getChallengerKills() + (data.getChallengerKills() >= data.getOpponentKills() ? 0 : 0)
                && data.getOpponentKills() >= 3) {
            duel.finish(EndOfDuelType.CHALLENGER_NO_LIVES_LEFT);
            return;
        }
        // Otherwise, respawn challenger at spawn
        duel.respawnAtSpawn(ch, true);
        announceScore(duel);
    }

    @Override
    public void onOpponentDeath(ServerPlayer opp, Duel duel) {
        var data = duel.getData();
        data.addKillForChallenger();
        if (data.getChallengerKills() >= 3) {
            duel.finish(EndOfDuelType.OPPONENT_NO_LIVES_LEFT);
            return;
        }
        duel.respawnAtSpawn(opp, false);
        announceScore(duel);
    }

    private void announceScore(Duel duel) {
        var ch = duel.getChallengerPlayer();
        var op = duel.getOpponentPlayer();
        var d = duel.getData();
        String msg = String.format("§eScore: §b%s §f%d - %d §d%s",
                d.getChallengerName(), d.getChallengerKills(), d.getOpponentKills(), d.getOpponentName());
        if (ch != null) ch.sendSystemMessage(Component.literal(msg));
        if (op != null) op.sendSystemMessage(Component.literal(msg));
    }

    @Override
    public void end(Duel duel, EndOfDuelType type) {
        duel.finish(type);
    }
}

