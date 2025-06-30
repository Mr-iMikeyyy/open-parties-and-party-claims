package com.madmike.opapc.util;

import com.glisco.numismaticoverhaul.item.MoneyBagItem;
import com.glisco.numismaticoverhaul.item.NumismaticOverhaulItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class CurrencyUtil {

    public static final int BRONZE_PER_SILVER = 100;
    public static final int SILVER_PER_GOLD = 100;
    public static final int BRONZE_PER_GOLD = BRONZE_PER_SILVER * SILVER_PER_GOLD;

    // Converts gold, silver, bronze to total bronze
    public static long toTotalBronze(int gold, int silver, int bronze) {
        return (long) gold * BRONZE_PER_GOLD + (long) silver * BRONZE_PER_SILVER + bronze;
    }

    // Converts total bronze to gold/silver/bronze
    public static CoinBreakdown fromTotalBronze(long totalBronze) {
        int gold = (int) (totalBronze / BRONZE_PER_GOLD);
        totalBronze %= BRONZE_PER_GOLD;

        int silver = (int) (totalBronze / BRONZE_PER_SILVER);
        int bronze = (int) (totalBronze % BRONZE_PER_SILVER);

        return new CoinBreakdown(gold, silver, bronze);
    }

    public static long getValueOfItemStack(ItemStack stack) {
        Item item = stack.getItem();
        int count = stack.getCount();

        if (item == NumismaticOverhaulItems.BRONZE_COIN) {
            return count;
        } else if (item == NumismaticOverhaulItems.SILVER_COIN) {
            return count * 100L;
        } else if (item == NumismaticOverhaulItems.GOLD_COIN) {
            return count * 10_000L;
        } else if (item instanceof MoneyBagItem) {
            CompoundTag nbt = stack.getOrCreateTag();
            if (nbt.contains("Value")) {
                return nbt.getLong("Value");
            }
        }

        return 0;
    }

    public static Component formatPrice(long price, boolean discounted, boolean upcharged) {
        CoinBreakdown coins = fromTotalBronze(price);
        Component text;
        String priceString = "G: " + coins.gold() + ", S: " + coins.silver() + ", B: " + coins.bronze();
        if (discounted) {
            text = Component.literal(priceString).withStyle(ChatFormatting.GREEN);
        } else if (upcharged) {
            text = Component.literal(priceString).withStyle(ChatFormatting.RED);
        } else {
            text = Component.literal(priceString);
        }
        return text;
    }

    // Simple record to hold breakdown
    public record CoinBreakdown(int gold, int silver, int bronze) {}
}
