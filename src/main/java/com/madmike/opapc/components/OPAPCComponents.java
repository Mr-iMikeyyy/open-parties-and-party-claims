package com.madmike.opapc.components;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.partyclaim.components.player.CombatTimerComponent;
import com.madmike.opapc.partyclaim.components.player.TeleportCooldownComponent;
import com.madmike.opapc.trade.components.player.UnlockedStoreSlotsComponent;
import com.madmike.opapc.partyclaim.components.scoreboard.PartyNamesComponent;
import com.madmike.opapc.partyclaim.components.scoreboard.PartyClaimsComponent;
import com.madmike.opapc.trade.components.scoreboard.OffersComponent;
import com.madmike.opapc.trade.components.scoreboard.OfflineSalesComponent;
import com.madmike.opapc.trade.components.scoreboard.SellersComponent;
import com.madmike.opapc.war.components.scoreboard.WarStatsComponent;
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

    public static final ComponentKey<WarStatsComponent> WAR_STATS =
            ComponentRegistryV3.INSTANCE.getOrCreate(new ResourceLocation(OPAPC.MOD_ID, "war_stats"), WarStatsComponent.class);

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
        registry.registerScoreboardComponent(WAR_STATS, WarStatsComponent::new);
    }
}
