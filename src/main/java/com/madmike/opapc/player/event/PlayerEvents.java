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
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
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
                    OPAPC.getClaimsManager().get(Level.OVERWORLD.location(), player.chunkPosition());

            if (spawnClaim == null) return; // no claim here

            IServerPartyAPI playerParty =
                    OPAPC.getPartyManager().getPartyByMember(player.getUUID());

            // Case 1: Player belongs to a party but isn't the claim owner
            if (playerParty != null &&
                    !playerParty.getOwner().getUUID().equals(spawnClaim.getPlayerId())) {

                OPAPC.LOGGER.warn("Player {} spawned inside unauthorized claim owned by {}. Warping out.",
                        player.getGameProfile().getName(),
                        spawnClaim.getPlayerId());

                PartyClaim claim = OPAPCComponents.PARTY_CLAIMS
                        .get(server.getScoreboard())
                        .getClaim(playerParty.getId());

                if (claim != null && claim.getWarpPos() != null) {
                    SafeWarpHelper.warpPlayer(player, claim.getWarpPos());
                } else {
                    SafeWarpHelper.warpPlayerToWorldSpawn(player);
                }
                return;
            }

            // Case 2: No valid party -> try respawn pos
            ResourceKey<Level> respawnDim = player.getRespawnDimension();
            ServerLevel respawnLevel = server.getLevel(respawnDim);
            BlockPos respawnPos = player.getRespawnPosition();

            if (respawnLevel != null && respawnPos != null) {
                player.teleportTo(
                        respawnLevel,
                        respawnPos.getX() + 0.5,
                        respawnPos.getY(),
                        respawnPos.getZ() + 0.5,
                        player.getYRot(),
                        player.getXRot()
                );
            } else {
                OPAPC.LOGGER.warn("Player {} had no valid respawn set. Sending to world spawn.",
                        player.getGameProfile().getName());
                SafeWarpHelper.warpPlayerToWorldSpawn(player);
            }
        });
    }
}
