package com.madmike.opapc.components;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.components.player.timers.CombatTimerComponent;
import com.madmike.opapc.components.player.timers.TeleportCooldownComponent;
import com.madmike.opapc.components.player.trades.UnlockedStoreSlotsComponent;
import com.madmike.opapc.components.scoreboard.parties.PartyNamesComponent;
import com.madmike.opapc.components.scoreboard.parties.PartyClaimsComponent;
import com.madmike.opapc.components.scoreboard.trades.OffersComponent;
import com.madmike.opapc.components.scoreboard.trades.OfflineSalesComponent;
import com.madmike.opapc.components.scoreboard.trades.SellersComponent;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import dev.onyxstudios.cca.api.v3.scoreboard.ScoreboardComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.scoreboard.ScoreboardComponentInitializer;
import net.minecraft.resources.ResourceLocation;

public class OPAPCComponents implements ScoreboardComponentInitializer, EntityComponentInitializer {

    public static final ComponentKey<UnlockedStoreSlotsComponent> UNLOCKED_STORE_SLOTS =
            ComponentRegistryV3.INSTANCE.getOrCreate(new ResourceLocation(OPAPC.MOD_ID, "unlocked_store_slots"), UnlockedStoreSlotsComponent.class);

    public static final ComponentKey<CombatTimerComponent> COMBAT_TIMER =
            ComponentRegistryV3.INSTANCE.getOrCreate(new ResourceLocation(OPAPC.MOD_ID, "combat_timer"), CombatTimerComponent.class);

    public static final ComponentKey<TeleportCooldownComponent> TELE_TIMER =
            ComponentRegistryV3.INSTANCE.getOrCreate(new ResourceLocation(OPAPC.MOD_ID, "tele_timer"), TeleportCooldownComponent.class);

    public static final ComponentKey<OffersComponent> OFFERS =
            ComponentRegistryV3.INSTANCE.getOrCreate(new ResourceLocation(OPAPC.MOD_ID, "offers"), OffersComponent.class);

    public static final ComponentKey<PartyNamesComponent> PARTY_NAMES =
            ComponentRegistryV3.INSTANCE.getOrCreate(new ResourceLocation(OPAPC.MOD_ID, "party_names"), PartyNamesComponent.class);

    public static final ComponentKey<PartyClaimsComponent> PARTY_CLAIMS =
            ComponentRegistryV3.INSTANCE.getOrCreate(new ResourceLocation(OPAPC.MOD_ID, "party_claims"), PartyClaimsComponent.class);

    public static final ComponentKey<OfflineSalesComponent> OFFLINE_SALES =
            ComponentRegistryV3.INSTANCE.getOrCreate(new ResourceLocation(OPAPC.MOD_ID, "offline_sales"), OfflineSalesComponent.class);

    public static final ComponentKey<SellersComponent> SELLERS =
            ComponentRegistryV3.INSTANCE.getOrCreate(new ResourceLocation(OPAPC.MOD_ID, "sellers"), SellersComponent.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(UNLOCKED_STORE_SLOTS, UnlockedStoreSlotsComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
        registry.registerForPlayers(COMBAT_TIMER, CombatTimerComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
        registry.registerForPlayers(TELE_TIMER, TeleportCooldownComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
    }

    @Override
    public void registerScoreboardComponentFactories(ScoreboardComponentFactoryRegistry registry) {
        registry.registerScoreboardComponent(OFFERS, OffersComponent::new);
        registry.registerScoreboardComponent(PARTY_NAMES, PartyNamesComponent::new);
        registry.registerScoreboardComponent(PARTY_CLAIMS, PartyClaimsComponent::new);
        registry.registerScoreboardComponent(OFFLINE_SALES, OfflineSalesComponent::new);
        registry.registerScoreboardComponent(SELLERS, SellersComponent::new);
    }
}
