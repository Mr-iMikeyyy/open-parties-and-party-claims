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

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.OPAPCConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class DuelClientEvents {

    public static void register() {
        // Right-click in air (items like pearls, food, potions, bows)
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide
                    && OPAPCComponents.IN_DUEL.get(player).isInDuel()
                    && OPAPCComponents.DUEL_BANNED_ITEMS.get(world.getScoreboard()).isBlocked(stack)) {

                player.displayClientMessage(Component.literal("Can't use this during a duel."), true);
                return InteractionResultHolder.fail(stack);   // ✅ cancel client-side; no packet
            }
            return InteractionResultHolder.pass(stack);
        });

        // Right-click on block (buckets, flint & steel on blocks, doors, etc.)
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            ItemStack stack = player.getItemInHand(hand);

            // 🚫 NEW: block placement while in a duel
            if (OPAPCComponents.IN_DUEL.get(player).isInDuel()
                    && stack.getItem() instanceof BlockItem
                    && OPAPCComponents.DUEL_BANNED_ITEMS.get(world.getScoreboard()).isBlockPlacementDisabled()) {
                player.displayClientMessage(Component.literal("Can't place blocks during a duel."), true);
                return InteractionResult.FAIL; // cancel client-side; prevents place packet
            }

            if (world.isClientSide && OPAPCComponents.IN_DUEL.get(player).isInDuel() && OPAPCComponents.DUEL_BANNED_ITEMS.get(world.getScoreboard()).isBlocked(stack)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        // Right-click on entity (name tags, buckets on mobs, leads)
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide && OPAPCComponents.IN_DUEL.get(player).isInDuel() && OPAPCComponents.DUEL_BANNED_ITEMS.get(world.getScoreboard()).isBlocked(stack)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        // Optional: block breaking/attacking during duel (if you want)
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClientSide && OPAPCComponents.IN_DUEL.get(player).isInDuel()) return InteractionResult.FAIL;
            return InteractionResult.PASS;
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            // If you *don't* want to stop melee, delete this handler.
            if (world.isClientSide && OPAPCComponents.IN_DUEL.get(player).isInDuel() && OPAPCComponents.DUEL_BANNED_ITEMS.get(world.getScoreboard()).isBlocked(player.getItemInHand(hand))) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        // Safety net: stop continuous use (bows, shields, food if it somehow started)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || !client.level.isClientSide) return;
            if (OPAPCComponents.IN_DUEL.get(client.player).isInDuel() && client.player.isUsingItem()) {
                ItemStack using = client.player.getUseItem();
                if (OPAPCComponents.DUEL_BANNED_ITEMS.get(client.level.getScoreboard()).isBlocked(using)) {
                    client.player.stopUsingItem(); // halts charging/eating if it slipped through
                }
            }
        });
    }
}
