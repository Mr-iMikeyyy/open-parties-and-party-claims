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
