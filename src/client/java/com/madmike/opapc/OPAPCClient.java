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

package com.madmike.opapc;

import com.madmike.opapc.duel.DuelClientEvents;
import com.madmike.opapc.trade.keybind.TradingScreenKeyBind;
import com.madmike.opapc.trade.net.TradingClientReceiver;
import net.fabricmc.api.ClientModInitializer;

public class OPAPCClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {

		TradingScreenKeyBind.register();

		TradingClientReceiver.register();

		DuelClientEvents.register();

		OPAPC.LOGGER.info("Client initialized");
	}
}