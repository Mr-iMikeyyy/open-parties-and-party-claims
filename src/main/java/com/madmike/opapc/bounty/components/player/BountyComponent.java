package com.madmike.opapc.bounty.components.player;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public class BountyComponent implements ComponentV3 {

    private final ServerPlayer provider; // the player this component is attached to
    private long bounty;

    public BountyComponent(ServerPlayer player) {
        this.provider = player;
        this.bounty = 0;
    }

    // --- Accessors ---

    public long getBounty() {
        return bounty;
    }

    public boolean hasBounty() {
        return bounty > 0;
    }

    public void setBounty(long amount) {
        this.bounty = amount;
    }

    public void clearBounty() {
        this.bounty = 0;
    }

    // --- Persistence ---

    @Override
    public void readFromNbt(CompoundTag tag) {
        this.bounty = tag.getLong("Bounty");
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.putLong("Bounty", this.bounty);
    }
}
