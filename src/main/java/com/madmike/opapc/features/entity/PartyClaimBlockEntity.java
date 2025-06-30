package com.madmike.opapc.features.entity;

import com.madmike.opapc.features.OPAPCFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class PartyClaimBlockEntity extends BlockEntity {

    private UUID partyId;

    public PartyClaimBlockEntity(BlockPos pos, BlockState state) {
        super(OPAPCFeatures.PARTY_CLAIM_BLOCK_ENTITY, pos, state);
    }

    public UUID getPartyId() {
        return partyId;
    }

    public void setPartyId(UUID partyId) {
        this.partyId = partyId;
        setChanged();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("PartyId")) {
            partyId = tag.getUUID("PartyId");
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (partyId != null) {
            tag.putUUID("PartyId", partyId);
        }
    }
}
