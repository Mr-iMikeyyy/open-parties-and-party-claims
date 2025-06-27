package com.madmike.opapc.components;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.components.player.timers.CombatTimerComponent;
import com.madmike.opapc.components.player.trades.UnlockedStoreSlotsComponent;
import com.madmike.opapc.components.scoreboard.parties.PartyNamesComponent;
import com.madmike.opapc.components.scoreboard.parties.PartyClaimsComponent;
import com.madmike.opapc.components.scoreboard.trades.OffersComponent;
import com.madmike.opapc.components.scoreboard.trades.OfflineSalesComponent;
import com.madmike.opapc.components.scoreboard.trades.SellersComponent;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import dev.onyxstudios.cca.api.v3.scoreboard.ScoreboardComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.scoreboard.ScoreboardComponentInitializer;
import net.minecraft.util.Identifier;

public class OPAPCComponents implements ScoreboardComponentInitializer, EntityComponentInitializer {
    public static final ComponentKey<UnlockedStoreSlotsComponent> UNLOCKED_STORE_SLOTS =
            ComponentRegistry.getOrCreate(new Identifier(OPAPC.MOD_ID, "unlocked_store_slots"), UnlockedStoreSlotsComponent.class);

    public static final ComponentKey<CombatTimerComponent> COMBAT_TIMER =
            ComponentRegistry.getOrCreate(new Identifier(OPAPC.MOD_ID, "combat_timer"), CombatTimerComponent.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry entityComponentFactoryRegistry) {
        entityComponentFactoryRegistry.registerForPlayers(UNLOCKED_STORE_SLOTS, UnlockedStoreSlotsComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
        entityComponentFactoryRegistry.registerForPlayers(COMBAT_TIMER, CombatTimerComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
    }

    public static final ComponentKey<OffersComponent> OFFERS =
            ComponentRegistry.getOrCreate(new Identifier(OPAPC.MOD_ID, "offers"), OffersComponent.class);

    public static final ComponentKey<PartyNamesComponent> PARTY_NAMES =
            ComponentRegistry.getOrCreate(new Identifier(OPAPC.MOD_ID, "party_names"), PartyNamesComponent.class);

    public static final ComponentKey<PartyClaimsComponent> PARTY_CLAIMS =
            ComponentRegistry.getOrCreate(new Identifier(OPAPC.MOD_ID, "party_claims"), PartyClaimsComponent.class);

    public static final ComponentKey<OfflineSalesComponent> OFFLINE_SALES =
            ComponentRegistry.getOrCreate(new Identifier(OPAPC.MOD_ID, "offline_sales"), OfflineSalesComponent.class);

    public static final ComponentKey<SellersComponent> SELLERS =
            ComponentRegistry.getOrCreate(new Identifier(OPAPC.MOD_ID, "sellers"), SellersComponent.class);

    @Override
    public void registerScoreboardComponentFactories(ScoreboardComponentFactoryRegistry scoreboardComponentFactoryRegistry) {
        scoreboardComponentFactoryRegistry.registerScoreboardComponent(OFFERS, OffersComponent::new);
        scoreboardComponentFactoryRegistry.registerScoreboardComponent(PARTY_NAMES, PartyNamesComponent::new);
        scoreboardComponentFactoryRegistry.registerScoreboardComponent(PARTY_CLAIMS, PartyClaimsComponent::new);
        scoreboardComponentFactoryRegistry.registerScoreboardComponent(OFFLINE_SALES, OfflineSalesComponent::new);
        scoreboardComponentFactoryRegistry.registerScoreboardComponent(SELLERS, SellersComponent::new);
    }
}
