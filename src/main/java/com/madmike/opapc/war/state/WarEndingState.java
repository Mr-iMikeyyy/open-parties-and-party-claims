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
import com.madmike.opapc.warp.util.SafeWarpHelper;
import com.madmike.opapc.war.EndOfWarType;
import com.madmike.opapc.war.War;
import com.madmike.opapc.war.data.WarData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class WarEndingState implements IWarState{
    EndOfWarType endType;
    long endedAt;
    long endingDurationSeconds;

    public WarEndingState(EndOfWarType type) {
        this.endType = type;
        this.endedAt = System.currentTimeMillis();
        this.endingDurationSeconds = OPAPCConfig.warEndingDurationSeconds * 1000L;
    }

    @Override
    public void enter(War war) {

        if (endType == EndOfWarType.ATTACKERS_WIN_BLOCKS || endType == EndOfWarType.ATTACKERS_LOSE_TIME) {
            war.getData().broadcastToAttackers(Component.literal("The War is Ending! You have " + OPAPCConfig.warEndingDurationSeconds + " seconds to get the ship out of the claim!"));
            war.getData().broadcastToDefenders(Component.literal("The War is Ending! If the enemy ship is in your claim now is the time to steal! They will be forced out in " + OPAPCConfig.warEndingDurationSeconds + " seconds!"));
        }
        else {
            war.setState(new WarEndedState(endType));
        }

    }

    @Override
    public void tick(War war) {
        long elapsed = System.currentTimeMillis() - endedAt;

        if (elapsed >= endingDurationSeconds) {
            war.setState(new WarEndedState(endType));
        }
    }

    @Override
    public void onAttackerDeath(ServerPlayer player, War war) {
        player.setHealth(player.getMaxHealth());
        WarData data = war.getData();
        if (data.getAttackingClaim().getWarpPos() != null) {
            SafeWarpHelper.warpPlayerToOverworldPos(player, data.getAttackingClaim().getWarpPos());
        }
        else {
            SafeWarpHelper.warpPlayerToWorldSpawn(player);
        }
    }

    @Override
    public void onDefenderDeath(ServerPlayer player, War war) {
        player.setHealth(player.getMaxHealth());

        BlockPos warpPos = war.getData().getDefendingClaim().getWarpPos();

        if (warpPos != null) {
            SafeWarpHelper.warpPlayerToOverworldPos(player, warpPos);
        } else {
            SafeWarpHelper.warpPlayerToWorldSpawn(player);
        }
    }

    @Override
    public void onWarBlockBroken(BlockPos pos, War war) {

    }

    @Override
    public void end(War war, EndOfWarType type) {

    }
}
