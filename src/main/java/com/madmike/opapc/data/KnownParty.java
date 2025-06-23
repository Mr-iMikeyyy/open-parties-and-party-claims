package com.madmike.opapc.data;

import com.madmike.opapc.block.PartyClaimBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;

import java.util.UUID;

public class KnownParty() {

    private final UUID partyId;
    private String name;
    private int claims;
    private PartyClaimBlock partyClaimBlock;
    private long donations;

    public KnownParty(UUID partyID, String name) {
        this.partyId = partyID;
        this.name = name;
        this.claims = 0;
        this.partyClaimBlock = null;
    }
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("PartyID", partyId);
        nbt.putString("Name", name);
        nbt.putInt("Claims", claims);
        nbt.putLong
        return nbt;
    }

    public static KnownParty fromNbt(NbtCompound nbt) {
        UUID partyID = nbt.getUuid("PartyID");
        String name = nbt.getString("Name");
        return new KnownParty(partyID, name);
    }

    public void writeToBuf(PacketByteBuf buf) {
        buf.writeUuid(partyID);
        buf.writeString(name);
    }

    public static KnownParty readFromBuf(PacketByteBuf buf) {
        UUID partyID = buf.readUuid();
        String name = buf.readString();
        return new KnownParty(partyID, name);
    }

    public UUID getPartyId() {
        return partyId;
    }

    public int getClaims() {
        return claims;
    }

    public void setClaims(int claims) {
        this.claims = claims;
    }

    public PartyClaimBlock getPartyClaimBlock() {
        return partyClaimBlock;
    }

    public void setPartyClaimBlock(PartyClaimBlock partyClaimBlock) {
        this.partyClaimBlock = partyClaimBlock;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
