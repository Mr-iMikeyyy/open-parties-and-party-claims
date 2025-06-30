package com.madmike.opapc.components.player.trades;

import com.madmike.opapc.components.OPAPCComponents;
import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class UnlockedStoreSlotsComponent implements Component {

    private final Player player;
    private int unlockedSlots = 5;

    public UnlockedStoreSlotsComponent(Player player) {
        this.player = player;
    }

    public int getUnlockedSlots() {
        return unlockedSlots;
    }

    public void setUnlockedSlots(int slots) {
        this.unlockedSlots = slots;
        OPAPCComponents.UNLOCKED_STORE_SLOTS.sync(player);
    }

    public void increment(int amount) {
        this.unlockedSlots += amount;
        OPAPCComponents.UNLOCKED_STORE_SLOTS.sync(player);
    }

    public void reset() {
        this.unlockedSlots = 5;
        OPAPCComponents.UNLOCKED_STORE_SLOTS.sync(player);
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        this.unlockedSlots = tag.getInt("UnlockedSlots");
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.putInt("UnlockedSlots", this.unlockedSlots);
    }

}
