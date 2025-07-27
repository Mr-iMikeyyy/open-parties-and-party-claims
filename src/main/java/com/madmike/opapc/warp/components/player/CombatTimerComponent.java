package com.madmike.opapc.warp.components.player;

import com.madmike.opapc.OPAPCConfig;
import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class CombatTimerComponent implements Component {
    private long lastDamageTime = 0;
    private final Player player;

    public CombatTimerComponent(Player player) {
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

    public long getRemainingTimeMs() {
        long durationMs = OPAPCConfig.combatDurationSeconds * 1000L;
        long remaining = durationMs - (System.currentTimeMillis() - lastDamageTime);
        return Math.max(remaining, 0);
    }

    public int getRemainingTimeSeconds() {
        long remainingMs = getRemainingTimeMs();
        return (int) Math.ceil(remainingMs / 1000.0);
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        this.lastDamageTime = tag.getLong("LastDamageTime");
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.putLong("LastDamageTime", this.lastDamageTime);
    }
}
