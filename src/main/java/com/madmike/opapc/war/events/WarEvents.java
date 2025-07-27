package com.madmike.opapc.war.events;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.war.WarManager;
import com.madmike.opapc.war.data.WarData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class WarEvents {
    public static void register() {
        //Cancel death and inform WarManager if player died while in war
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, amount) -> {

            //Checks if entity that died is a player
            if (entity instanceof ServerPlayer player) {
                //Checks if player is in war
                WarData war = WarManager.INSTANCE.playerIsInWar(player.getUUID());
                if (war != null) {
                    WarManager.INSTANCE.onPlayerDeath(player, war);
                    return false;
                }
            }

            // Allows death
            return true;
        });

        //Tick the war
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            WarManager.INSTANCE.tick();
        });

        // If player's party is in war on join, then kick
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            UUID uuid = player.getUUID();

            var party = OPAPC.getPartyManager().getPartyByMember(uuid);
            if (party == null) return;

            WarData war = WarManager.INSTANCE.playerIsInWar(player.getUUID());

            if (war != null) {
                player.connection.disconnect(Component.literal("Your party is currently engaged in a war. You cannot join right now."));
            }
        });
    }
}
