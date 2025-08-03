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

package com.madmike.opapc.war.features;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.war.features.block.WarBlock;
import com.madmike.opapc.war.features.item.WarBlockItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;


public class WarFeatures {

    public static final Block WAR_BLOCK = new WarBlock(FabricBlockSettings.of().strength(50.0f, 1200.0f).dropsNothing().noLootTable());
    public static final Item WAR_BLOCK_ITEM = new WarBlockItem(WAR_BLOCK, new Item.Properties());

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation(OPAPC.MOD_ID, "war_block"), WAR_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(OPAPC.MOD_ID, "war_block"), WAR_BLOCK_ITEM);

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
            entries.accept(WAR_BLOCK_ITEM);
        });
    }

    public static Block getWarBlock() {
        return WAR_BLOCK;
    }
}
