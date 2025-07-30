package com.madmike.opapc.partyclaim.components.scoreboard;

import com.madmike.opapc.partyclaim.data.PartyClaim;
import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PartyClaimsComponent implements ComponentV3 {
    private final Scoreboard provider;
    private final List<PartyClaim> partyClaims = new ArrayList<>();
    private final MinecraftServer server;

    public PartyClaimsComponent(Scoreboard scoreboard, MinecraftServer server) {
        this.server = server;
        this.provider = scoreboard;
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

            this.partyClaims.add(claim);
        }
    }

    @Override
    public void writeToNbt(@NotNull CompoundTag nbt) {
        ListTag claimsList = new ListTag();
        for (PartyClaim claim : partyClaims) {
            CompoundTag claimTag = claim.writeToNbt();
            claimTag.putUUID("PartyId", claim.getPartyId());
            claimsList.add(claimTag);
        }
        nbt.put("PartyClaims", claimsList);
    }

    public void createClaim(UUID partyId) {
        partyClaims.add(new PartyClaim(partyId));
    }

    public PartyClaim getClaim(UUID partyId) {
        for (PartyClaim claim : partyClaims) {
            if (claim.getPartyId().equals(partyId)) {
                return claim;
            }
        }
        return null;
    }

    public List<PartyClaim> getAllClaims() {
        return partyClaims;
    }

    public void removeClaim(UUID partyId) {
        partyClaims.removeIf(claim -> claim.getPartyId().equals(partyId));
    }
}
