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

package com.madmike.opapc.duel.components.scoreboard;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scoreboard component that holds duel-banned items and tags.
 * - Persists to NBT
 * - Auto-syncs to clients
 * - Clients query it to block uses locally
 */
public final class DuelBannedItemsComponent implements ComponentV3, AutoSyncedComponent {
    // Raw ids we persist/sync
    private final Set<ResourceLocation> itemIds = new HashSet<>();
    private final Set<ResourceLocation> tagIds  = new HashSet<>();

    // Resolved cache (fast lookup). Not persisted—rebuilt from ids.
    private final Set<Item> resolvedItems = new HashSet<>();

    // Server-toggled duel rule (synced to clients)
    private boolean blockPlacementDisabled = true; // default: disallow block placement during duels

    // Provider
    private MinecraftServer server;
    private Scoreboard sb;

    public DuelBannedItemsComponent(Scoreboard sb, MinecraftServer sv) {
        this.server = sv;
        this.sb = sb;
    }

    /* ---------------- Public API ---------------- */

    /** Server-only: replace entire contents and sync to all players. */
    public void setAll(List<String> itemIdStrings, List<String> tagIdStrings) {
        itemIds.clear();
        tagIds.clear();

        if (itemIdStrings != null) {
            for (String s : itemIdStrings) {
                try { itemIds.add(new ResourceLocation(s.trim())); } catch (Exception ignored) {}
            }
        }
        if (tagIdStrings != null) {
            for (String s : tagIdStrings) {
                try { tagIds.add(new ResourceLocation(s.trim())); } catch (Exception ignored) {}
            }
        }
        rebuildCache();
    }

    /** Add/remove helpers if you want dynamic edits at runtime. */
    public boolean addItemId(ResourceLocation id) {
        boolean changed = itemIds.add(id);
        if (changed) {
            rebuildCache();
        }
        return changed;
    }
    public boolean removeItemId(ResourceLocation id) {
        boolean changed = itemIds.remove(id);
        if (changed) {
            rebuildCache();
        }
        return changed;
    }
    public boolean addTagId(ResourceLocation id) {
        return tagIds.add(id);
    }
    public boolean removeTagId(ResourceLocation id) {
        return tagIds.remove(id);
    }

    /** Query from client or server. */
    public boolean isBlocked(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // explicit items first (fast)
        if (resolvedItems.contains(stack.getItem())) return true;
        // tag membership (dynamic; follows datapack tag reloads automatically)
        for (ResourceLocation tagId : tagIds) {
            if (stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, tagId))) {
                return true;
            }
        }
        return false;
    }

    //server Only
    public void setBlockPlacementDisabled(boolean disabled) {
        if (this.blockPlacementDisabled != disabled) {
            this.blockPlacementDisabled = disabled;
        }
    }

    /** Client/server read. */
    public boolean isBlockPlacementDisabled() {
        return blockPlacementDisabled;
    }

    /* ---------------- Internal ---------------- */

    private void rebuildCache() {
        resolvedItems.clear();
        for (ResourceLocation id : itemIds) {
            BuiltInRegistries.ITEM.getOptional(id).ifPresent(resolvedItems::add);
        }
    }

    /* ---------------- Persistence & Sync ---------------- */

    @Override
    public void readFromNbt(CompoundTag tag) {
        itemIds.clear();
        tagIds.clear();

        ListTag items = tag.getList("ItemIds", CompoundTag.TAG_STRING);
        for (int i = 0; i < items.size(); i++) {
            try { itemIds.add(new ResourceLocation(items.getString(i))); } catch (Exception ignored) {}
        }
        ListTag tags = tag.getList("TagIds", CompoundTag.TAG_STRING);
        for (int i = 0; i < tags.size(); i++) {
            try { tagIds.add(new ResourceLocation(tags.getString(i))); } catch (Exception ignored) {}
        }
        rebuildCache();
    }

    @Override
    public void writeToNbt(@NotNull CompoundTag tag) {
        ListTag items = new ListTag();
        for (ResourceLocation id : itemIds) items.add(StringTag.valueOf(id.toString()));
        tag.put("ItemIds", items);

        ListTag tags = new ListTag();
        for (ResourceLocation id : tagIds) tags.add(StringTag.valueOf(id.toString()));
        tag.put("TagIds", tags);
    }

}
