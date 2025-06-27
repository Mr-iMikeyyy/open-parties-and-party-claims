package com.madmike.opapc.event;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.components.scoreboard.trades.OfflineSalesComponent;
import com.madmike.opapc.data.trades.OfflineSale;
import com.madmike.opapc.util.CurrencyUtil;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

import java.util.*;

public class EventManager {
    public static void register() {

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            UUID playerId = player.getUuid();

            OfflineSalesComponent component = OPAPCComponents.OFFLINE_SALES.get(server.getScoreboard());
            List<OfflineSale> sales = component.getSales(playerId);

            if (!sales.isEmpty()) {
                long totalProfit = sales.stream()
                        .mapToLong(OfflineSale::profitAmount)
                        .sum();

                // Give currency
                CurrencyComponent wallet = ModComponents.CURRENCY.get(player);
                wallet.modify(totalProfit);

                // Notify player
                Text coins = CurrencyUtil.formatPrice(totalProfit, false, false);
                player.sendMessage(Text.literal("§6You made " + coins + " coins while you were away!"), false);

                // Remove sales
                component.clearSales(playerId);
            }

            OPAPCComponents.SELLERS.get(server.getScoreboard()).updateSellerNameIfChanged(playerId, player.getName().getString());
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity) {
                OPAPCComponents.COMBAT_TIMER.get(player).onDamaged();
            }
            return ActionResult.PASS;
        });

    }
}
