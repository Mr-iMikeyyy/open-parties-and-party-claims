package com.madmike.opapc.duel.events;

import com.madmike.opapc.raid.RaidManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;

public class DuelEvents {
    public static void register() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, amount) -> {

            if (entity instanceof ServerPlayer player) {
                if (DuelManager.INSTANCE.playerIsInDuel(player.getUUID())) {
                    DuelManager.INSTANCE.onDuelerDeath(player);
                    return false;
                }
            }

            return true;

        });
    }
}
