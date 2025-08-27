/*
 * Copyright (C) 2025 Mr-iMikeyyy (and contributors)
 *
 * This file is part of OPAPC (Open Parties and Party Claims).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of version 3 of the GNU Lesser General Public License
 * (LGPL-3.0-only) as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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

            }

            //--- WAR ENDED ---
            if (event instanceof WarEndedEvent ended) {
                WarData data = ended.getWar().getData();
                EndOfWarType type = ended.getEndType();

                // Always restore protections
                OPAPC.playerConfigs()
                        .getLoadedConfig(data.getDefendingParty().getOwner().getUUID())
                        .getUsedSubConfig().tryToSet(PlayerConfigOptions.PROTECT_CLAIMED_CHUNKS, true);

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
                    result.append(Component.literal("§eAttacker Lives Remaining: §c" + data.getAttackersLeftToKill().size() + "\n"))
                            .append(Component.literal("§eWar Blocks Remaining: §c" + data.getWarBlocksLeft() + "\n"))
                            .append(Component.literal("§eDuration: §7" + (data.getDurationSeconds() / 60) + " min\n"))
                            .append(Component.literal("§eClaim Wipe Possible: §c" + (data.getDefendingClaim().getClaimedChunksList().isEmpty() ? "Yes" : "No") + "\n\n"));
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

            if (entity instanceof ServerPlayer player) {
                War war = WarManager.INSTANCE.findWarByPlayer(player);
                if (war != null) {
                    WarManager.INSTANCE.handlePlayerDeath(player, war);
                    //Disallow Death
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

            var party = OPAPC.parties().getPartyByMember(uuid);
            if (party == null) return;

            WarManager wm = WarManager.INSTANCE;
            War warByPlayer = wm.findWarByPlayer(player);
            if (warByPlayer != null) {
                return;
            }

            War warByParty = wm.findWarByParty(party);
            if (warByParty != null) {
                player.connection.disconnect(Component.literal("Your party is currently engaged in a war. You cannot join right now."));
            }
        });
    }
}
