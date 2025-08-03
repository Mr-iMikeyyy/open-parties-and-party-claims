package com.madmike.opapc.war.event;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCConfig;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import com.madmike.opapc.util.SafeWarpHelper;
import com.madmike.opapc.war.EndOfWarType;
import com.madmike.opapc.war.War;
import com.madmike.opapc.war.WarManager;
import com.madmike.opapc.war.data.WarData;
import com.madmike.opapc.war.event.bus.WarEventBus;
import com.madmike.opapc.war.event.events.WarDeclaredEvent;
import com.madmike.opapc.war.event.events.WarEndedEvent;
import com.madmike.opapc.war.event.events.WarStartedEvent;
import com.madmike.opapc.war.features.block.WarBlockSpawner;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import xaero.pac.common.server.player.config.api.PlayerConfigOptions;

import java.util.List;
import java.util.UUID;

public class WarEvents {
    public static void register() {
        WarEventBus.register(event -> {

            //--- WAR DECLARED ---
            if (event instanceof WarDeclaredEvent declared) {
                WarData data = declared.getWar().getData();
                if (OPAPCConfig.shouldBroadcastWarDeclarationsServerWide) {
                    OPAPC.broadcast(data.getInfo());
                }
                else {
                    data.broadcastToWar(data.getInfo());
                }
            }

            //--- WAR STARTED ---
            if (event instanceof WarStartedEvent started) {
                WarData data = started.getWar().getData();

                BlockPos safeBlockSpawnPos = WarBlockSpawner.findSafeSpawn(data);
                if (safeBlockSpawnPos != null) {
                    WarBlockSpawner.spawnWarBlock(safeBlockSpawnPos);
                }
                else {
                    started.getWar().getState().end(started.getWar(), EndOfWarType.BUG);
                }

                if (data.getWarp()) {
                    for (ServerPlayer player : data.getAttackingPlayers()) {
                        BlockPos spawnPos = SafeWarpHelper.findSafeSpawnOutsideClaim(data.getDefendingClaim());
                        if (spawnPos != null) {
                            SafeWarpHelper.warpPlayer(player, spawnPos);
                        }
                        else {
                            started.getWar().getState().end(started.getWar(), EndOfWarType.BUG);
                        }
                    }
                }


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

            //--- WAR ENDED ---
            if (event instanceof WarEndedEvent ended) {
                WarData data = ended.getWar().getData();
                EndOfWarType type = ended.getEndType();

                // Always restore protections
                OPAPC.getPlayerConfigs()
                        .getLoadedConfig(data.getDefendingParty().getOwner().getUUID())
                        .getUsedSubConfig().tryToSet(PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS, true);

                // Only teleport attackers if NOT a wipe without warp
                boolean shouldTeleportAttackers = !(type == EndOfWarType.ATTACKERS_WIN_WIPE && !data.getWarp());

                if (shouldTeleportAttackers) {
                    if (data.getWarp()) {
                        for (ServerPlayer player : data.getAttackingPlayers()) {
                            BlockPos warpPos = data.getAttackingClaim().getWarpPos();
                            if (warpPos != null) {
                                SafeWarpHelper.warpPlayer(player, warpPos);
                            } else {
                                BlockPos sharedPos = OPAPC.getServer().overworld().getSharedSpawnPos();
                                SafeWarpHelper.warpPlayer(player, sharedPos);
                            }
                        }
                    } else {
                        for (ServerPlayer player : data.getAttackingPlayers()) {
                            BlockPos safePos = SafeWarpHelper.findSafeSpawnOutsideClaim(data.getDefendingClaim());
                            if (safePos != null) {
                                SafeWarpHelper.warpPlayer(player, safePos);
                            } else {
                                BlockPos sharedPos = OPAPC.getServer().overworld().getSharedSpawnPos();
                                SafeWarpHelper.warpPlayer(player, sharedPos);
                            }
                        }
                    }
                }

                PartyClaim defendingClaim = data.getDefendingClaim();
                PartyClaim attackingClaim = data.getAttackingClaim();

                // === Build Results Message and do stats ===
                Component msg;
                String winner;

                MutableComponent result = Component.literal("§6§l--- War Results ---\n")
                        .append(Component.literal("§eAttacking Party: §c" + data.getAttackingPartyName() + "\n"))
                        .append(Component.literal("§eDefending Party: §a" + data.getDefendingPartyName() + "\n"))
                        .append(Component.literal("§eEnd Condition: §b" + type.name().replace("_", " ") + "\n"));

                switch (type) {
                    case ATTACKERS_WIN_WIPE -> {

                        attackingClaim.incrementWarAttacksWon();

                        winner = "§cAttackers (" + data.getAttackingPartyName() + ")";
                        result.append(Component.literal("§eWinner: " + winner + "\n"))
                                .append(Component.literal("§4§lThe defending party has been wiped from the map!\n"))
                                .append(Component.literal("§7Their claims no longer exist.\n\n"));
                    }
                    case ATTACKERS_WIN_BLOCKS -> {

                        attackingClaim.incrementWarAttacksWon();
                        defendingClaim.incrementWarDefencesLost();

                        winner = "§cAttackers (" + data.getAttackingPartyName() + ")";
                        result.append(Component.literal("§eWinner: " + winner + "\n"))
                                .append(Component.literal("§eThe attackers broke all available War Blocks!\n"))
                                .append(Component.literal("§7The defending party survives, but with reduced territory.\n\n"));
                    }
                    case BUG -> {
                        winner = "§4§lNone (Error)";
                        result.append(Component.literal("§eWinner: " + winner + "\n"))
                                .append(Component.literal("§4§lThis war ended unexpectedly due to a bug.\n"))
                                .append(Component.literal("§7Please report this to a server admin.\n\n"));
                    }
                    default -> {

                        attackingClaim.incrementWarAttacksLost();
                        defendingClaim.incrementWarDefencesWon();

                        winner = "§aDefenders (" + data.getDefendingPartyName() + ")";
                        result.append(Component.literal("§eWinner: " + winner + "\n"))
                                .append(Component.literal("§aThe defenders successfully held their ground!\n\n"));
                    }
                }

                if (type != EndOfWarType.BUG) {
                    result.append(Component.literal("§eAttacker Lives Remaining: §c" + data.getAttackerLivesRemaining() + "\n"))
                            .append(Component.literal("§eWar Blocks Remaining: §c" + data.getWarBlocksLeft() + "\n"))
                            .append(Component.literal("§eDuration: §7" + (data.getDurationSeconds() / 60) + " min\n"))
                            .append(Component.literal("§eClaim Wipe Possible: §c" + (data.getDefendingClaim().getClaimedChunksList().isEmpty() ? "Yes" : "No") + "\n\n"))
                            .append(Component.literal("§cAttackers: " + data.getAttackingPlayers().size() + "\n"));
                    for (ServerPlayer attacker : data.getAttackingPlayers()) {
                        result.append(Component.literal(" §7- §c" + attacker.getName().getString() + "\n"));
                    }
                    result.append(Component.literal("§aDefenders: " + data.getDefendingPlayers().size() + "\n"));
                    for (ServerPlayer defender : data.getDefendingPlayers()) {
                        result.append(Component.literal(" §7- §a" + defender.getName().getString() + "\n"));
                    }
                }

                msg = result;

                // Broadcast result
                if (OPAPCConfig.shouldBroadcastWarResultsToServer) {
                    OPAPC.broadcast(msg);
                } else {
                    data.broadcastToWar(msg);
                }
            }
        });

        //Cancel death and inform WarManager if player died while in war
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, amount) -> {

            //Checks if entity that died is a player
            if (entity instanceof ServerPlayer player) {
                WarManager wm = WarManager.INSTANCE;
                War war = wm.findWarByPlayer(player);
                if (war != null) {
                    wm.handlePlayerDeath(player, war);
                    return false;
                }
            }

            // Allows death
            return true;
        });

        //Tick the war
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            WarManager.INSTANCE.tickAll();
        });

        // If player's party is in war on join, then kick if not part of the war
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            UUID uuid = player.getUUID();

            var party = OPAPC.getPartyManager().getPartyByMember(uuid);
            if (party == null) return;

            WarManager wm = WarManager.INSTANCE;
            War warByPlayer = wm.findWarByPlayer(player);
            if (warByPlayer != null) {
                return;
            }

            War warByParty = wm.findWarByParty(party);
            if (warByParty != null) {
                player.connection.disconnect(Component.literal("Your party is currently engaged in a war that you were not at the start of. You cannot join right now."));
            }
        });
    }
}
