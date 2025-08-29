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

import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.trade.net.packets.TradePacketIds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.UUID;

public class TradeServerReceiver {

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(TradePacketIds.REMOVE_OFFER, (server, player, handler, buf, responseSender) -> {
            UUID offerId = buf.readUUID();
            server.execute(() -> {
                OPAPCComponents.OFFERS.get(server.getScoreboard()).removeOffer(offerId);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(TradePacketIds.BUY_OFFER, (server, player, handler, buf, responseSender) -> {
            UUID offerId = buf.readUUID();
            server.execute(() -> {
                OPAPCComponents.OFFERS.get(server.getScoreboard()).buyOffer(offerId, player);
            });
        });
    }
}
