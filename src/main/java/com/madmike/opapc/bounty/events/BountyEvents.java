package com.madmike.opapc.bounty.events;

import com.glisco.numismaticoverhaul.ModComponents;
import com.madmike.opapc.OPAPCComponents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;

public class BountyEvents {
    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(((entity, damageSource) -> {
            if (entity instanceof ServerPlayer player) {
                if (damageSource.getEntity() instanceof ServerPlayer killer) {
                    if (OPAPCComponents.BOUNTY.get(player).getBounty() > 0) {
                        ModComponents.CURRENCY.get(killer).modify(OPAPCComponents.BOUNTY.get(player).getBounty());
                        OPAPCComponents.BOUNTY.get(player).setBounty(0);
                    }
                    else {
                        OPAPCComponents.BOUNTY.get(killer).setBounty(OPAPCComponents.BOUNTY.get(killer).getBounty() + );
                    }
                }
            }

        }));
    }
}
