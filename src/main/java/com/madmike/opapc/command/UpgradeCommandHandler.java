package com.madmike.opapc.command;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.components.player.trades.UnlockedStoreSlotsComponent;
import com.madmike.opapc.config.OPAPCConfig;
import com.madmike.opapc.util.CurrencyUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class UpgradeCommandHandler {
    public static void handleUpgradeCommand(ServerPlayerEntity player, MinecraftServer server) {
        UnlockedStoreSlotsComponent unlockedSlotsComponent = OPAPCComponents.UNLOCKED_STORE_SLOTS.get(player);
        int unlockedSlots = unlockedSlotsComponent.getUnlockedSlots();
        if (unlockedSlots >= OPAPCConfig.maxStoreSlotsPerPlayer) {
            player.sendMessage(Text.literal("You’ve reached the maximum number of unlocked slots.").formatted(Formatting.GRAY));
            return;
        }

        CurrencyComponent wallet = ModComponents.CURRENCY.get(player);

        int cost = (unlockedSlots + 1) * 10000; // 1 gold = 10,000 bronze if using Numismatic default

        if (wallet.getValue() >= cost) {
            wallet.modify(-cost);
            unlockedSlotsComponent.increment(1);
            player.sendMessage(Text.literal("Upgraded your available sell slots by 1! It is now " + unlockedSlots).formatted(Formatting.GOLD));
        } else {
            CurrencyUtil.CoinBreakdown needed = CurrencyUtil.fromTotalBronze(cost);
            player.sendMessage(Text.literal("Not enough funds to upgrade. You need G: " + needed.gold() + ", S: " + needed.silver() + ", B: " + needed.bronze()).formatted(Formatting.RED));
        }
    }
}
