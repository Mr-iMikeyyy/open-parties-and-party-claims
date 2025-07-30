package com.madmike.opapc.warp.events;

import com.madmike.opapc.OPAPCComponents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public class WarpEvents {
    public static void register() {
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                OPAPCComponents.COMBAT_COOLDOWN.get(serverPlayer).onDamaged();
            }
            return InteractionResult.PASS;
        });
    }
}
