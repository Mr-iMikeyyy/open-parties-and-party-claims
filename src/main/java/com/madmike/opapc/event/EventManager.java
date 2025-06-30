package com.madmike.opapc.event;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.data.trades.OfflineSale;
import com.madmike.opapc.util.CurrencyUtil;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTabs;

import java.util.*;

import static com.madmike.opapc.features.OPAPCFeatures.PARTY_CLAIM_BLOCK_ITEM;

public class EventManager {
    public static void register() {

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            UUID playerId = player.getUUID();

            var component = OPAPCComponents.OFFLINE_SALES.get(server.getScoreboard());
            List<OfflineSale> sales = component.getSales(playerId);

            if (!sales.isEmpty()) {
                long totalProfit = sales.stream()
                        .mapToLong(OfflineSale::profitAmount)
                        .sum();

                // Give currency
                CurrencyComponent wallet = ModComponents.CURRENCY.get(player);
                wallet.modify(totalProfit);

                // Notify player
                Component coins = CurrencyUtil.formatPrice(totalProfit, false, false);
                player.sendSystemMessage(Component.literal("§6You made " + coins.getString() + " coins while you were away!"));

                // Remove sales
                component.clearSales(playerId);
            }

            OPAPCComponents.SELLERS.get(server.getScoreboard()).updateSellerNameIfChanged(playerId, player.getName().getString());
        });

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                OPAPCComponents.COMBAT_TIMER.get(serverPlayer).onDamaged();
            }
            return InteractionResult.PASS;
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
            entries.accept(PARTY_CLAIM_BLOCK_ITEM);
        });
    }
}
