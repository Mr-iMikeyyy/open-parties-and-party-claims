package com.madmike.opapc;

import com.madmike.opapc.gui.TradingScreen;
import com.madmike.opapc.net.ClientReceiver;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class OPAPCClient implements ClientModInitializer {

	private static KeyBinding openTradeKeybind;

	@Override
	public void onInitializeClient() {


		// Register the keybind
		openTradeKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.open_trading_screen",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_T, // default key
				"Open Parties and Trades"
		));

		// Register client tick event to check for the key press
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openTradeKeybind.wasPressed()) {
				client.setScreen(new TradingScreen());
			}
		});

		ClientReceiver.register();

		OPAPC.LOGGER.info("Client initialized");
	}
}