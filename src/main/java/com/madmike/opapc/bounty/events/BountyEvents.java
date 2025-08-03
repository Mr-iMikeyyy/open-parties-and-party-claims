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

package com.madmike.opapc.bounty.events;

import com.glisco.numismaticoverhaul.ModComponents;
import com.madmike.opapc.OPAPCComponents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;

public class BountyEvents {
    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(((entity, damageSource) -> {
            if (entity instanceof ServerPlayer player) {
                if (damageSource.getEntity() instanceof ServerPlayer killer) {
                    if (OPAPCComponents.BOUNTY.get(player).getBounty() > 0) {
                        ModComponents.CURRENCY.get(killer).modify(OPAPCComponents.BOUNTY.get(player).getBounty());
                        OPAPCComponents.BOUNTY.get(player).setBounty(0);
                    }
                    else {
                        OPAPCComponents.BOUNTY.get(killer).setBounty(OPAPCComponents.BOUNTY.get(killer).getBounty() + );
                    }
                }
            }

        }));
    }
}
