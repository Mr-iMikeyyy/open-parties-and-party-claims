package com.madmike.opapc.partyclaim.data;


import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record Donor(UUID playerId, long amount) {

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("PlayerId", playerId);
        nbt.putLong("Amount", amount);
        return nbt;
    }

    public static Donor fromNbt(CompoundTag nbt) {
        UUID playerId = nbt.getUUID("PlayerId");
        long amount = nbt.getLong("Amount");
        return new Donor(playerId, amount);
    }
}
