package com.madmike.opapc.features.entity;

import com.madmike.opapc.features.OPAPCFeatures;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class PartyClaimBlockEntity extends BlockEntity {
    private UUID partyId;
    private final UUID blockId;

    public PartyClaimBlockEntity(BlockPos pos, BlockState state) {
        super(OPAPCFeatures.PARTY_CLAIM_BLOCK_ENTITY, pos, state);
        this.blockId = UUID.randomUUID();
    }

    public UUID getBlockId() {
        return blockId;
    }

    public UUID getPartyId() {
        return partyId;
    }

    public void setPartyId(UUID partyId) {
        this.partyId = partyId;
        markDirty(); // triggers a save
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.containsUuid("PartyId")) {
            partyId = nbt.getUuid("PartyId");
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (partyId != null) {
            nbt.putUuid("PartyId", partyId);
        }
    }
}
