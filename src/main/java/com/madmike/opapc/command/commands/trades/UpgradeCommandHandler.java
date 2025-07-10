package com.madmike.opapc.command.commands.trades;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.trade.components.player.UnlockedStoreSlotsComponent;
import com.madmike.opapc.config.OPAPCConfig;
import com.madmike.opapc.util.CurrencyUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class UpgradeCommandHandler {
    public static void handleUpgradeCommand(ServerPlayer player, MinecraftServer server) {
        UnlockedStoreSlotsComponent unlockedSlotsComponent = OPAPCComponents.UNLOCKED_STORE_SLOTS.get(player);
        int unlockedSlots = unlockedSlotsComponent.getUnlockedSlots();
        if (unlockedSlots >= OPAPCConfig.maxStoreSlotsPerPlayer) {
            player.sendSystemMessage(Component.literal("You’ve reached the maximum number of unlocked slots.").withStyle(ChatFormatting.GRAY));
            return;
        }

        CurrencyComponent wallet = ModComponents.CURRENCY.get(player);

        int cost = (unlockedSlots + 1) * 10_000; // 1 gold = 10,000 bronze

        if (wallet.getValue() >= cost) {
            wallet.modify(-cost);
            unlockedSlotsComponent.increment(1);
            player.sendSystemMessage(Component.literal("Upgraded your available sell slots by 1! It is now " + (unlockedSlots + 1)).withStyle(ChatFormatting.GOLD));
        } else {
            CurrencyUtil.CoinBreakdown needed = CurrencyUtil.fromTotalBronze(cost);
            player.sendSystemMessage(
                    Component.literal("Not enough funds to upgrade. You need G: " + needed.gold() +
                                    ", S: " + needed.silver() +
                                    ", B: " + needed.bronze())
                            .withStyle(ChatFormatting.RED)
            );
        }
    }
}
