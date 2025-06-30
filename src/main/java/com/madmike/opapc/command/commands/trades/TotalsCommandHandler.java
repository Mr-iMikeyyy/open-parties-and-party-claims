package com.madmike.opapc.command.commands.trades;

import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.util.CurrencyUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class TotalsCommandHandler {
    public static void handleTotalsCommand(ServerPlayer player, MinecraftServer server) {
        long total = OPAPCComponents.SELLERS.get(server.getScoreboard()).getSellerInfo(player.getUUID()).totalSales();
        CurrencyUtil.CoinBreakdown coins = CurrencyUtil.fromTotalBronze(total);

        player.sendSystemMessage(
                Component.literal("Your total sales are: G: " + coins.gold() + ", S: " + coins.silver() + ", B: " + coins.bronze() + ".")
        );
    }
}
