package com.madmike.opapc.components.scoreboard.parties;

import com.madmike.opapc.data.parties.claims.PartyClaim;
import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyClaimsComponent implements Component {
    private final Scoreboard scoreboard;
    private final MinecraftServer server;

    // Key: Party UUID
    private final Map<UUID, PartyClaim> partyClaims = new HashMap<>();

    public PartyClaimsComponent(Scoreboard scoreboard, MinecraftServer server) {
        this.scoreboard = scoreboard;
        this.server = server;
    }

    // 🔽 Deserialize from NBT
    @Override
    public void readFromNbt(NbtCompound nbt) {
        this.partyClaims.clear();

        NbtList claimsList = nbt.getList("PartyClaims", NbtElement.COMPOUND_TYPE);
        for (NbtElement element : claimsList) {
            NbtCompound claimNbt = (NbtCompound) element;
            UUID partyId = claimNbt.getUuid("PartyId");

            PartyClaim claim = new PartyClaim(partyId);
            claim.readFromNbt(claimNbt);

            this.partyClaims.put(partyId, claim);
        }
    }

    // 🔼 Serialize to NBT
    @Override
    public void writeToNbt(NbtCompound nbt) {
        NbtList claimsList = new NbtList();
        for (PartyClaim claim : partyClaims.values()) {
            NbtCompound claimNbt = claim.writeToNbt();
            claimNbt.putUuid("PartyId", claim.getPartyId()); // Ensure the ID is saved
            claimsList.add(claimNbt);
        }
        nbt.put("PartyClaims", claimsList);
    }

    // 🔍 Create claim by party ID (creates if missing)
    public PartyClaim createClaim(UUID partyId, BlockPos blockPos) {
        return partyClaims.computeIfAbsent(partyId, PartyClaim::new);
    }

    // ❓ Lookup claim without creating
    public PartyClaim getClaim(UUID partyId) {
        return partyClaims.get(partyId);
    }

    // 📊 Getters for infrastructure
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
        claim.deletePcbBlock(server.getOverworld());
        partyClaims.remove(partyId);
    }
}
