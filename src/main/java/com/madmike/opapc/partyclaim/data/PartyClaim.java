package com.madmike.opapc.partyclaim.data;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import xaero.pac.common.server.player.config.api.PlayerConfigOptions;

import java.util.*;

public class PartyClaim {

    private final Map<UUID, Donor> donations = new HashMap<>();
    private UUID partyId;
    private int boughtClaims;
    private BlockPos warpPos = null;
    private long lastWarInsuranceTime;
    private long lastRaidInsuranceTime;
    private int warDefencesWon;
    private int warDefencesLost;
    private int warAttacksLost;
    private int warAttacksWon;
    private int claimsLostToWar;
    private int claimsGainedFromWar;
    private int raidsWon;
    private int raidsLost;

    public PartyClaim(UUID partyId) {
        this.partyId = partyId;
        this.lastWarInsuranceTime = System.currentTimeMillis();
        this.lastRaidInsuranceTime = System.currentTimeMillis();
        this.boughtClaims = 1;
        this.warDefencesWon = 0;
        this.warAttacksLost = 0;
        this.warDefencesLost = 0;
        this.warAttacksWon = 0;
        this.claimsLostToWar = 0;
        this.claimsGainedFromWar = 0;
        this.raidsWon = 0;
        this.raidsLost = 0;
    }

    public boolean isWarInsured() {
        long currentTime = System.currentTimeMillis();
        long insuranceDurationMillis = OPAPCConfig.warInsuranceDurationDays * 24L * 60 * 60 * 1000;
        return currentTime - lastWarInsuranceTime <= insuranceDurationMillis;
    }

    public void renewWarInsurance() {
        this.lastWarInsuranceTime = System.currentTimeMillis();
    }

    public boolean isRaidInsured() {
        long currentTime = System.currentTimeMillis();
        long insuranceDurationMillis = OPAPCConfig.raidInsuranceDurationDays * 24L * 60 * 60 * 1000;
        return currentTime - lastRaidInsuranceTime <= insuranceDurationMillis;
    }

    public void renewRaidInsurance() {
        this.lastRaidInsuranceTime = System.currentTimeMillis();
    }

    public String getPartyName() {
        return OPAPC.getPlayerConfigs()
                .getLoadedConfig(OPAPC.getPartyManager()
                        .getPartyById(partyId)
                        .getOwner()
                        .getUUID())
                .getFromEffectiveConfig(PlayerConfigOptions.PARTY_NAME);
    }
    public List<ChunkPos> getClaimedChunksList() {
        List<ChunkPos> chunkClaims = new ArrayList<>();
        OPAPC.getClaimsManager().getPlayerInfo(OPAPC.getPartyManager()
                        .getPartyById(partyId).getOwner().getUUID())
                        .getDimension(Level.OVERWORLD.location())
                        .getStream().forEach(e -> e.getStream()
                        .forEach(chunkClaims::add));
        return chunkClaims;
    }

    public void setBoughtClaims(int boughtClaims) {
        this.boughtClaims = boughtClaims;
    }

    public int getBoughtClaims() {
        return boughtClaims;
    }

    public Map<UUID, Donor> getDonations() {
        return donations;
    }

    public void addDonation(UUID playerId, long value) {
        Donor existing = donations.get(playerId);
        long total = value + (existing != null ? existing.amount() : 0);
        donations.put(playerId, new Donor(playerId, total));
    }

    public BlockPos getWarpPos() {
        return warpPos;
    }

    public void setWarpPos(BlockPos warpPos) {
        this.warpPos = warpPos;
    }

    public UUID getPartyId() {
        return partyId;
    }

    public boolean chunkContainsWarpPos(ChunkPos chunk) {
        return new ChunkPos(warpPos).equals(chunk);
    }

    // --- Stats Getters, Setters, and Incrementers ---

    public int getWarDefencesWon() {
        return warDefencesWon;
    }

    public void setWarDefencesWon(int warDefencesWon) {
        this.warDefencesWon = warDefencesWon;
    }

    public void incrementWarDefencesWon() {
        this.warDefencesWon++;
    }

    public int getWarDefencesLost() {
        return warDefencesLost;
    }

    public void setWarDefencesLost(int warDefencesLost) {
        this.warDefencesLost = warDefencesLost;
    }

    public void incrementWarDefencesLost() {
        this.warDefencesLost++;
    }

    public int getWarAttacksLost() {
        return warAttacksLost;
    }

    public void setWarAttacksLost(int warAttacksLost) {
        this.warAttacksLost = warAttacksLost;
    }

    public void incrementWarAttacksLost() {
        this.warAttacksLost++;
    }

    public int getWarAttacksWon() {
        return warAttacksWon;
    }

    public void setWarAttacksWon(int warAttacksWon) {
        this.warAttacksWon = warAttacksWon;
    }

    public void incrementWarAttacksWon() {
        this.warAttacksWon++;
    }

    public int getClaimsLostToWar() {
        return claimsLostToWar;
    }

    public void setClaimsLostToWar(int claimsLostToWar) {
        this.claimsLostToWar = claimsLostToWar;
    }

    public void incrementClaimsLostToWar() {
        this.claimsLostToWar++;
    }

    public int getClaimsGainedFromWar() {
        return claimsGainedFromWar;
    }

    public void setClaimsGainedFromWar(int claimsGainedFromWar) {
        this.claimsGainedFromWar = claimsGainedFromWar;
    }

    public void incrementClaimsGainedFromWar() {
        this.claimsGainedFromWar++;
    }

    // === NBT SERIALIZATION ===
    public CompoundTag writeToNbt() {
        CompoundTag nbt = new CompoundTag();

        // basic fields
        nbt.putUUID("PartyId", partyId);
        nbt.putInt("BoughtClaims", boughtClaims);
        nbt.putLong("LastWarInsuranceTime", lastWarInsuranceTime);
        nbt.putLong("LastRaidInsuranceTime", lastRaidInsuranceTime);

        // stats
        nbt.putInt("WarDefencesWon", warDefencesWon);
        nbt.putInt("WarDefencesLost", warDefencesLost);
        nbt.putInt("WarAttacksLost", warAttacksLost);
        nbt.putInt("WarAttacksWon", warAttacksWon);
        nbt.putInt("ClaimsLostToWar", claimsLostToWar);
        nbt.putInt("ClaimsGainedFromWar", claimsGainedFromWar);

        // teleport position (if set)
        if (warpPos != null) {
            nbt.put("WarpPos", NbtUtils.writeBlockPos(warpPos));
        }

        // donations list
        ListTag donorList = new ListTag();
        for (Donor donor : donations.values()) {
            donorList.add(donor.toNbt());
        }
        nbt.put("Donations", donorList);

        return nbt;
    }

    public void readFromNbt(CompoundTag nbt) {
        // overwrite fields
        this.partyId = nbt.getUUID("PartyId");
        this.boughtClaims = nbt.getInt("BoughtClaims");
        this.lastWarInsuranceTime = nbt.getLong("LastWarInsuranceTime");
        this.lastRaidInsuranceTime = nbt.contains("LastRaidInsuranceTime")
                ? nbt.getLong("LastRaidInsuranceTime")
                : System.currentTimeMillis();

        // stats (with default fallback if missing)
        this.warDefencesWon = nbt.getInt("DefencesWon");
        this.warDefencesLost = nbt.getInt("DefencesLost");
        this.warAttacksLost = nbt.getInt("AttacksLost");
        this.warAttacksWon = nbt.getInt("AttacksWon");
        this.claimsLostToWar = nbt.getInt("ClaimsLostToWar");
        this.claimsGainedFromWar = nbt.getInt("ClaimsGainedFromWar");

        // teleport
        if (nbt.contains("WarpPos", Tag.TAG_COMPOUND)) {
            this.warpPos = NbtUtils.readBlockPos(nbt.getCompound("WarpPos"));
        } else {
            this.warpPos = null;
        }

        // donations
        this.donations.clear();
        ListTag donorList = nbt.getList("Donations", Tag.TAG_COMPOUND);
        for (Tag element : donorList) {
            Donor donor = Donor.fromNbt((CompoundTag) element);
            this.donations.put(donor.playerId(), donor);
        }
    }
}
