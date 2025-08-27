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

package com.madmike.opapc.player.event;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import com.madmike.opapc.util.SafeWarpHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;


public class PlayerEvents {

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();

            // Always sync player name
            OPAPCComponents.PLAYER_NAMES
                    .get(server.getScoreboard())
                    .addOrSet(player);

            // Only care about overworld spawns
            if (!player.level().dimension().equals(Level.OVERWORLD)) return;

            IPlayerChunkClaimAPI spawnClaim =
                    OPAPC.claims().get(Level.OVERWORLD.location(), player.chunkPosition());

            if (spawnClaim == null) return; // no claim here

            IServerPartyAPI playerParty =
                    OPAPC.parties().getPartyByMember(player.getUUID());

            if (playerParty != null) {
                 if (!spawnClaim.getPlayerId().equals(playerParty.getOwner().getUUID())) {
                     PartyClaim pc = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.scoreboard()).getClaim(playerParty.getId());
                     if (pc != null) {
                         BlockPos warpPos = pc.getWarpPos();
                         if (warpPos != null) {
                             SafeWarpHelper.warpPlayerToOverworldPos(player, warpPos);
                         }
                         else {
                             SafeWarpHelper.warpPlayerToWorldSpawn(player);
                         }
                     }
                     else {
                         SafeWarpHelper.warpPlayerToWorldSpawn(player);
                     }
                 }
            }
            else {
                if (player.getRespawnPosition() != null) {
                    SafeWarpHelper.warpPlayerToOverworldPos(player, player.getRespawnPosition());
                }
                else {
                    SafeWarpHelper.warpPlayerToWorldSpawn(player);
                }
            }
            
        });
    }
}
