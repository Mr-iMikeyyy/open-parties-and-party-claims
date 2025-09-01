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
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import xaero.pac.common.server.player.config.api.PlayerConfigOptions;

import java.util.*;

public class PartyClaim {

    private final Map<UUID, Donor> donations = new HashMap<>();
    private UUID partyId;
    private int boughtClaims;
    private BlockPos warpPos; // never null after normalize()
    private long lastWarInsuranceTime;
    private int warDefencesWon, warDefencesLost, warAttacksLost, warAttacksWon;
    private int claimsLostToWar, claimsGainedFromWar;

    /* ------------ construction / invariants ------------ */

    public PartyClaim(UUID partyId, BlockPos warpPos) {
        this.partyId = java.util.Objects.requireNonNull(partyId, "partyId");
        this.warpPos  = java.util.Objects.requireNonNull(warpPos, "warpPos");
        this.boughtClaims = 1;
    }

    /** Ensure invariants after loads or before writes. */
    public void normalize(MinecraftServer server) {
        if (this.warpPos == null) {
            this.warpPos = resolveFallbackWarpInsideClaim(this.partyId, server);
        }
        if (this.boughtClaims <= 0) this.boughtClaims = 1;
        if (this.partyId == null) throw new IllegalStateException("PartyClaim missing partyId");
    }

    /* ------------ live logic ------------ */

    public boolean isWarInsured() {
        long insuranceDurationMillis = OPAPCConfig.warInsuranceDurationDays * 24L * 60 * 60 * 1000L;
        return System.currentTimeMillis() - lastWarInsuranceTime <= insuranceDurationMillis;
    }

    public void renewWarInsurance() { this.lastWarInsuranceTime = System.currentTimeMillis(); }

    public String getPartyName() {
        return OPAPC.playerConfigs()
                .getLoadedConfig(OPAPC.parties().getPartyById(partyId).getOwner().getUUID())
                .getFromEffectiveConfig(PlayerConfigOptions.PARTY_NAME);
    }

    public List<ChunkPos> getClaimedChunksList() {
        List<ChunkPos> chunkClaims = new ArrayList<>();
        OPAPC.claims().getPlayerInfo(OPAPC.parties().getPartyById(partyId).getOwner().getUUID()).getDimension(Level.OVERWORLD.location()).getStream().forEach(list -> list.getStream().forEach(chunkClaims::add));
        return chunkClaims;
    }

    /* ------------ getters/setters ------------ */

    public void setBoughtClaims(int boughtClaims) { this.boughtClaims = boughtClaims; }
    public int getBoughtClaims() { return boughtClaims; }

    public Map<UUID, Donor> getDonations() { return donations; }
    public void addDonation(UUID playerId, String name, long value) {
        Donor existing = donations.get(playerId);
        long total = value + (existing != null ? existing.amount() : 0);
        donations.put(playerId, new Donor(name, total));
    }

    public BlockPos getWarpPos() { return warpPos; }

    /** never null */
    public void setWarpPos(BlockPos warpPos) {
        this.warpPos = java.util.Objects.requireNonNull(warpPos, "warpPos");
    }

    public UUID getPartyId() { return partyId; }

    public boolean chunkContainsWarpPos(ChunkPos chunk) {
        return new ChunkPos(warpPos).equals(chunk);
    }

    // stats ...
    public int getWarDefencesWon() { return warDefencesWon; }
    public void setWarDefencesWon(int v) { warDefencesWon = v; }
    public void incrementWarDefencesWon() { warDefencesWon++; }
    public int getWarDefencesLost() { return warDefencesLost; }
    public void setWarDefencesLost(int v) { warDefencesLost = v; }
    public void incrementWarDefencesLost() { warDefencesLost++; }
    public int getWarAttacksLost() { return warAttacksLost; }
    public void setWarAttacksLost(int v) { warAttacksLost = v; }
    public void incrementWarAttacksLost() { warAttacksLost++; }
    public int getWarAttacksWon() { return warAttacksWon; }
    public void setWarAttacksWon(int v) { warAttacksWon = v; }
    public void incrementWarAttacksWon() { warAttacksWon++; }
    public int getClaimsLostToWar() { return claimsLostToWar; }
    public void setClaimsLostToWar(int v) { claimsLostToWar = v; }
    public void incrementClaimsLostToWar() { claimsLostToWar++; }
    public int getClaimsGainedFromWar() { return claimsGainedFromWar; }
    public void setClaimsGainedFromWar(int v) { claimsGainedFromWar = v; }
    public void incrementClaimsGainedFromWar() { claimsGainedFromWar++; }

    /* ------------ NBT I/O ------------ */

    public CompoundTag writeToNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("PartyId", partyId);
        nbt.putInt("BoughtClaims", boughtClaims);
        nbt.putLong("LastWarInsuranceTime", lastWarInsuranceTime);

        nbt.putInt("WarDefencesWon", warDefencesWon);
        nbt.putInt("WarDefencesLost", warDefencesLost);
        nbt.putInt("WarAttacksLost", warAttacksLost);
        nbt.putInt("WarAttacksWon", warAttacksWon);
        nbt.putInt("ClaimsLostToWar", claimsLostToWar);
        nbt.putInt("ClaimsGainedFromWar", claimsGainedFromWar);

        nbt.put("WarpPos", NbtUtils.writeBlockPos(warpPos));

        ListTag donorList = new ListTag();
        for (Map.Entry<UUID, Donor> e : donations.entrySet()) {
            donorList.add(e.getValue().toNbt(e.getKey()));
        }
        nbt.put("Donations", donorList);
        return nbt;
    }

    public static PartyClaim fromNbt(CompoundTag nbt, MinecraftServer server) {
        UUID partyId = nbt.getUUID("PartyId");

        BlockPos warp;
        if (nbt.contains("WarpPos", Tag.TAG_COMPOUND)) {
            warp = NbtUtils.readBlockPos(nbt.getCompound("WarpPos"));
        } else {
            // old saves: pick something *inside* the claim
            warp = resolveFallbackWarpInsideClaim(partyId, server);
        }

        PartyClaim claim = new PartyClaim(partyId, warp);
        claim.boughtClaims = nbt.getInt("BoughtClaims");
        claim.lastWarInsuranceTime = nbt.getLong("LastWarInsuranceTime");

        // keys match write
        claim.warDefencesWon = nbt.getInt("WarDefencesWon");
        claim.warDefencesLost = nbt.getInt("WarDefencesLost");
        claim.warAttacksLost = nbt.getInt("WarAttacksLost");
        claim.warAttacksWon = nbt.getInt("WarAttacksWon");
        claim.claimsLostToWar = nbt.getInt("ClaimsLostToWar");
        claim.claimsGainedFromWar = nbt.getInt("ClaimsGainedFromWar");

        // donations
        claim.donations.clear();
        ListTag donorList = nbt.getList("Donations", Tag.TAG_COMPOUND);
        for (int i = 0; i < donorList.size(); i++) {
            CompoundTag donorTag = donorList.getCompound(i);
            UUID playerId = donorTag.getUUID("PlayerId");
            Donor donor = Donor.fromNbt(donorTag);
            claim.donations.put(playerId, donor);
        }

        claim.normalize(server);
        return claim;
    }

    /* ------------ UI ------------ */

    public net.minecraft.network.chat.Component getInfo() {
        var info = net.minecraft.network.chat.Component.literal("§6--- Party Claim Info ---\n")
                .append(net.minecraft.network.chat.Component.literal("§eParty: §f" + getPartyName() + "\n"))
                .append(net.minecraft.network.chat.Component.literal("§eParty ID: §7" + partyId + "\n"))
                .append(net.minecraft.network.chat.Component.literal("§eClaims Bought: §f" + boughtClaims + "\n"))
                .append(net.minecraft.network.chat.Component.literal("§eChunks Claimed: §f" + getClaimedChunksList().size() + "\n"))
                .append(net.minecraft.network.chat.Component.literal("§eWarp Position: §f" + warpPos.getX() + ", " + warpPos.getY() + ", " + warpPos.getZ() + "\n"))
                .append(net.minecraft.network.chat.Component.literal("§eWar Insurance: " + (isWarInsured() ? "§aActive" : "§cExpired") + "\n"))
                .append(net.minecraft.network.chat.Component.literal("\n§6--- War Stats ---\n"))
                .append(net.minecraft.network.chat.Component.literal("§eDefences Won: §f" + warDefencesWon + "\n"))
                .append(net.minecraft.network.chat.Component.literal("§eDefences Lost: §f" + warDefencesLost + "\n"))
                .append(net.minecraft.network.chat.Component.literal("§eAttacks Won: §f" + warAttacksWon + "\n"))
                .append(net.minecraft.network.chat.Component.literal("§eAttacks Lost: §f" + warAttacksLost + "\n"))
                .append(net.minecraft.network.chat.Component.literal("§eClaims Gained: §f" + claimsGainedFromWar + "\n"))
                .append(net.minecraft.network.chat.Component.literal("§eClaims Lost: §f" + claimsLostToWar + "\n"));

        for (var e : donations.entrySet()) {
            UUID donorId = e.getKey();
            Donor donor = e.getValue();
            String displayName = OPAPC.getServer().getPlayerList().getPlayer(donorId) != null ? donor.name() : donorId.toString();
            info = info.append(net.minecraft.network.chat.Component.literal("§e" + displayName + ": §f" + donor.amount() + "\n"));
        }
        return info;
    }
}
