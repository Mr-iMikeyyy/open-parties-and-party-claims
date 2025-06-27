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

    public void onTele() {
        lastTeleportTime = System.currentTimeMillis();
    }

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