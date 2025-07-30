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
                for (WarData war : WarManager.INSTANCE.getActiveWars()) {
                    if (!war.getAttackingPlayers().contains(player)) {
                        WarManager.INSTANCE.onAttackerDeath(player, war);
                        return false;
                    }
                }
                for (WarData war : WarManager.INSTANCE.getActiveWars()) {
                    if (!war.getDefendingPlayers().contains(player)) {
                        WarManager.INSTANCE.onDefenderDeath(player, war);
                        return false;
                    }
                }
            }

            // Allows death
            return true;
        });

        //Tick the war
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            WarManager.INSTANCE.tick();
        });

        // If player's party is in war on join, then kick if not part of the war
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            UUID uuid = player.getUUID();

            var party = OPAPC.getPartyManager().getPartyByMember(uuid);
            if (party == null) return;

            for (WarData war : WarManager.INSTANCE.getActiveWars()) {
                if (war.getDefendingParty().equals(party) || war.getAttackingParty().equals(party)) {
                    if (!war.getAttackingPlayers().contains(player) || !war.getDefendingPlayers().contains(player)) {
                        player.connection.disconnect(Component.literal("Your party is currently engaged in a war. You cannot join right now."));
                    }
                }
            }
        });
    }
}
