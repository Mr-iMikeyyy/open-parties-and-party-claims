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

package com.madmike.opapc.duel.events;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;

public class DuelEvents {
    public static void register() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, amount) -> {

            if (entity instanceof ServerPlayer player) {
                if (DuelManager.INSTANCE.playerIsInDuel(player.getUUID())) {
                    DuelManager.INSTANCE.onDuelerDeath(player);
                    return false;
                }
            }

            return true;

        });
    }
}
