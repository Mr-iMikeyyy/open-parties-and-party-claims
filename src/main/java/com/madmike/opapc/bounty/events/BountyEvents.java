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
import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.bounty.components.scoreboard.BountyBoardComponent;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class BountyEvents {
    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((victim, damageSource) -> {

            // Only care about player victims
            if (!(victim instanceof ServerPlayer dead)) return;

            // Only pay out for player killers
            if (!(damageSource.getEntity() instanceof ServerPlayer killer)) return;

            // No self-farming
            if (killer.getUUID().equals(dead.getUUID())) return;

            // Only mercs can claim (per your rule)
            var mercComp = OPAPCComponents.MERC.get(killer);
            if (!mercComp.isMerc()) return;

            BountyBoardComponent bbc = OPAPCComponents.BOUNTY_BOARD.get(OPAPC.scoreboard());

            //Get bounty
            long bounty = bbc.getBounty(dead.getUUID());
            if (bounty <= 0L) return;

            // Pay out
            var currency = ModComponents.CURRENCY.get(killer);
            currency.modify(bounty);

            // Remove bounty after successful payout
            bbc.removeBounty(dead.getUUID());

            // Feedback
            killer.sendSystemMessage(Component.literal("§aBounty claimed: §e" + bounty + " coins§a for " + dead.getGameProfile().getName() + "."));
            dead.sendSystemMessage(Component.literal("§cYour bounty was claimed by " + killer.getGameProfile().getName() + "."));
            OPAPC.LOGGER.info("[Bounty] {} claimed {} from bounty on {}.", killer.getGameProfile().getName(), bounty, dead.getGameProfile().getName());
        });
    }
}
