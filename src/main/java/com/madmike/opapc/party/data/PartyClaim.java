package com.madmike.opapc.party.data;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.config.OPAPCConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import xaero.pac.common.server.player.config.api.PlayerConfigOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyClaim {

    private final Map<UUID, Donor> donations = new HashMap<>();
    private UUID partyId;
    private int boughtClaims = 1;
    private BlockPos teleportPos = null;
    private long lastInsuranceTime;

    public PartyClaim(UUID partyId) {
        this.partyId = partyId;
        this.lastInsuranceTime = System.currentTimeMillis();
    }

    public boolean isInsured() {
        long currentTime = System.currentTimeMillis();
        long insuranceDurationMillis = OPAPCConfig.insuranceDurationDays * 24L * 60 * 60 * 1000;
        return currentTime - lastInsuranceTime <= insuranceDurationMillis;
    }

    public void renewInsurance() {
        this.lastInsuranceTime = System.currentTimeMillis();
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

    public Map<UUID, Donor> getDonations() {
        return donations;
    }

    public int getBoughtClaims() {
        return boughtClaims;
    }

    public BlockPos getTeleportPos() {
        return teleportPos;
    }

    public void setTeleportPos(BlockPos teleportPos) {
        this.teleportPos = teleportPos;
    }

    public UUID getPartyId() {
        return partyId;
    }

    public void addDonation(UUID playerId, String name, long value) {
        Donor existing = donations.get(playerId);
        long total = value + (existing != null ? existing.amount() : 0);
        donations.put(playerId, new Donor(playerId, name, total));
    }

    // === NBT SERIALIZATION ===

    public CompoundTag writeToNbt() {
        CompoundTag nbt = new CompoundTag();

        // basic fields
        nbt.putUUID("PartyId", partyId);
        nbt.putInt("BoughtClaims", boughtClaims);
        nbt.putLong("LastInsuranceTime", lastInsuranceTime);

        // teleport position (if set)
        if (teleportPos != null) {
            nbt.put("TeleportPos", NbtUtils.writeBlockPos(teleportPos));
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
        this.lastInsuranceTime = nbt.getLong("LastInsuranceTime");

        // teleport
        if (nbt.contains("TeleportPos", Tag.TAG_COMPOUND)) {
            this.teleportPos = NbtUtils.readBlockPos(nbt.getCompound("TeleportPos"));
        } else {
            this.teleportPos = null;
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
