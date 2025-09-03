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

package com.madmike.opapc.war.state;

import com.madmike.opapc.OPAPCConfig;
import com.madmike.opapc.war.EndOfWarType;
import com.madmike.opapc.war.War;
import com.madmike.opapc.war.data.WarData;
import com.madmike.opapc.war.event.bus.WarEventBus;
import com.madmike.opapc.war.event.events.WarEndedEvent;
import com.madmike.opapc.war.event.events.WarStartedEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class WarDeclaredState implements IWarState {
    private long declareTime;
    private long warPreparationPeriodMilli;


    @Override
    public void enter(War war) {
        this.declareTime = System.currentTimeMillis();

        int secondsToPrepare = OPAPCConfig.warPreparationSeconds;
        this.warPreparationPeriodMilli = secondsToPrepare * 1000L;

        WarData data = war.getData();
        data.broadcastToDefenders(Component.literal("Your claim is under attack by " + data.getAttackingClaim().getPartyName() + "! You have " + secondsToPrepare + " seconds to prepare! Use [/war join] to teleport to your claim. If you are outnumbered, use [/war merc hire <player>] to try and hire a mercenary or convince an ally to use [/war join <your party>]. You will be auto teleported to your party claim at the start of the war."));
    }

    @Override
    public void tick(War war) {
        long elapsed = System.currentTimeMillis() - declareTime;
        long remaining = warPreparationPeriodMilli - elapsed;

        // Broadcast every 5 seconds
        if (remaining > 0 && remaining % 5000 < 50) {
            war.getData().broadcastToWar(Component.literal("§eWar begins in " + (remaining / 1000) + " seconds!"));
        }

        if (elapsed >= warPreparationPeriodMilli) {
            war.setState(new WarStartedState());
            war.getData().setStartTime(System.currentTimeMillis());
            WarEventBus.post(new WarStartedEvent(war));
        }
    }

    @Override
    public void onPlayerDeath(ServerPlayer player, War war) {
    }

    @Override
    public void onWarBlockBroken(War war) { }

    @Override
    public void onPlayerQuit(ServerPlayer player, War war) {
        WarData data = war.getData();
        UUID playerId = player.getUUID();
        if (data.getMercenaryIds().contains(playerId)) {
            data.removeMercenary(playerId);
            return;
        }
        if (data.getAttackerIds().contains(playerId)) {

        }
    }

    @Override
    public void end(War war, EndOfWarType type) {
        war.setState(new WarEndedState(type));
        WarEventBus.post(new WarEndedEvent(war, type));
    }
}
