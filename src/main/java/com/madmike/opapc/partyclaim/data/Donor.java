package com.madmike.opapc.partyclaim.data;


import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record Donor(String name, long amount) {

    public CompoundTag toNbt(UUID playerId) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("PlayerId", playerId);
        tag.putString("Name", name);
        tag.putLong("Amount", amount);
        return tag;
    }

    public static Donor fromNbt(CompoundTag tag) {
        String name = tag.getString("Name");
        long amount = tag.getLong("Amount");
        return new Donor(name, amount);
    }
}
