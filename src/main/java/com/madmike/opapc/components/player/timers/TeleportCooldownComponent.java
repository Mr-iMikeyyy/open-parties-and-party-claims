package com.madmike.opapc.components.player.timers;

import com.madmike.opapc.config.OPAPCConfig;
import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

public class TeleportCooldownComponent implements Component {
    private long lastTeleportTime = 0;
    ServerPlayerEntity player;

    public TeleportCooldownComponent(ServerPlayerEntity player) {
        this.player = player;
    }

    /** Call this when the player teleports */
    public void onDamaged() {
        lastTeleportTime = System.currentTimeMillis();
    }

    /** Call this when you want to check if the player is still in combat */
    public boolean hasCooldown() {
        long durationMs = OPAPCConfig.teleportCooldownInSeconds * 1000L;
        return System.currentTimeMillis() - lastTeleportTime < durationMs;
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        this.lastTeleportTime = tag.getLong("LastDamageTime");
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        tag.putLong("LastDamageTime", this.lastTeleportTime);
    }
}