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

package com.madmike.opapc.duel.data;

import com.madmike.opapc.OPAPCConfig;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DuelBannedItems {
    // Quick lookup set built from config strings
    private static final Set<Item> BANNED_CONFIG_SET = new HashSet<>();

    /** Build the config set from OPAPCConfig.duelBannedItemsRaw. Call after OPAPCConfig.load(). */
    public static void reloadFromConfig() {
        BANNED_CONFIG_SET.clear();
        // We need a registry access to resolve Items. On both sides, we can use the built-in root.
        // In dedicated server/common code paths, RegistryAccess.BUILTIN.get() works for vanilla items.
        // If you need modded items at early bootstrap, call this after registries are ready (e.g. after mods init).
        RegistryAccess access = RegistryAccess.BUILTIN.get(); // ok for vanilla + registered content
        var itemRegistry = access.registryOrThrow(Registries.ITEM);

        List<String> ids = OPAPCConfig.duelBannedItemsRaw;
        for (String raw : ids) {
            try {
                ResourceLocation id = new ResourceLocation(raw.trim());
                Item item = itemRegistry.getOptional(id).orElse(null);
                if (item != null) {
                    BANNED_CONFIG_SET.add(item);
                } else {
                    // If not found now (e.g., datapack adds items), it just won't enter the set.
                    // You could log here if you want:
                    // OPAPC.LOGGER.warn("Unknown item in duelBannedItems: {}", raw);
                }
            } catch (Exception ignored) {
                // Bad ID format; silently skip or log if you prefer
                // OPAPC.LOGGER.warn("Invalid item id in duelBannedItems: {}", raw);
            }
        }
    }

    /** True if the stack is banned by config OR (optionally) by tag. Runs on both client and server. */
    public static boolean isBlockedInDuel(ItemStack stack) {
        if (stack.isEmpty()) return false;

        // Config list check (fast)
        return BANNED_CONFIG_SET.contains(stack.getItem());
    }
}
