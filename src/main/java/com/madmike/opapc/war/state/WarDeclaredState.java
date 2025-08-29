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
import com.madmike.opapc.war.event.bus.WarEventBus;
import com.madmike.opapc.war.event.events.WarEndedEvent;
import com.madmike.opapc.war.event.events.WarStartedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class WarDeclaredState implements IWarState {
    private long declareTime;
    private long warPreparationPeriodSeconds;


    @Override
    public void enter(War war) {
        this.declareTime = System.currentTimeMillis();
        this.warPreparationPeriodSeconds = OPAPCConfig.warPreparationSeconds * 1000L;
    }

    @Override
    public void tick(War war) {
        long elapsed = System.currentTimeMillis() - declareTime;
        long remaining = warPreparationPeriodSeconds - elapsed;

        // Broadcast every 5 seconds
        if (remaining > 0 && remaining % 5000 < 50) {
            war.getData().broadcastToWar(Component.literal("§eWar begins in " + (remaining / 1000) + " seconds!"));
        }

        if (elapsed >= warPreparationPeriodSeconds) {
            war.setState(new WarStartedState());
            war.getData().setStartTime(System.currentTimeMillis());
            WarEventBus.post(new WarStartedEvent(war));
        }
    }

    @Override
    public void onAttackerDeath(ServerPlayer player, War war) {
        player.setHealth(player.getMaxHealth());
    }

    @Override
    public void onDefenderDeath(ServerPlayer player, War war) {
        player.setHealth(player.getMaxHealth());
    }

    @Override
    public void onWarBlockBroken(BlockPos pos, War war) { }

    @Override
    public void onDefenderQuit(ServerPlayer player, War war) {

    }

    @Override
    public void onAttackerQuit(ServerPlayer player, War war) {

    }

    @Override
    public void end(War war, EndOfWarType type) {
        war.setState(new WarEndedState(type));
        WarEventBus.post(new WarEndedEvent(war, type));
    }
}
