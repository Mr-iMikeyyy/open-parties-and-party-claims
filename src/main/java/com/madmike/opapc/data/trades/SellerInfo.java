package com.madmike.opapc.data.trades;

import net.minecraft.nbt.NbtCompound;

import java.util.UUID;

public record SellerInfo(UUID id, String name, long totalSales) {
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("Id", id);
        nbt.putString("Name", name);
        nbt.putLong("Sales", totalSales);
        return nbt;
    }

    public static SellerInfo fromNbt(NbtCompound nbt) {
        UUID id = nbt.getUuid("Id");
        String name = nbt.getString("Name");
        long totalSales = nbt.getLong("Sales");
        return new SellerInfo(id, name, totalSales);
    }

    public SellerInfo addSales(long amount) {
        return new SellerInfo(id, name, totalSales + amount);
    }

    public SellerInfo withName(String newName) {
        return new SellerInfo(id, newName, totalSales);
    }
}
