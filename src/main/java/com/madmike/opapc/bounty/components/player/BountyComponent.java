package com.madmike.opapc.bounty.components.player;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class BountyComponent implements ComponentV3 {

    private Player provider;

    private int bounty;

    public BountyComponent(Player player) {
        this.provider = player;
    }




    @Override
    public void readFromNbt(CompoundTag tag) {

    }

    @Override
    public void writeToNbt(CompoundTag tag) {

    }
}
