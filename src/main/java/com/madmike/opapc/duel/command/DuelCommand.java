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

package com.madmike.opapc.duel.command;

import com.madmike.opapc.OPAPC;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class DuelCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((commandDispatcher, commandBuildContext, commandSelection) -> {

            LiteralArgumentBuilder<CommandSourceStack> duelCommand = literal("duel").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayer();
                if (player != null) {
                    player.sendSystemMessage(Component.literal("""
                        §6====== Duel Command Help ======
                    
                        §e/duel challenge <player> <map> <wager> §7- Challenge a player to a Duel, wager (in gold) is optional
                        §e/duel accept §7- Accept a challenge
                        §e/duel deny §7- Deny a challenge
                        §e/duel profile §7- View your Duel stats
                        §e/duel top §7- View top performing duelers
                        
                        §6--- Rules ---
                        
                        §7• Be the first to 3 kills, winner takes all
                        §7• You cannot challenge party members to duels
                    
                        §6--- Server Owners ---
                        
                        §e/duel maps add <mapName> §7- Add a map
                        §e/duel maps rename <currentName> <newName> §7- Rename an existing map
                        §e/duel maps spawn add <mapName> §7- Adds a spawn point where you are standing
                        §e/duel maps spawn remove <mapName> <spawnPoint> §7- Removes the spawn point from the map
                        §e/duel maps setUnderConstruction <mapName> §7- Set a map under construction and not viable for duels
                        §e/duel maps setReady <mapName> §7- Set a map ready for duels
                        """)
                    );
                }
                return 1;
            });


            duelCommand.then(literal("challenge")
                    .then(argument("player", StringArgumentType.string())
                            .suggests((context, builder) -> {

                                ServerPlayer player = context.getSource().getPlayer();
                                if (player == null) return builder.buildFuture();

                                IServerPartyAPI party = OPAPC.getPartyManager().getPartyByMember(context.getSource().getPlayer().getUUID());

                                for (ServerPlayer otherPlayer : OPAPC.getServer().getPlayerList().getPlayers()) {
                                    if (party != null) {
                                        IServerPartyAPI otherParty = OPAPC.getPartyManager().getPartyByMember(otherPlayer.getUUID());
                                        if (otherParty != null && otherParty.getId().equals(party.getId())) {
                                            continue;
                                        }
                                    }

                                    builder.suggest(otherPlayer.getName().getString());
                                }

                                return builder.buildFuture();
                            })
                            .then(argument("map", StringArgumentType.string())
                                    .suggests(((context, builder) -> {

                                    }))
                            )
                    )
            );

            commandDispatcher.register(duelCommand);
        });
    }
}
