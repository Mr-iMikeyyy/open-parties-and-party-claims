package com.madmike.opapc.command.commands.trades;

import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.data.trades.SellerInfo;
import com.madmike.opapc.util.CurrencyUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;

public class TopCommandHandler {
    public static void handleTopCommand(ServerPlayer player, MinecraftServer server) {

        List<SellerInfo> top = OPAPCComponents.SELLERS
                .get(server.getScoreboard())
                .getAllSellers()
                .stream()
                .sorted(Comparator.comparingLong(SellerInfo::totalSales).reversed())
                .limit(10)
                .toList();

        if (top.isEmpty()) {
            player.sendSystemMessage(Component.literal("No sellers found."));
            return;
        }

        player.sendSystemMessage(Component.literal("Top 10 Sellers:"));
        for (int i = 0; i < top.size(); i++) {
            SellerInfo seller = top.get(i);
            String line = String.format(
                    "%d. %s - %s",
                    i + 1,
                    seller.name(),
                    CurrencyUtil.formatPrice(seller.totalSales(), false, false).getString()
            );
            player.sendSystemMessage(Component.literal(line));
        }
    }
}
