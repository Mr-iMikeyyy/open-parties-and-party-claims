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

package com.madmike.opapc.war.command;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import com.madmike.opapc.util.PartyLookup;
import com.madmike.opapc.util.ServerRestartChecker;
import com.madmike.opapc.war.War;
import com.madmike.opapc.war.WarManager;
import com.madmike.opapc.war.command.util.WarProximity;
import com.madmike.opapc.war.command.util.WarSuggestionProvider;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.List;

import static com.madmike.opapc.util.CommandFailureHandler.fail;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class WarCommand {

    public static void register() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {


            LiteralArgumentBuilder<CommandSourceStack> warCommand = literal("war").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayer();
                if (player != null) {
                    player.sendSystemMessage(Component.literal("""
                            §6====== War Command Help ======
                            
                            §e/war declare <party> §7- Declare war on a party
                            §e/war join §7- Join your party's war
                            §e/war join <party> §7- Join an ally's war
                            §e/war info §7- View your current war status
                            §e/war top §7- View top performing war parties
                            §e/war insurance §7- View your claim's insurance info
                            
                            §6--- Objectives ---
                            §7• Declaring war disables the target party claims protections and barriers
                            §7• Destroy war blocks that spawn in the defenders territory
                            §7• Destroying war blocks unclaims the defenders chunk claim
                            §7• And also awards the attacking party a bonus claim
                            
                            §6--- Rules & Mechanics ---
                            §7• Only party leaders can declare wars
                            §7• Logins from offline players on either team are blocked
                            §7• When all blocks are destroyed, attackers win
                            §7• When time runs out, or all attacker lives are lost, defenders win
                            §7• After a war ends, defenders are given war insurance
                            §7• Insurance lasts for 3 days and protects from war
                            
                            §6--- Dynamic War Scaling ---
                            §7• Duration: §e3 minutes §7per online defender
                            §7• Attacker Lives: §e1 life per attacker
                            §7• War Block Spawns: §e3 per online defender
                            
                            §6--- Buff System ---
                            §7• If teams are imbalanced, the team with less players gains §ebuffs
                            §7• Buff strength scales up the more uneven the teams are
                            """)
                    );
                }
                return 1;
            });

            //region Declare
            warCommand.then(literal("declare")
                    .requires(ctx -> {
                        ServerPlayer player = ctx.getPlayer();
                        if (player == null) {
                            return false;
                        }
                        return OPAPC.parties().getPartyByOwner(player.getUUID()) != null;
                    })
                    .then(argument("party", StringArgumentType.string())
                            .suggests(WarSuggestionProvider::suggestTargets)
                            .executes(ctx -> {

                                ServerPlayer player = ctx.getSource().getPlayer();
                                if (player == null) return fail(ctx, "Must be a player to use this command.");

                                if (!player.level().dimension().equals(Level.OVERWORLD))
                                    return fail(player, "You can only declare war from the overworld.");

                                if (!ServerRestartChecker.isSafeToStartEventNow())
                                    return fail(player, "Cannot declare war because the server is going to restart soon");

                                IServerPartyAPI attackingParty = PartyLookup.getOwnerParty(player);
                                if (attackingParty == null)
                                    return fail(player, "Must own a party to declare a war");

                                String targetName = StringArgumentType.getString(ctx, "party");
                                IServerPartyAPI defendingParty = PartyLookup.findByName(targetName);
                                if (defendingParty == null)
                                    return fail(player, "No party with that name was found.");

                                if (attackingParty.isAlly(defendingParty.getId()))
                                    return fail(player, "You cannot declare war on your allies.");

                                for (ServerPlayer attackingPlayer : attackingParty.getOnlineMemberStream().toList()) {
                                    if (OPAPCComponents.IN_DUEL.get(attackingPlayer).isInDuel()) {
                                        return fail(player, "One of your players is in a duel.");
                                    }
                                }

                                for (ServerPlayer defendingPlayer : defendingParty.getOnlineMemberStream().toList()) {
                                    if (OPAPCComponents.IN_DUEL.get(defendingPlayer).isInDuel()) {
                                        return fail(player, "One of the defending players is in a duel.");
                                    }
                                }

                                var comp = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.scoreboard());

                                //Check Attacking claim
                                PartyClaim attackingClaim = comp.getClaim(attackingParty.getId());
                                if (attackingClaim == null)
                                    return fail(player, "Your party must own a claim to declare war.");

                                //Check defending claim
                                PartyClaim defendingClaim = comp.getClaim(defendingParty.getId());
                                if (defendingClaim == null) {
                                    return fail(player, "That party does not own a claim.");
                                }

                                //Check proximity
                                if (!WarProximity.isWithinLoadingDistance(player, defendingClaim)) {
                                    return fail(player, "You are not near enough to the claim to declare war, move closer.");
                                }

                                //Check insurance
                                if (defendingClaim.isWarInsured()) {
                                    return fail(player, "That party is currently insured against wars.");
                                }

                                // Check active wars
                                if (WarManager.INSTANCE.isWarActive()) {
                                    War war = WarManager.INSTANCE.findWarByParty(attackingParty);
                                    if (war != null) {
                                        return fail(player, "You are already in a war!");
                                    }
                                    war = WarManager.INSTANCE.findWarByParty(defendingParty);
                                    if (war != null) {
                                        return fail(player, "This party is already in a war!");
                                    }
                                }


                                // Check online defenders
                                List<ServerPlayer> defenders = defendingParty.getOnlineMemberStream().toList();
                                if (defenders.isEmpty()) {
                                    return fail(player, "There's no one online to defend that claim.");
                                }

                                //Check player has enough money
                                CurrencyComponent wallet = ModComponents.CURRENCY.get(player);
                                int cost = defendingClaim.getClaimedChunksList().size();
                                if (wallet.getValue() < defendingClaim.getClaimedChunksList().size()) {
                                    return fail(player, "You don't have enough gold to attack this claim. Requires " + cost + " gold.");
                                }

                                WarManager.INSTANCE.declareWar(attackingParty, defendingParty, attackingClaim, defendingClaim, player.blockPosition());
                                wallet.modify(-cost);
                                return 1;
                            })
                    )
            );
            //endregion

            //region Join
            warCommand.then(literal("join")
                    .requires(ctx -> ctx.getPlayer() != null)
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayer();
                        if (player == null) return fail(ctx, "Must be a player to use this command.");

                        if (OPAPCComponents.IN_DUEL.get(player).isInDuel())
                            return fail(player, "You cannot join a war while in a duel.");

                        // Find player's party
                        IServerPartyAPI myParty = OPAPC.parties().getPartyByMember(player.getUUID());
                        if (myParty == null)
                            return fail(player, "You must be in a party to join a war.");

                        // Find war involving your party
                        War war = WarManager.INSTANCE.findWarByParty(myParty);
                        if (war == null)
                            return fail(player, "Your party is not currently in a war.");

                        // Try join on the correct side based on party membership
                        boolean ok = WarManager.INSTANCE.tryJoin(war, player, myParty, null);
                        if (!ok) return fail(player, "Unable to join the war right now.");

                        player.sendSystemMessage(Component.literal("§aJoined your party's war."));
                        return 1;
                    })
                    .then(argument("ally", StringArgumentType.string())
                            .suggests(WarSuggestionProvider::suggestAlliesInWar) // <-- implement or replace with your own
                            .requires(ctx -> ctx.getPlayer() != null)
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayer();
                                if (player == null) return fail(ctx, "Must be a player to use this command.");

                                if (OPAPCComponents.IN_DUEL.get(player).isInDuel())
                                    return fail(player, "You cannot join a war while in a duel.");

                                // Your party:
                                IServerPartyAPI myParty = OPAPC.parties().getPartyByMember(player.getUUID());
                                if (myParty == null)
                                    return fail(player, "You must be in a party to join an ally's war.");

                                // Target ally party name -> party
                                String allyName = StringArgumentType.getString(ctx, "ally");
                                IServerPartyAPI allyParty = PartyLookup.findByName(allyName);
                                if (allyParty == null)
                                    return fail(player, "No party with that name was found.");

                                // You must actually be allies
                                if (!myParty.isAlly(allyParty.getId()))
                                    return fail(player, "Your party is not allied with that party.");

                                // Find war that ally is participating in
                                War war = WarManager.INSTANCE.findWarByParty(allyParty);
                                if (war == null)
                                    return fail(player, "That ally is not currently in a war.");

                                // Join on the ally's side
                                boolean ok = WarManager.INSTANCE.tryJoin(war, player, myParty, allyParty);
                                if (!ok) return fail(player, "Unable to join your ally's war right now.");

                                player.sendSystemMessage(Component.literal("§aJoined your ally's war on their side."));
                                return 1;
                            })
                    )
            );
            //endregion

            //region Info
            warCommand.then(literal("info")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayer();
                        if (player == null) {
                            return fail(ctx, "Must be a player to use this command.");
                        }

                        War war = WarManager.INSTANCE.findWarByPlayer(player);
                        if (war != null) {
                            war.onRequestInfo(player);
                        } else {
                            return fail(player, "You are not in a war");
                        }
                        return 1;
                    })
            );
            //endregion

            dispatcher.register(warCommand);

        });
    }
}
