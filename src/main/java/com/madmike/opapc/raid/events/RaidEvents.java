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

package com.madmike.opapc.raid.events;

import com.madmike.opapc.raid.RaidManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public class RaidEvents {
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            RaidManager.INSTANCE.tick();
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            if (RaidManager.getActiveRaids(serverPlayer)) {
                serverPlayer.sendSystemMessage(Component.literal("You cannot place blocks while raiding a claim."));
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });

        UseItemCallback.EVENT.register(((player, world, hand) -> {

        }));

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return true; // allow if not server player

            if (RaidManager.getActiveRaids(serverPlayer)) {
                serverPlayer.sendSystemMessage(Component.literal("You cannot break blocks while raiding a claim."));
                return false; // cancel the break
            }

            return true; // allow the break
        });
    }
}
