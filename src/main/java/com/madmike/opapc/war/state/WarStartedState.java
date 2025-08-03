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

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import com.madmike.opapc.util.SafeWarpHelper;
import com.madmike.opapc.war.EndOfWarType;
import com.madmike.opapc.war.War;
import com.madmike.opapc.war.data.WarData;
import com.madmike.opapc.war.event.bus.WarEventBus;
import com.madmike.opapc.war.event.events.WarEndedEvent;
import com.madmike.opapc.war.features.block.WarBlockSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class WarStartedState implements IWarState {

    @Override
    public void tick(War war) {
        if (war.getData().isExpired()) {
            war.end(EndOfWarType.ATTACKERS_LOSE_TIME);
        }
    }

    @Override
    public void onAttackerDeath(ServerPlayer player, War war) {
        player.setHealth(player.getMaxHealth());
        WarData data = war.getData();
        data.decrementAttackerLivesRemaining();
        if (data.getAttackerLivesRemaining() <= 0) {
            end(war, EndOfWarType.ATTACKERS_LOSE_DEATHS);
        }
        else {
            BlockPos safeWarpPos = SafeWarpHelper.findSafeSpawnOutsideClaim(war.getData().getDefendingClaim());
            if (safeWarpPos != null) {
                SafeWarpHelper.warpPlayer(player, safeWarpPos);
            }
            else {
                end(war, EndOfWarType.BUG);
            }
        }
    }

    @Override
    public void onDefenderDeath(ServerPlayer player, War war) {
        player.setHealth(player.getMaxHealth());

        var claim = war.getData().getDefendingClaim();
        BlockPos targetPos = SafeWarpHelper.findSafeSpawnInsideClaim(claim);

        if (targetPos == null) {
            targetPos = claim.getWarpPos();
        }

        if (targetPos != null) {
            SafeWarpHelper.warpPlayer(player, targetPos);
        } else {
            end(war, EndOfWarType.BUG);
        }
    }

    public void onWarBlockBroken(BlockPos pos, War war) {
        WarData data = war.getData();
        ChunkPos chunkPos = new ChunkPos(pos);

        // Ensure the defending claim actually has this chunk
        if (!data.getDefendingClaim().getClaimedChunksList().contains(chunkPos)) {
            end(war, EndOfWarType.BUG);
            return;
        }

        // Unclaim the chunk
        OPAPC.getClaimsManager().unclaim(Level.OVERWORLD.location(), chunkPos.x, chunkPos.z);

        // Double-check that unclaim succeeded
        if (data.getDefendingClaim().getClaimedChunksList().contains(chunkPos)) {
            end(war, EndOfWarType.BUG);
            return;
        }

        // Handle wipe condition
        if (data.getWipe() && data.getDefendingClaim().getClaimedChunksList().isEmpty()) {
            OPAPCComponents.PARTY_CLAIMS
                    .get(OPAPC.getServer().getScoreboard())
                    .removeClaim(data.getDefendingParty().getId());
            end(war, EndOfWarType.ATTACKERS_WIN_WIPE);
            return;
        }

        // Update stats
        PartyClaim defendingClaim = data.getDefendingClaim();
        PartyClaim attackingClaim = data.getAttackingClaim();

        defendingClaim.setBoughtClaims(defendingClaim.getBoughtClaims() - 1);
        defendingClaim.incrementClaimsLostToWar();
        attackingClaim.incrementClaimsGainedFromWar();

        data.decrementWarBlocksLeft();

        // Check war block count
        if (data.getWarBlocksLeft() <= 0) {
            end(war, EndOfWarType.ATTACKERS_WIN_BLOCKS);
            return;
        }

        // Spawn next war block
        BlockPos nextSpawn = WarBlockSpawner.findSafeSpawn(data);
        if (nextSpawn == null) {
            end(war, EndOfWarType.BUG);
            return;
        }

        WarBlockSpawner.spawnWarBlock(nextSpawn);
    }

    @Override
    public void end(War war, EndOfWarType type) {
        war.setState(new WarEndedState(type));
        WarEventBus.post(new WarEndedEvent(war, type));
    }
}
