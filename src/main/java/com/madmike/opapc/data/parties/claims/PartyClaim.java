package com.madmike.opapc.data.parties.claims;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyClaim {
    private final Map<UUID, Donor> donations = new HashMap<>();
    private UUID partyId;
    private int boughtClaims = 0;
    private long currentDonationAmount = 0;
    private BlockPos pcb;

    public PartyClaim(UUID partyId) {
        this.partyId = partyId;
    }

    public void setPcb(BlockPos pcb) {
        this.pcb = pcb;
    }

    public BlockPos getPcbeBlockPos() {
        return pcb;
    }

    public Map<UUID, Donor> getDonations() {
        return donations;
    }

    public int getBoughtClaims() {
        return boughtClaims;
    }

    public long getCurrentDonationAmount() {
        return currentDonationAmount;
    }

    public NbtCompound writeToNbt() {
        NbtCompound nbt = new NbtCompound();

        nbt.putUuid("PartyId", partyId);
        nbt.putInt("BoughtClaims", boughtClaims);
        nbt.putLong("CurrentDonationAmount", currentDonationAmount);

        if (pcb != null) {
            nbt.putInt("PcbeX", pcb.getX());
            nbt.putInt("PcbeY", pcb.getY());
            nbt.putInt("PcbeZ", pcb.getZ());
        }

        NbtList donorList = new NbtList();
        for (Donor donor : donations.values()) {
            donorList.add(donor.toNbt());
        }
        nbt.put("Donations", donorList);

        return nbt;
    }

    public void readFromNbt(NbtCompound nbt) {
        this.partyId = nbt.getUuid("PartyId");
        this.boughtClaims = nbt.getInt("BoughtClaims");
        this.currentDonationAmount = nbt.getLong("CurrentDonationAmount");

        if (nbt.contains("PcbeX") && nbt.contains("PcbeY") && nbt.contains("PcbeZ")) {
            this.pcb = new BlockPos(nbt.getInt("PcbeX"), nbt.getInt("PcbeY"), nbt.getInt("PcbeZ"));
        } else {
            this.pcb = null;
        }

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

    public void deletePcbBlock(World world) {
        if (pcb != null) {
            if (world.isChunkLoaded(pcb)) {
                world.removeBlock(pcb, false); // remove block without dropping items
            }
            pcb = null;
        }
    }
}
