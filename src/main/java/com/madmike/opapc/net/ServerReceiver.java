package com.madmike.opapc.net;

import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.net.packets.PacketIds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.UUID;

public class ServerReceiver {

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(PacketIds.REMOVE_OFFER, (server, player, handler, buf, responseSender) -> {
            UUID offerId = buf.readUUID();
            server.execute(() -> {
                OPAPCComponents.OFFERS.get(server.getScoreboard()).removeOffer(offerId);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(PacketIds.BUY_OFFER, (server, player, handler, buf, responseSender) -> {
            UUID offerId = buf.readUUID();
            server.execute(() -> {
                OPAPCComponents.OFFERS.get(server.getScoreboard()).buyOffer(offerId, player);
            });
        });
    }
}
