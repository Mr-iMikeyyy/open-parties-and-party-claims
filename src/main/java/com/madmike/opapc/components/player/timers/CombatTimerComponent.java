package com.madmike.opapc.components.player.timers;

import com.madmike.opapc.config.OPAPCConfig;
import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

public class CombatTimerComponent implements Component {
    private long lastDamageTime = 0;
    ServerPlayerEntity player;

    public CombatTimerComponent(ServerPlayerEntity player) {
        this.player = player;
    }

    /** Call this when the player is damaged */
    public void onDamaged() {
        lastDamageTime = System.currentTimeMillis();
    }

    /** Call this when you want to check if the player is still in combat */
    public boolean isInCombat() {
        long durationMs = OPAPCConfig.combatDurationSeconds * 1000L;
        return System.currentTimeMillis() - lastDamageTime < durationMs;
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        this.lastDamageTime = tag.getLong("LastDamageTime");
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        tag.putLong("LastDamageTime", this.lastDamageTime);
    }
}
