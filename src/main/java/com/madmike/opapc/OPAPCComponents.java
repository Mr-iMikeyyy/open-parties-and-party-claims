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

import com.madmike.opapc.duel.components.scoreboard.DuelMapsComponent;
import com.madmike.opapc.partyclaim.components.player.PartyRejoinCooldownComponent;
import com.madmike.opapc.player.name.PlayerNameComponent;
import com.madmike.opapc.warp.components.player.WarpCombatCooldownComponent;
import com.madmike.opapc.warp.components.player.WarpCooldownComponent;
import com.madmike.opapc.trade.components.player.UnlockedStoreSlotsComponent;
import com.madmike.opapc.partyclaim.components.scoreboard.PartyClaimsComponent;
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

public class OPAPCComponents implements ScoreboardComponentInitializer, EntityComponentInitializer {

    //Player Components

    public static final ComponentKey<UnlockedStoreSlotsComponent> UNLOCKED_STORE_SLOTS =
            ComponentRegistryV3.INSTANCE.getOrCreate(new ResourceLocation(OPAPC.MOD_ID, "unlocked_store_slots"), UnlockedStoreSlotsComponent.class);

    public static final ComponentKey<WarpCombatCooldownComponent> COMBAT_COOLDOWN =
            ComponentRegistryV3.INSTANCE.getOrCreate(new ResourceLocation(OPAPC.MOD_ID, "combat_cooldown"), WarpCombatCooldownComponent.class);

    public static final ComponentKey<WarpCooldownComponent> WARP_COOLDOWN =
            ComponentRegistryV3.INSTANCE.getOrCreate(new ResourceLocation(OPAPC.MOD_ID, "warp_cooldown"), WarpCooldownComponent.class);

    public static final ComponentKey<PartyRejoinCooldownComponent> PARTY_REJOIN_COOLDOWN =
            ComponentRegistryV3.INSTANCE.getOrCreate(new ResourceLocation(OPAPC.MOD_ID, "party_rejoin_cooldown"), PartyRejoinCooldownComponent.class);

    //Scoreboard Components

    public static final ComponentKey<OffersComponent> OFFERS =
            ComponentRegistryV3.INSTANCE.getOrCreate(new ResourceLocation(OPAPC.MOD_ID, "offers"), OffersComponent.class);

    public static final ComponentKey<DuelMapsComponent> DUEL_MAPS =
            ComponentRegistryV3.INSTANCE.getOrCreate(new ResourceLocation(OPAPC.MOD_ID, "duel_maps"), DuelMapsComponent.class);

    public static final ComponentKey<PartyClaimsComponent> PARTY_CLAIMS =
            ComponentRegistryV3.INSTANCE.getOrCreate(new ResourceLocation(OPAPC.MOD_ID, "party_claims"), PartyClaimsComponent.class);

    public static final ComponentKey<OfflineSalesComponent> OFFLINE_SALES =
            ComponentRegistryV3.INSTANCE.getOrCreate(new ResourceLocation(OPAPC.MOD_ID, "offline_sales"), OfflineSalesComponent.class);

    public static final ComponentKey<SellersComponent> SELLERS =
            ComponentRegistryV3.INSTANCE.getOrCreate(new ResourceLocation(OPAPC.MOD_ID, "sellers"), SellersComponent.class);

    public static final ComponentKey<PlayerNameComponent> PLAYER_NAMES =
            ComponentRegistryV3.INSTANCE.getOrCreate(new ResourceLocation(OPAPC.MOD_ID, "player_names"), PlayerNameComponent.class);



    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(UNLOCKED_STORE_SLOTS, UnlockedStoreSlotsComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
        registry.registerForPlayers(COMBAT_COOLDOWN, WarpCombatCooldownComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
        registry.registerForPlayers(WARP_COOLDOWN, WarpCooldownComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
        registry.registerForPlayers(PARTY_REJOIN_COOLDOWN, PartyRejoinCooldownComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
    }

    @Override
    public void registerScoreboardComponentFactories(ScoreboardComponentFactoryRegistry registry) {
        registry.registerScoreboardComponent(OFFERS, OffersComponent::new);
        registry.registerScoreboardComponent(DUEL_MAPS, DuelMapsComponent::new);
        registry.registerScoreboardComponent(PARTY_CLAIMS, PartyClaimsComponent::new);
        registry.registerScoreboardComponent(OFFLINE_SALES, OfflineSalesComponent::new);
        registry.registerScoreboardComponent(SELLERS, SellersComponent::new);
        registry.registerScoreboardComponent(PLAYER_NAMES, PlayerNameComponent::new);
    }
}
