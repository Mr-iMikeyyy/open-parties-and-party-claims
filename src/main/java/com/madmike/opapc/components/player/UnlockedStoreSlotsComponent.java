package com.madmike.opapc.components.player;

import com.madmike.opapc.components.OPAPCComponents;
import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

public class UnlockedStoreSlotsComponent implements Component, AutoSyncedComponent {

    private final PlayerEntity player;
    private int unlockedSlots = 5;

    public UnlockedStoreSlotsComponent(PlayerEntity player) {
        this.player = player;
    }

    public int getUnlockedSlots() {
        return unlockedSlots;
    }

    public void setUnlockedSlots(int slots) {
        this.unlockedSlots = slots;
        OPAPCComponents.UNLOCKED_SLOTS.sync(player);
    }

    public void increment(int amount) {
        this.unlockedSlots += amount;
        OPAPCComponents.UNLOCKED_SLOTS.sync(player);
    }

    public void reset() {
        this.unlockedSlots = 5;
       OPAPCComponents.UNLOCKED_SLOTS.sync(player);
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        this.unlockedSlots = tag.getInt("UnlockedSlots");
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        tag.putInt("UnlockedSlots", this.unlockedSlots);
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return player == this.player; // only sync with the provider itself
    }
}
