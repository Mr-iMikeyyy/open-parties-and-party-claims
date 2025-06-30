package com.madmike.opapc.data.parties.claims;

import com.madmike.opapc.features.entity.PartyClaimBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyClaim {

    private final Map<UUID, Donor> donations = new HashMap<>();
    private UUID partyId;
    private int boughtClaims = 0;
    private long currentDonationAmount = 0;

    private BlockPos pcbPos;

    public PartyClaim(UUID partyId) {
        this.partyId = partyId;
    }

    public void setPcb(BlockPos pcbPos) {
        this.pcbPos = pcbPos;
    }

    public BlockPos getPcbBlockPos() {
        return pcbPos;
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

    public @Nullable PartyClaimBlockEntity getBlockEntity(MinecraftServer server) {
        if (pcbPos == null) return null;
        ServerLevel world = server.getLevel(Level.OVERWORLD);
        if (world == null) return null;
        BlockEntity be = world.getBlockEntity(pcbPos);
        return be instanceof PartyClaimBlockEntity claimBe ? claimBe : null;
    }

    public CompoundTag writeToNbt() {
        CompoundTag nbt = new CompoundTag();

        nbt.putUUID("PartyId", partyId);
        nbt.putInt("BoughtClaims", boughtClaims);
        nbt.putLong("CurrentDonationAmount", currentDonationAmount);

        if (pcbPos != null) {
            nbt.putInt("PcbX", pcbPos.getX());
            nbt.putInt("PcbY", pcbPos.getY());
            nbt.putInt("PcbZ", pcbPos.getZ());
        }

        ListTag donorList = new ListTag();
        for (Donor donor : donations.values()) {
            donorList.add(donor.toNbt());
        }
        nbt.put("Donations", donorList);

        return nbt;
    }

    public void readFromNbt(CompoundTag nbt) {
        this.partyId = nbt.getUUID("PartyId");
        this.boughtClaims = nbt.getInt("BoughtClaims");
        this.currentDonationAmount = nbt.getLong("CurrentDonationAmount");

        if (nbt.contains("PcbX") && nbt.contains("PcbY") && nbt.contains("PcbZ")) {
            this.pcbPos = new BlockPos(nbt.getInt("PcbX"), nbt.getInt("PcbY"), nbt.getInt("PcbZ"));
        } else {
            this.pcbPos = null;
        }

        this.donations.clear();
        ListTag donorList = nbt.getList("Donations", Tag.TAG_COMPOUND);
        for (Tag element : donorList) {
            Donor donor = Donor.fromNbt((CompoundTag) element);
            this.donations.put(donor.playerId(), donor);
        }
    }

    public UUID getPartyId() {
        return partyId;
    }

    public void addDonation(UUID playerId, String name, long value) {
        Donor existing = donations.get(playerId);
        long total = value + (existing != null ? existing.amount() : 0);
        donations.put(playerId, new Donor(playerId, name, total));

        currentDonationAmount += value;

        while (currentDonationAmount >= (boughtClaims + 1) * 10_000L) {
            currentDonationAmount -= (boughtClaims + 1) * 10_000L;
            boughtClaims++;
        }
    }

    public void deletePcbBlock(MinecraftServer server) {
        if (pcbPos != null) {
            ServerLevel world = server.getLevel(Level.OVERWORLD);
            if (world != null && world.isLoaded(pcbPos)) {
                world.removeBlock(pcbPos, false);
            }
            pcbPos = null;
        }
    }
}
