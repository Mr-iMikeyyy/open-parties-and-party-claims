package com.madmike.opapc.trade.packets;

import com.madmike.opapc.components.OPAPCComponents;
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
