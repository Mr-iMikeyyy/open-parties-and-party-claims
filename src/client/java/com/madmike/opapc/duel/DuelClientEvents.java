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

package com.madmike.opapc.duel;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

public class DuelClientEvents {

    public static void register() {
        // Right-click in air (items like pearls, food, potions, bows)
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide && DuelStatus.inDuel(player) && DuelRules.isBlockedInDuel(stack)) {
                player.displayClientMessage(Component.literal("Can't use this during a duel."), true);
                return TypedActionResult.fail(stack); // cancels client-side and stops packet
            }
            return TypedActionResult.pass(stack);
        });

        // Right-click on block (buckets, flint & steel on blocks, doors, etc.)
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide && DuelStatus.inDuel(player) && DuelRules.isBlockedInDuel(stack)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        // Right-click on entity (name tags, buckets on mobs, leads)
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide && DuelStatus.inDuel(player) && DuelRules.isBlockedInDuel(stack)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        // Optional: block breaking/attacking during duel (if you want)
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClientSide && DuelStatus.inDuel(player)) return InteractionResult.FAIL;
            return InteractionResult.PASS;
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            // If you *don't* want to stop melee, delete this handler.
            if (world.isClientSide && DuelStatus.inDuel(player) && DuelRules.isBlockedInDuel(player.getItemInHand(hand))) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        // Safety net: stop continuous use (bows, shields, food if it somehow started)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Minecraft mc = client;
            if (mc.player == null || !mc.level.isClientSide) return;
            if (DuelStatus.inDuel(mc.player) && mc.player.isUsingItem()) {
                ItemStack using = mc.player.getUseItem();
                if (DuelRules.isBlockedInDuel(using)) {
                    mc.player.stopUsingItem(); // halts charging/eating if it slipped through
                }
            }
        });
    }
}
