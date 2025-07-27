package com.madmike.opapc.trade.net;

import com.madmike.opapc.trade.gui.TradingScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.Screen;

import static com.madmike.opapc.trade.packets.TradePacketIds.REBUILD_TABS;
import static com.madmike.opapc.trade.packets.TradePacketIds.REFRESH_TRADE_SCREEN;

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
