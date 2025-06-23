package com.madmike.opapc.net.packets;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

import java.util.UUID;

import static com.madmike.opapc.net.packets.PacketIds.REMOVE_OFFER;

public class RemoveOfferC2SPacket {
    public static void send(UUID offerId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(offerId);
        ClientPlayNetworking.send(REMOVE_OFFER, buf);
    }
}
