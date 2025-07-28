package com.madmike.opapc.partyclaim.data;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import xaero.pac.common.server.player.config.api.PlayerConfigOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyClaim {

    private final Map<UUID, Donor> donations = new HashMap<>();
    private UUID partyId;
    private int boughtClaims;
    private BlockPos warpPos = null;
    private long lastWarInsuranceTime;
    private long lastRaidInsuranceTime;
    private int defencesWon;
    private int defencesLost;
    private int attacksLost;
    private int attacksWon;
    private int claimsLostToWar;
    private int claimsGainedFromWar;

    public PartyClaim(UUID partyId) {
        this.partyId = partyId;
        this.lastWarInsuranceTime = System.currentTimeMillis();
        this.lastRaidInsuranceTime = System.currentTimeMillis();
        this.boughtClaims = 1;
        this.defencesWon = 0;
        this.defencesLost = 0;
        this.attacksLost = 0;
        this.attacksWon = 0;
        this.claimsLostToWar = 0;
        this.claimsGainedFromWar = 0;
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

    public void setBoughtClaims(int boughtClaims) {
        this.boughtClaims = boughtClaims;
    }

    public int getBoughtClaims() {
        return boughtClaims;
    }

    public Map<UUID, Donor> getDonations() {
        return donations;
    }

    public void addDonation(UUID playerId, String name, long value) {
        Donor existing = donations.get(playerId);
        long total = value + (existing != null ? existing.amount() : 0);
        donations.put(playerId, new Donor(playerId, name, total));
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

    public int getDefencesWon() {
        return defencesWon;
    }

    public void setDefencesWon(int defencesWon) {
        this.defencesWon = defencesWon;
    }

    public void incrementDefencesWon() {
        this.defencesWon++;
    }

    public int getDefencesLost() {
        return defencesLost;
    }

    public void setDefencesLost(int defencesLost) {
        this.defencesLost = defencesLost;
    }

    public void incrementDefencesLost() {
        this.defencesLost++;
    }

    public int getAttacksLost() {
        return attacksLost;
    }

    public void setAttacksLost(int attacksLost) {
        this.attacksLost = attacksLost;
    }

    public void incrementAttacksLost() {
        this.attacksLost++;
    }

    public int getAttacksWon() {
        return attacksWon;
    }

    public void setAttacksWon(int attacksWon) {
        this.attacksWon = attacksWon;
    }

    public void incrementAttacksWon() {
        this.attacksWon++;
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
        nbt.putInt("DefencesWon", defencesWon);
        nbt.putInt("DefencesLost", defencesLost);
        nbt.putInt("AttacksLost", attacksLost);
        nbt.putInt("AttacksWon", attacksWon);
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
        this.defencesWon = nbt.getInt("DefencesWon");
        this.defencesLost = nbt.getInt("DefencesLost");
        this.attacksLost = nbt.getInt("AttacksLost");
        this.attacksWon = nbt.getInt("AttacksWon");
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
