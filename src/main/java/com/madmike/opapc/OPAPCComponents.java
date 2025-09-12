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

package com.madmike.opapc;

import com.madmike.opapc.bounty.components.scoreboard.BountyBoardComponent;
import com.madmike.opapc.duel.components.player.InDuelComponent;
import com.madmike.opapc.duel.components.scoreboard.DuelBannedItemsComponent;
import com.madmike.opapc.duel.components.scoreboard.DuelMapsComponent;
import com.madmike.opapc.player.component.player.PartyRejoinCooldownComponent;
import com.madmike.opapc.player.component.scoreboard.PlayerNameComponent;
import com.madmike.opapc.war.merc.component.player.MercComponent;
import com.madmike.opapc.warp.components.player.WarpCombatCooldownComponent;
import com.madmike.opapc.warp.components.player.WarpCooldownComponent;
import com.madmike.opapc.trade.components.player.UnlockedStoreSlotsComponent;
import com.madmike.opapc.pioneer.components.scoreboard.PartyClaimsComponent;
import com.madmike.opapc.trade.components.scoreboard.OffersComponent;
import com.madmike.opapc.trade.components.scoreboard.OfflineSalesComponent;
import com.madmike.opapc.trade.components.scoreboard.SellersComponent;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import dev.onyxstudios.cca.api.v3.scoreboard.ScoreboardComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.scoreboard.ScoreboardComponentInitializer;
import net.minecraft.resources.ResourceLocation;

public final class OPAPCComponents implements ScoreboardComponentInitializer, EntityComponentInitializer {

    private OPAPCComponents() {} // utility holder

    /* -------------------------------------------------------
     * Helpers
     * ----------------------------------------------------- */
    private static ResourceLocation id(String path) {
        return new ResourceLocation(OPAPC.MOD_ID, path);
    }

    /* -------------------------------------------------------
     * Player-scoped Components (attached to ServerPlayer)
     * ----------------------------------------------------- */

    // Gameplay state
    public static final ComponentKey<InDuelComponent> IN_DUEL =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("in_duel"), InDuelComponent.class);

    public static final ComponentKey<MercComponent> MERC =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("merc"), MercComponent.class);

    // Cooldowns / timers
    public static final ComponentKey<PartyRejoinCooldownComponent> PARTY_REJOIN_COOLDOWN =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("party_rejoin_cooldown"), PartyRejoinCooldownComponent.class);

    public static final ComponentKey<WarpCombatCooldownComponent> COMBAT_COOLDOWN =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("combat_cooldown"), WarpCombatCooldownComponent.class);

    public static final ComponentKey<WarpCooldownComponent> WARP_COOLDOWN =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("warp_cooldown"), WarpCooldownComponent.class);

    // Economy / shop
    public static final ComponentKey<UnlockedStoreSlotsComponent> UNLOCKED_STORE_SLOTS =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("unlocked_store_slots"), UnlockedStoreSlotsComponent.class);

    /* -------------------------------------------------------
     * Scoreboard-scoped Components (server-wide/stateful)
     * ----------------------------------------------------- */

    // Economy / shop
    public static final ComponentKey<OffersComponent> OFFERS =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("offers"), OffersComponent.class);

    public static final ComponentKey<OfflineSalesComponent> OFFLINE_SALES =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("offline_sales"), OfflineSalesComponent.class);

    public static final ComponentKey<SellersComponent> SELLERS =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("sellers"), SellersComponent.class);

    // Players / names
    public static final ComponentKey<PlayerNameComponent> PLAYER_NAMES =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("player_names"), PlayerNameComponent.class);

    // Parties / claims
    public static final ComponentKey<PartyClaimsComponent> PARTY_CLAIMS =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("party_claims"), PartyClaimsComponent.class);

    // Duels
    public static final ComponentKey<DuelMapsComponent> DUEL_MAPS =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("duel_maps"), DuelMapsComponent.class);

    public static final ComponentKey<DuelBannedItemsComponent> DUEL_BANNED_ITEMS =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("duel_banned_items"), DuelBannedItemsComponent.class);

    public static final ComponentKey<BountyBoardComponent> BOUNTY_BOARD =
            ComponentRegistryV3.INSTANCE.getOrCreate(id("bounty_board"), BountyBoardComponent.class);

    /* -------------------------------------------------------
     * Registration
     * ----------------------------------------------------- */

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        // Duel
        registry.registerForPlayers(IN_DUEL, InDuelComponent::new, RespawnCopyStrategy.ALWAYS_COPY);

        //Merc
        registry.registerForPlayers(MERC, MercComponent::new, RespawnCopyStrategy.ALWAYS_COPY);

        //Cooldowns
        registry.registerForPlayers(PARTY_REJOIN_COOLDOWN, PartyRejoinCooldownComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
        registry.registerForPlayers(COMBAT_COOLDOWN, WarpCombatCooldownComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
        registry.registerForPlayers(WARP_COOLDOWN, WarpCooldownComponent::new, RespawnCopyStrategy.ALWAYS_COPY);

        //Economy
        registry.registerForPlayers(UNLOCKED_STORE_SLOTS, UnlockedStoreSlotsComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
    }

    @Override
    public void registerScoreboardComponentFactories(ScoreboardComponentFactoryRegistry registry) {
        // Group by feature area for easy scanning
        // Economy
        registry.registerScoreboardComponent(OFFERS, OffersComponent::new);
        registry.registerScoreboardComponent(OFFLINE_SALES, OfflineSalesComponent::new);
        registry.registerScoreboardComponent(SELLERS, SellersComponent::new);

        // Player Names
        registry.registerScoreboardComponent(PLAYER_NAMES, PlayerNameComponent::new);

        // Parties / claims
        registry.registerScoreboardComponent( PARTY_CLAIMS, PartyClaimsComponent::new);

        // Duels
        registry.registerScoreboardComponent(DUEL_MAPS, DuelMapsComponent::new);
        registry.registerScoreboardComponent(DUEL_BANNED_ITEMS, DuelBannedItemsComponent::new);

        // Bounties
        registry.registerScoreboardComponent(BOUNTY_BOARD, BountyBoardComponent::new);
    }
}
