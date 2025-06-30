package com.madmike.opapc.components.scoreboard.parties;

import com.madmike.opapc.data.parties.claims.PartyClaim;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyClaimsComponent implements dev.onyxstudios.cca.api.v3.component.Component {
    private final Scoreboard scoreboard;
    private final MinecraftServer server;
    private final Map<UUID, PartyClaim> partyClaims = new HashMap<>();

    public PartyClaimsComponent(Scoreboard scoreboard, MinecraftServer server) {
        this.scoreboard = scoreboard;
        this.server = server;
    }

    @Override
    public void readFromNbt(CompoundTag nbt) {
        this.partyClaims.clear();

        ListTag claimsList = nbt.getList("PartyClaims", Tag.TAG_COMPOUND);
        for (Tag element : claimsList) {
            CompoundTag claimTag = (CompoundTag) element;
            UUID partyId = claimTag.getUUID("PartyId");

            PartyClaim claim = new PartyClaim(partyId);
            claim.readFromNbt(claimTag);

            this.partyClaims.put(partyId, claim);
        }
    }

    @Override
    public void writeToNbt(CompoundTag nbt) {
        ListTag claimsList = new ListTag();
        for (PartyClaim claim : partyClaims.values()) {
            CompoundTag claimTag = claim.writeToNbt();
            claimTag.putUUID("PartyId", claim.getPartyId());
            claimsList.add(claimTag);
        }
        nbt.put("PartyClaims", claimsList);
    }

    public PartyClaim createClaim(UUID partyId) {
        return partyClaims.computeIfAbsent(partyId, PartyClaim::new);
    }

    public PartyClaim getClaim(UUID partyId) {
        return partyClaims.get(partyId);
    }

    public Map<UUID, PartyClaim> getAllClaims() {
        return partyClaims;
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public void removeClaim(UUID partyId) {
        PartyClaim claim = partyClaims.get(partyId);
        if (claim != null) {
            claim.deletePcbBlock(server);
            partyClaims.remove(partyId);
        }
    }
}
