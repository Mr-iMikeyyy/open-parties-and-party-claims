package com.madmike.opapc.data.trades;

import net.minecraft.nbt.NbtCompound;

import java.util.UUID;

public record OfflineSale(UUID sellerID, long profitAmount) {
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("SellerID", sellerID);
        nbt.putLong("Profit", profitAmount);
        return nbt;
    }

    public static OfflineSale fromNbt(NbtCompound nbt) {
        UUID sellerId = nbt.getUuid("SellerID");
        long profit = nbt.getLong("Profit");
        return new OfflineSale(sellerId, profit);
    }
}
