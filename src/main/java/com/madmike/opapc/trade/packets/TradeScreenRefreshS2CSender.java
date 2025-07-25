package com.madmike.opapc.trade.packets;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import static com.madmike.opapc.trade.packets.TradePacketIds.REBUILD_TABS;
import static com.madmike.opapc.trade.packets.TradePacketIds.REFRESH_TRADE_SCREEN;

public class TradeScreenRefreshS2CSender {

    public static void sendRefresh(ServerPlayer player) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        ServerPlayNetworking.send(player, REFRESH_TRADE_SCREEN, buf);
    }

    public static void sendRefreshToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendRefresh(player);
        }
    }

    public static void sendRebuildTabs(ServerPlayer player) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        ServerPlayNetworking.send(player, REBUILD_TABS, buf);
    }

    public static void sendRebuildTabsToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendRebuildTabs(player);
        }
    }
}
