package com.madmike.opapc;

import com.madmike.opapc.trade.keybind.TradingScreenKeyBind;
import com.madmike.opapc.trade.net.TradingClientReceiver;
import net.fabricmc.api.ClientModInitializer;

public class OPAPCClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {

		TradingScreenKeyBind.register();

		TradingClientReceiver.register();

		OPAPC.LOGGER.info("Client initialized");
	}
}