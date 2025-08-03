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

package com.madmike.opapc.trade.events;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.trade.data.OfflineSale;
import com.madmike.opapc.util.CurrencyUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public class TradeEvents {
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

            OPAPCComponents.SELLERS.get(server.getScoreboard()).updateSellerNameIfChanged(playerId, player.getGameProfile().getName());
        });
    }
}
