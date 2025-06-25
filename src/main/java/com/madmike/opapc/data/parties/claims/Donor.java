package com.madmike.opapc.data.parties.claims;

import net.minecraft.nbt.NbtCompound;

import java.util.UUID;

public record Donor(UUID playerId, String name, long amount) {

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("PlayerId", playerId);
        nbt.putString("Name", name);
        nbt.putLong("Amount", amount);
        return nbt;
    }

    public static Donor fromNbt(NbtCompound nbt) {
        UUID playerId = nbt.getUuid("PlayerId");
        String name = nbt.getString("Name");
        long amount = nbt.getLong("Amount");
        return new Donor(playerId, name, amount);
    }
}
