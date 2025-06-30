package com.madmike.opapc;

import com.madmike.opapc.gui.TradingScreen;
import com.madmike.opapc.net.ClientReceiver;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class OPAPCClient implements ClientModInitializer {

	private static KeyMapping openTradeKeybind;

	@Override
	public void onInitializeClient() {

		// Register the keybind
		openTradeKeybind = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.open_trading_screen",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_T,
				"Open Parties and Trades"
		));

		// Register client tick event to check for key press
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openTradeKeybind.consumeClick()) {
				client.setScreen(new TradingScreen());
			}
		});

		ClientReceiver.register();

		OPAPC.LOGGER.info("Client initialized");
	}
}