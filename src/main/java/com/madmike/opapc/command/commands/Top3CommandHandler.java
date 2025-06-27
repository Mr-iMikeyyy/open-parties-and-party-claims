package com.madmike.opapc.command.commands;

import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.data.trades.SellerInfo;
import com.madmike.opapc.util.CurrencyUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.List;

public class Top3CommandHandler {
    public static void handleTop3Command(ServerPlayerEntity player, MinecraftServer server) {

        List<SellerInfo> top3 = OPAPCComponents.SELLERS
                .get(server.getScoreboard())
                .getAllSellers()
                .stream()
                .sorted(Comparator.comparingLong(SellerInfo::totalSales).reversed())
                .limit(3)
                .toList();

        // Build message
        if (top3.isEmpty()) {
            player.sendMessage(Text.literal("No sellers found."), false);
            return;
        }

        player.sendMessage(Text.literal("Top 3 Sellers:"), false);
        for (int i = 0; i < top3.size(); i++) {
            SellerInfo seller = top3.get(i);
            String line = String.format(
                    "%d. %s - %s",
                    i + 1,
                    seller.name(),
                    CurrencyUtil.formatPrice(seller.totalSales(), false, false)
            );
            player.sendMessage(Text.literal(line), false);
        }
    }
}
