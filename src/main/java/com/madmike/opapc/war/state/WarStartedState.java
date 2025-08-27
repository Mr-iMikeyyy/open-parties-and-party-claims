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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.List;

public class WarStartedState implements IWarState {

    @Override
    public void enter(War war) {

        WarData data = war.getData();

        WarBlockSpawner.findAndSpawnWarBlockAsync(data, true);

        //Apply Buffs

        int attackerCount = data.getAttackerIds().size();
        int defenderCount = data.getDefenderIds().size();
        if (defenderCount != attackerCount) {
            int amp = Math.abs(attackerCount - defenderCount);
            List<ServerPlayer> buffTargets = defenderCount < attackerCount ? data.getDefendingParty().getOnlineMemberStream().toList() : data.getAttackingParty().getOnlineMemberStream().toList();

            for (ServerPlayer player : buffTargets) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, data.getDurationSeconds(), amp, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, data.getDurationSeconds(), amp, true, true));
            }
        }

        data.broadcastToWar(Component.literal("The War Has Commenced!"));
    }

    @Override
    public void tick(War war) {
        if (war.getData().isExpired()) {
            end(war, EndOfWarType.ATTACKERS_LOSE_TIME);
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

        data.removeAttacker(player.getUUID());
        if (data.getAttackerIds().isEmpty()) {
            end(war, EndOfWarType.ATTACKERS_LOSE_DEATHS);
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
        OPAPC.claims().unclaim(Level.OVERWORLD.location(), chunkPos.x, chunkPos.z);

        // Double-check that unclaim succeeded
        if (data.getDefendingClaim().getClaimedChunksList().contains(chunkPos)) {
            end(war, EndOfWarType.BUG);
            return;
        }

        // Update stats
        PartyClaim defendingClaim = data.getDefendingClaim();
        PartyClaim attackingClaim = data.getAttackingClaim();

        defendingClaim.setBoughtClaims(defendingClaim.getBoughtClaims() - 1);
        defendingClaim.incrementClaimsLostToWar();

        attackingClaim.setBoughtClaims(attackingClaim.getBoughtClaims() + 1);
        attackingClaim.incrementClaimsGainedFromWar();

        data.decrementWarBlocksLeft();

        // Handle wipe condition
        if (data.getWipe() && data.getDefendingClaim().getClaimedChunksList().isEmpty()) {
            OPAPCComponents.PARTY_CLAIMS
                    .get(OPAPC.scoreboard())
                    .removeClaim(data.getDefendingParty().getId());
            end(war, EndOfWarType.ATTACKERS_WIN_WIPE);
            return;
        }

        // Check war block count
        if (data.getWarBlocksLeft() <= 0) {
            end(war, EndOfWarType.ATTACKERS_WIN_BLOCKS);
            return;
        }

        // Spawn next war block
        WarBlockSpawner.findAndSpawnWarBlockAsync(data, true);
    }

    @Override
    public void end(War war, EndOfWarType type) {
        war.setState(new WarEndingState(type));
        WarEventBus.post(new WarEndedEvent(war, type));
    }
}
