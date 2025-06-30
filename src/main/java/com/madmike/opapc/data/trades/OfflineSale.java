package com.madmike.opapc.data.trades;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record OfflineSale(UUID sellerID, long profitAmount) {

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("SellerID", sellerID);
        nbt.putLong("Profit", profitAmount);
        return nbt;
    }

    public static OfflineSale fromNbt(CompoundTag nbt) {
        UUID sellerId = nbt.getUUID("SellerID");
        long profit = nbt.getLong("Profit");
        return new OfflineSale(sellerId, profit);
    }
}
