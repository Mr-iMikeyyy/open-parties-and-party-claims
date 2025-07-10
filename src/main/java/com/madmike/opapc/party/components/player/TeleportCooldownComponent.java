package com.madmike.opapc.party.components.player;

import com.madmike.opapc.config.OPAPCConfig;
import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.time.Duration;

public class TeleportCooldownComponent implements Component {
    private long lastTeleportTime = 0;
    private final Player player;

    public TeleportCooldownComponent(Player player) {
        this.player = player;
    }

    /** Call this when the player teleports */
    public void onTele() {
        lastTeleportTime = System.currentTimeMillis();
    }

    /** Check if player still has a teleport cooldown */
    public boolean hasCooldown() {
        long durationMs = OPAPCConfig.teleportCooldownInSeconds * 1000L;
        return System.currentTimeMillis() - lastTeleportTime < durationMs;
    }

    /** Get remaining cooldown as Duration for minutes/seconds display */
    public Duration getRemainingTime() {
        long durationMs = OPAPCConfig.teleportCooldownInSeconds * 1000L;
        long remainingMs = durationMs - (System.currentTimeMillis() - lastTeleportTime);
        if (remainingMs < 0) remainingMs = 0;
        return Duration.ofMillis(remainingMs);
    }

    /** Get formatted minutes/seconds string for chat/overlay */
    public String getFormattedRemainingTime() {
        Duration remaining = getRemainingTime();
        long minutes = remaining.toMinutes();
        long seconds = remaining.minusMinutes(minutes).getSeconds();
        return String.format("%d min %d sec", minutes, seconds);
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        this.lastTeleportTime = tag.getLong("LastTeleportTime"); // ✅ Correct key
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        tag.putLong("LastTeleportTime", this.lastTeleportTime); // ✅ Correct key
    }
}