package com.madmike.opapc.trade.net.packets;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

import static com.madmike.opapc.trade.packets.TradePacketIds.BUY_OFFER;

public class BuyOfferC2SPacket {
    public static void send(UUID offerId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeUUID(offerId);
        ClientPlayNetworking.send(BUY_OFFER, buf);
    }
}
