package com.madmike.opapc.data.parties;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class PartyName {

    private final UUID partyId;
    private String name;

    public PartyName(UUID partyID, String name) {
        this.partyId = partyID;
        this.name = name;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("PartyId", partyId);
        nbt.putString("Name", name);
        return nbt;
    }

    public static PartyName fromNbt(CompoundTag nbt) {
        UUID partyID = nbt.getUUID("PartyId");
        String name = nbt.getString("Name");
        return new PartyName(partyID, name);
    }

    public UUID getPartyId() {
        return partyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
