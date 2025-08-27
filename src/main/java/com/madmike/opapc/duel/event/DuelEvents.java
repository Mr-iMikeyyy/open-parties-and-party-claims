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

package com.madmike.opapc.duel.event;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.OPAPCConfig;
import com.madmike.opapc.duel.DuelChallengeManager;
import com.madmike.opapc.duel.DuelManager;
import com.madmike.opapc.duel.components.player.InDuelComponent;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import com.madmike.opapc.util.SafeWarpHelper;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

public class DuelEvents {
    public static void register() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, amount) -> {

            if (entity instanceof ServerPlayer player) {
                if (OPAPCComponents.IN_DUEL.get(player).isInDuel()) {
                    return DuelManager.INSTANCE.handlePlayerDeath(player);
                }
            }

            return true;

        });

        ServerPlayConnectionEvents.JOIN.register((listener, sd, sv) -> {
            ServerPlayer player = listener.getPlayer();
            InDuelComponent comp = OPAPCComponents.IN_DUEL.get(player);
            if (comp.isInDuel()) {
                IServerPartyAPI party = OPAPC.getPartyManager().getPartyByMember(listener.getPlayer().getUUID());
                if ( party != null) {
                    PartyClaim pc = OPAPCComponents.PARTY_CLAIMS.get(sv.getScoreboard()).getClaim(party.getId());
                    if (pc != null && pc.getWarpPos() != null) {
                        SafeWarpHelper.warpPlayerToOverworldPos(player, pc.getWarpPos());
                    }
                } else if (player.getRespawnPosition() != null) {
                    SafeWarpHelper.warpPlayerToOverworldPos(player, player.getRespawnPosition());
                } else {
                    SafeWarpHelper.warpPlayerToWorldSpawn(player);
                }
                comp.setInDuel(false);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((d, ds) -> {
            DuelManager.INSTANCE.handlePlayerQuit(d.getPlayer());
            DuelChallengeManager.INSTANCE.handlePlayerQuit(d.getPlayer());
        });

        ServerLifecycleEvents.SERVER_STARTED.register((s) -> {
            OPAPCComponents.DUEL_BANNED_ITEMS.get(s.getScoreboard()).setAll(OPAPCConfig.duelBannedItemsRaw, OPAPCConfig.duelBannedItemTagsRaw, OPAPCConfig.allowBlockPlacementInDuel);
        });
    }
}
