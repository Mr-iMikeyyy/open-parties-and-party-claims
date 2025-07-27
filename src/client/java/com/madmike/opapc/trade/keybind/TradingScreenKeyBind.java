package com.madmike.opapc.trade.keybind;

import com.madmike.opapc.trade.gui.TradingScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class TradingScreenKeyBind {

    private static KeyMapping openTradeKeyBind;

    public static void register () {

        // Register the keybind
        openTradeKeyBind = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.open_trading_screen",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_T,
                "Open Parties and Trades"
        ));

        // Register client tick event to check for key press
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openTradeKeyBind.consumeClick()) {
                client.setScreen(new TradingScreen());
            }
        });
    }
}
