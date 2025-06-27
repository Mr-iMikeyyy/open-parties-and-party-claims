package com.madmike.opapc.command.commands;

import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.util.CurrencyUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class TotalsCommandHandler {
    public static void handleTotalsCommand(ServerPlayerEntity player, MinecraftServer server) {
        long total = OPAPCComponents.SELLERS.get(server.getScoreboard()).getSellerInfo(player.getUuid()).totalSales();
        CurrencyUtil.CoinBreakdown coins = CurrencyUtil.fromTotalBronze(total);

        player.sendMessage(Text.literal("Your total sales are: G: " + coins.gold() + ", S: " + coins.silver() + ", B: " + coins.bronze() + "."));
    }
}
