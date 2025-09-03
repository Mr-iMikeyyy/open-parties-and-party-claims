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

import com.madmike.opapc.warp.util.SafeWarpHelper;
import com.madmike.opapc.war.EndOfWarType;
import com.madmike.opapc.war.War;
import com.madmike.opapc.war.data.WarData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public class WarEndedState implements IWarState {
    private final EndOfWarType endType;

    public WarEndedState(EndOfWarType endType) {
        this.endType = endType;
    }

    @Override
    public void enter(War war) {
        WarData data = war.getData();
        switch (endType) {
            case ATTACKERS_WIN_WIPE -> {
                data.getAttackingClaim().incrementWarAttacksWon();
            }

            case ATTACKERS_WIN_BLOCKS -> {
                data.getAttackingClaim().incrementWarAttacksWon();
                data.getDefendingClaim().incrementWarDefencesLost();
            }

            case ATTACKERS_LOSE_DEATHS, ATTACKERS_LOSE_TIME -> {
                data.getDefendingClaim().incrementWarDefencesWon();
                data.getAttackingClaim().incrementWarAttacksLost();
            }

            case BUG -> {

            }
        }

        for (ServerPlayer player : data.getAttackingParty().getOnlineMemberStream().toList()) {
            if (data.getDefendingClaim().getClaimedChunksList().contains(new ChunkPos(player.blockPosition()))) {
                BlockPos warpPos = data.getDefendingClaim().getWarpPos();
                if (warpPos != null) {
                    SafeWarpHelper.warpPlayerToOverworldPos(player, warpPos);
                }
                else {
                    SafeWarpHelper.warpPlayerToWorldSpawn(player);
                }
            }
        }

        data.setIsExpired(true);
    }

    @Override
    public void tick(War war) { }

    @Override
    public void onPlayerDeath(ServerPlayer player, War war) {

    }

    @Override
    public void onWarBlockBroken(War war) { }

    @Override
    public void onPlayerQuit(ServerPlayer player, War war) {

    }

    @Override
    public void end(War war, EndOfWarType type) { }
}
