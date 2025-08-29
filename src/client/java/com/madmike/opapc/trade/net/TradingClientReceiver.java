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

package com.madmike.opapc.trade.net;

import com.madmike.opapc.trade.gui.TradingScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.Screen;

import static com.madmike.opapc.trade.net.packets.TradePacketIds.REBUILD_TABS;
import static com.madmike.opapc.trade.net.packets.TradePacketIds.REFRESH_TRADE_SCREEN;

public class TradingClientReceiver {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(REFRESH_TRADE_SCREEN, (client, handler, buf, responseSender) -> {
            client.execute(() -> {
                Screen currentScreen = client.screen;
                if (currentScreen instanceof TradingScreen screen) {
                    screen.refresh();
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(REBUILD_TABS, (client, handler, buf, responseSender) -> {
            client.execute(() -> {
                Screen currentScreen = client.screen;
                if (currentScreen instanceof TradingScreen screen) {
                    screen.rebuildTabs();
                }
            });
        });
    }
}
