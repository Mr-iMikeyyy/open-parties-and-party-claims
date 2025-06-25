package com.madmike.opapc.data.parties.claims;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyClaim {
    private final Map<UUID, Donor> donations = new HashMap<>();
    private final UUID partyId;
    private int boughtClaims = 0;
    private long currentDonationAmount = 0;

    public PartyClaim(UUID partyId) {
        this.partyId = partyId;
    }

    public Map<UUID, Donor> getDonations() {
        return donations;
    }

    public int getBoughtClaims() {
        return boughtClaims;
    }

    public void setBoughtClaims(int boughtClaims) {
        this.boughtClaims = boughtClaims;
    }

    public long getCurrentDonationAmount() {
        return currentDonationAmount;
    }

    public void setCurrentDonationAmount(long currentDonationAmount) {
        this.currentDonationAmount = currentDonationAmount;
    }

    // 🔽 Serialize to NBT
    public NbtCompound writeToNbt() {
        NbtCompound nbt = new NbtCompound();

        nbt.putInt("BoughtClaims", boughtClaims);
        nbt.putLong("CurrentDonationAmount", currentDonationAmount);

        NbtList donorList = new NbtList();
        for (Donor donor : donations.values()) {
            donorList.add(donor.toNbt());
        }
        nbt.put("Donations", donorList);

        return nbt;
    }

    // 🔼 Deserialize from NBT
    public void readFromNbt(NbtCompound nbt) {
        this.boughtClaims = nbt.getInt("BoughtClaims");
        this.currentDonationAmount = nbt.getLong("CurrentDonationAmount");

        this.donations.clear();
        NbtList donorList = nbt.getList("Donations", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : donorList) {
            Donor donor = Donor.fromNbt((NbtCompound) element);
            this.donations.put(donor.playerId(), donor);
        }
    }

    public UUID getPartyId() {
        return partyId;
    }

    public void addDonation(UUID playerId, String name, long value) {
        // Update donor contribution
        Donor existing = donations.get(playerId);
        long total = value + (existing != null ? existing.amount() : 0);
        donations.put(playerId, new Donor(playerId, name, total));

        // Add to current pool
        currentDonationAmount += value;

        // Check if enough for a new claim
        while (currentDonationAmount >= (boughtClaims + 1) * 10_000L) {
            currentDonationAmount -= (boughtClaims + 1) * 10_000L;
            boughtClaims++;
        }


    }
}
