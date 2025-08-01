package com.madmike.opapc.war2.event;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCConfig;
import com.madmike.opapc.util.SafeWarpFinder;
import com.madmike.opapc.war2.War;
import com.madmike.opapc.war2.WarManager2;
import com.madmike.opapc.war2.data.WarData2;
import com.madmike.opapc.war2.event.bus.WarEventBus;
import com.madmike.opapc.war2.event.events.WarDeclaredEvent;
import com.madmike.opapc.war2.event.events.WarEndedEvent;
import com.madmike.opapc.war2.event.events.WarStartedEvent;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import xaero.pac.common.server.player.config.api.PlayerConfigOptions;

import java.util.List;
import java.util.UUID;

public class WarEvents {
    public static void register() {
        WarEventBus.register(event -> {

            if (event instanceof WarDeclaredEvent declared) {
                WarData2 data = declared.getWar().getData();
                if (OPAPCConfig.shouldBroadcastWarDeclarationsServerWide) {
                    OPAPC.broadcast(data.getInfo());
                }
                else {
                    data.broadcastToWar(data.getInfo());
                }
            }


            if (event instanceof WarStartedEvent started) {
                WarData2 data = started.getWar().getData();
                //broadcast message?
                data.broadcastToWar(Component.literal("The War Has Commenced!"));

                //Apply Buffs
                int attackerCount = data.getAttackingPlayers().size();
                int defenderCount = data.getDefendingPlayers().size();
                if (defenderCount == attackerCount) return;

                int amp = Math.abs(attackerCount - defenderCount);
                List<ServerPlayer> buffTargets = defenderCount < attackerCount ? data.getDefendingPlayers() : data.getAttackingPlayers();

                for (ServerPlayer player : buffTargets) {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, data.getDurationSeconds(), amp, true, true));
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, data.getDurationSeconds(), amp, true, true));
                }

            }


            if (event instanceof WarEndedEvent ended) {
                WarData2 data = ended.getWar().getData();

                // Restore protections
                OPAPC.getPlayerConfigs()
                        .getLoadedConfig(data.getDefendingParty().getOwner().getUUID())
                        .getUsedSubConfig().tryToSet(PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS, true);

                // Teleport attackers to their party claim if warp true, otherwise warp them outside the defenders claim
                if (data.getWarp()) {
                    for (ServerPlayer player : data.getAttackingPlayers()) {
                        BlockPos warpPos = data.getAttackingClaim().getWarpPos();
                        player.teleportTo(OPAPC.getServer().overworld(),
                                warpPos.getX() + 0.5, warpPos.getY(), warpPos.getZ() + 0.5,
                                player.getYRot(), player.getXRot());
                    }
                } else {
                    for (ServerPlayer player : data.getAttackingPlayers()) {
                        BlockPos safePos = SafeWarpFinder.findSafeSpawnOutsideClaim(data.getDefendingClaim());
                        player.teleportTo(OPAPC.getServer().overworld(),
                                safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5,
                                player.getYRot(), player.getXRot());
                    }
                }

                // Broadcast result
                String msg = "§cWar ended between " +
                        data.getAttackingParty().getName() + " and " +
                        data.getDefendingParty().getName() +
                        " (" + ended.getEndType() + ")";
                OPAPC.broadcast(msg);
            }
        });

        //Cancel death and inform WarManager if player died while in war
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, amount) -> {

            //Checks if entity that died is a player
            if (entity instanceof ServerPlayer player) {
                WarManager2.INSTANCE.handlePlayerDeath(player);
                return false;
            }

            // Allows death
            return true;
        });

        //Tick the war
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            WarManager2.INSTANCE.tickAll();
        });

        // If player's party is in war on join, then kick if not part of the war
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            UUID uuid = player.getUUID();

            var party = OPAPC.getPartyManager().getPartyByMember(uuid);
            if (party == null) return;

            for (WarData2 war : WarManager2.INSTANCE.getActiveWars()) {
                WarData2 data = war.getData();
                if (war.getDefendingParty().equals(party) || war.getAttackingParty().equals(party)) {
                    if (!war.getAttackingPlayers().contains(player) || !war.getDefendingPlayers().contains(player)) {
                        player.connection.disconnect(Component.literal("Your party is currently engaged in a war. You cannot join right now."));
                    }
                }
            }
        });
    }
}
