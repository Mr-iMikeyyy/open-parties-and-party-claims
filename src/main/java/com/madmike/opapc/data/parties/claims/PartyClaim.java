package com.madmike.opapc.data.parties.claims;

import com.madmike.opapc.features.entity.PartyClaimBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
        ServerWorld world = server.getWorld(World.OVERWORLD);
        if (world == null) return null;
        BlockEntity be = world.getBlockEntity(pcbPos);
        return be instanceof PartyClaimBlockEntity claimBe ? claimBe : null;
    }

    public NbtCompound writeToNbt() {
        NbtCompound nbt = new NbtCompound();

        nbt.putUuid("PartyId", partyId);
        nbt.putInt("BoughtClaims", boughtClaims);
        nbt.putLong("CurrentDonationAmount", currentDonationAmount);

        if (pcbPos != null) {
            nbt.putInt("PcbX", pcbPos.getX());
            nbt.putInt("PcbY", pcbPos.getY());
            nbt.putInt("PcbZ", pcbPos.getZ());
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

        if (nbt.contains("PcbX") && nbt.contains("PcbY") && nbt.contains("PcbZ") && nbt.contains("PcbDimension")) {
            this.pcbPos = new BlockPos(nbt.getInt("PcbX"), nbt.getInt("PcbY"), nbt.getInt("PcbZ"));
            Identifier dimId = new Identifier(nbt.getString("PcbDimension"));
        } else {
            this.pcbPos = null;
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
            ServerWorld world = server.getWorld(World.OVERWORLD);
            if (world != null && world.isChunkLoaded(pcbPos)) {
                world.removeBlock(pcbPos, false);
            }
            pcbPos = null;
        }
    }
}
