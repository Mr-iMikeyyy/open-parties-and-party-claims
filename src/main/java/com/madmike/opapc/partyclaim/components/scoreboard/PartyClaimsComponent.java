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

package com.madmike.opapc.partyclaim.components.scoreboard;

import com.madmike.opapc.partyclaim.data.PartyClaim;
import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PartyClaimsComponent implements ComponentV3 {
    private final Scoreboard provider;
    private final MinecraftServer server;
    private final Map<UUID, PartyClaim> partyClaims = new HashMap<>();

    public PartyClaimsComponent(Scoreboard scoreboard, MinecraftServer server) {
        this.server = server;
        this.provider = scoreboard;
    }

    @Override
    public void readFromNbt(CompoundTag nbt) {
        partyClaims.clear();
        ListTag claimsList = nbt.getList("PartyClaims", Tag.TAG_COMPOUND);

        for (int i = 0; i < claimsList.size(); i++) {
            CompoundTag claimTag = claimsList.getCompound(i);
            if (!claimTag.hasUUID("PartyId")) continue;

            PartyClaim claim = PartyClaim.fromNbt(claimTag, server);
            claim.normalize(server); // guarantees non-null warpPos etc.
            partyClaims.put(claim.getPartyId(), claim);
        }
    }

    @Override
    public void writeToNbt(@NotNull CompoundTag nbt) {
        ListTag claimsList = new ListTag();
        for (PartyClaim claim : partyClaims.values()) {
            claim.normalize(server);
            claimsList.add(claim.writeToNbt());
        }
        nbt.put("PartyClaims", claimsList);
    }

    /** Create a claim, using the leader’s current position as the warp. */
    public void createClaim(UUID partyId, BlockPos leadersCurrentPos) {
        partyClaims.putIfAbsent(partyId, new PartyClaim(partyId, leadersCurrentPos));
    }

    public PartyClaim getClaim(UUID partyId) { return partyClaims.get(partyId); }
    public Collection<PartyClaim> getAllClaims() { return Collections.unmodifiableCollection(partyClaims.values()); }
    public void removeClaim(UUID partyId) { partyClaims.remove(partyId); }
}
