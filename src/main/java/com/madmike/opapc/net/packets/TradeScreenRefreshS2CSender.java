package com.madmike.opapc.net.packets;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import static com.madmike.opapc.net.packets.PacketIds.REBUILD_TABS;
import static com.madmike.opapc.net.packets.PacketIds.REFRESH_TRADE_SCREEN;

public class TradeScreenRefreshS2CSender {


    public static void sendRefresh(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        ServerPlayNetworking.send(player, REFRESH_TRADE_SCREEN, buf);
    }

    public static void sendRefreshToAll(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendRefresh(player);
        }
    }

    public static void sendRebuildTabs(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        ServerPlayNetworking.send(player, REBUILD_TABS, buf);
    }

    public static void sendRebuildTabsToAll(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendRebuildTabs(player);
        }
    }
}
