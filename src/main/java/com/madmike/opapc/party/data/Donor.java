package com.madmike.opapc.party.data;


import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record Donor(UUID playerId, String name, long amount) {

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("PlayerId", playerId);
        nbt.putString("Name", name);
        nbt.putLong("Amount", amount);
        return nbt;
    }

    public static Donor fromNbt(CompoundTag nbt) {
        UUID playerId = nbt.getUUID("PlayerId");
        String name = nbt.getString("Name");
        long amount = nbt.getLong("Amount");
        return new Donor(playerId, name, amount);
    }
}
