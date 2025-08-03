/*
 * Copyright (C) 2025 Mr-iMikeyyy (and contributors)
 *
 * This file is part of OPAPC (Open Parties and Party Claims).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of version 3 of the GNU Lesser General Public License
 * (LGPL-3.0-only) as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.madmike.opapc.partyclaim.data;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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

    public void addDonation(UUID playerId, String name, long value) {
        Donor existing = donations.get(playerId);
        long total = value + (existing != null ? existing.amount() : 0);
        donations.put(playerId, new Donor(name, total));
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
        for (Map.Entry<UUID, Donor> entry : donations.entrySet()) {
            donorList.add(entry.getValue().toNbt(entry.getKey()));
        }
        nbt.put("Donations", donorList);

        return nbt;
    }

    public void readFromNbt(CompoundTag nbt) {
        // overwrite fields
        this.partyId = nbt.getUUID("PartyId");
        this.boughtClaims = nbt.getInt("BoughtClaims");
        this.lastWarInsuranceTime = nbt.getLong("LastWarInsuranceTime");
        this.lastRaidInsuranceTime = nbt.getLong("LastRaidInsuranceTime");

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

        //donations
        this.donations.clear();
        ListTag donorList = nbt.getList("Donations", Tag.TAG_COMPOUND);

        for (Tag element : donorList) {
            CompoundTag donorTag = (CompoundTag) element;
            UUID playerId = donorTag.getUUID("PlayerId");
            Donor donor = Donor.fromNbt(donorTag);
            donations.put(playerId, donor);
        }
    }

    public Component getInfo() {
        MutableComponent info = Component.literal("§6--- Party Claim Info ---\n");

        // Party name & ID
        info.append(Component.literal("§eParty: §f" + getPartyName() + "\n"));
        info.append(Component.literal("§eParty ID: §7" + partyId.toString() + "\n"));

        // Claims
        info.append(Component.literal("§eClaims Bought: §f" + boughtClaims + "\n"));
        info.append(Component.literal("§eChunks Claimed: §f" + getClaimedChunksList().size() + "\n"));

        // Warp
        if (warpPos != null) {
            info.append(Component.literal("§eWarp Position: §f" + warpPos.getX() + ", " + warpPos.getY() + ", " + warpPos.getZ() + "\n"));
        } else {
            info.append(Component.literal("§eWarp Position: §7Not Set\n"));
        }

        // Insurance
        info.append(Component.literal("§eWar Insurance: " + (isWarInsured() ? "§aActive" : "§cExpired") + "\n"));
        info.append(Component.literal("§eRaid Insurance: " + (isRaidInsured() ? "§aActive" : "§cExpired") + "\n"));

        // War Stats
        info.append(Component.literal("\n§6--- War Stats ---\n"));
        info.append(Component.literal("§eDefences Won: §f" + warDefencesWon + "\n"));
        info.append(Component.literal("§eDefences Lost: §f" + warDefencesLost + "\n"));
        info.append(Component.literal("§eAttacks Won: §f" + warAttacksWon + "\n"));
        info.append(Component.literal("§eAttacks Lost: §f" + warAttacksLost + "\n"));
        info.append(Component.literal("§eClaims Gained: §f" + claimsGainedFromWar + "\n"));
        info.append(Component.literal("§eClaims Lost: §f" + claimsLostToWar + "\n"));

        // Raid Stats
        info.append(Component.literal("\n§6--- Raid Stats ---\n"));
        info.append(Component.literal("§eRaids Won: §f" + raidsWon + "\n"));
        info.append(Component.literal("§eRaids Lost: §f" + raidsLost + "\n"));

        for (Map.Entry<UUID, Donor> entry : donations.entrySet()) {
            UUID donorId = entry.getKey();
            Donor donor = entry.getValue();

            String displayName = OPAPC.getServer().getPlayerList().getPlayer(donorId) != null
                    ? donor.name()  // use saved name if player is known
                    : donorId.toString();

            info.append(Component.literal("§e" + displayName + ": §f" + donor.amount() + "\n"));
        }

        return info;
    }
}
