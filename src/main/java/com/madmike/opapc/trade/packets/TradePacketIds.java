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

package com.madmike.opapc.trade.packets;

import com.madmike.opapc.OPAPC;
import net.minecraft.resources.ResourceLocation;

public class TradePacketIds {

    public static final ResourceLocation REMOVE_OFFER = new ResourceLocation(OPAPC.MOD_ID, "remove_offer");
    public static final ResourceLocation BUY_OFFER = new ResourceLocation(OPAPC.MOD_ID, "buy_offer");

    public static final ResourceLocation REFRESH_TRADE_SCREEN = new ResourceLocation(OPAPC.MOD_ID, "refresh_trades");
    public static final ResourceLocation REBUILD_TABS = new ResourceLocation(OPAPC.MOD_ID, "rebuild_tabs");
}
