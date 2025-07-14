package com.madmike.opapc.trade.data;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record SellerInfo(UUID id, String name, long totalSales) {

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("Id", id);
        nbt.putString("Name", name);
        nbt.putLong("Sales", totalSales);
        return nbt;
    }

    public static SellerInfo fromNbt(CompoundTag nbt) {
        UUID id = nbt.getUUID("Id");
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
