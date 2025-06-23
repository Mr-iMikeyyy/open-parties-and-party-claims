package com.madmike.opapc.components;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.components.player.UnlockedStoreSlotsComponent;
import com.madmike.opapc.components.scoreboard.KnownPartiesComponent;
import com.madmike.opapc.components.scoreboard.OffersComponent;
import com.madmike.opapc.components.scoreboard.OfflineSalesComponent;
import com.madmike.opapc.components.scoreboard.SellersComponent;
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

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry entityComponentFactoryRegistry) {
        entityComponentFactoryRegistry.registerForPlayers(UNLOCKED_STORE_SLOTS, UnlockedStoreSlotsComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
    }

    public static final ComponentKey<OffersComponent> OFFERS =
            ComponentRegistry.getOrCreate(new Identifier(OPAPC.MOD_ID, "offers"), OffersComponent.class);

    public static final ComponentKey<KnownPartiesComponent> KNOWN_PARTIES =
            ComponentRegistry.getOrCreate(new Identifier(OPAPC.MOD_ID, "known_parties"), KnownPartiesComponent.class);

    public static final ComponentKey<OfflineSalesComponent> OFFLINE_SALES =
            ComponentRegistry.getOrCreate(new Identifier(OPAPC.MOD_ID, "offline_sales"), OfflineSalesComponent.class);

    public static final ComponentKey<OfflineSalesComponent> SELLERS =
            ComponentRegistry.getOrCreate(new Identifier(OPAPC.MOD_ID, "sellers"), OfflineSalesComponent.class);

    @Override
    public void registerScoreboardComponentFactories(ScoreboardComponentFactoryRegistry scoreboardComponentFactoryRegistry) {
        scoreboardComponentFactoryRegistry.registerScoreboardComponent(OFFERS, OffersComponent::new);
        scoreboardComponentFactoryRegistry.registerScoreboardComponent(KNOWN_PARTIES, KnownPartiesComponent::new);
        scoreboardComponentFactoryRegistry.registerScoreboardComponent(OFFLINE_SALES, OfflineSalesComponent::new);
        scoreboardComponentFactoryRegistry.registerScoreboardComponent(SELLERS, SellersComponent::new);
    }
}
