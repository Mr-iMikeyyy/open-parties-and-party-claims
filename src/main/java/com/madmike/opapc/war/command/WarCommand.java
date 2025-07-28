package com.madmike.opapc.war.command;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.OPAPCConfig;
import com.madmike.opapc.partyclaim.components.scoreboard.PartyClaimsComponent;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import com.madmike.opapc.war.WarManager;
import com.madmike.opapc.war.data.WarData;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;
import xaero.pac.common.server.player.config.api.IPlayerConfigAPI;
import xaero.pac.common.server.player.config.api.IPlayerConfigManagerAPI;
import xaero.pac.common.server.player.config.api.PlayerConfigOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
                    
                        §e/war declare <party> <warp> §7- Declare war on a party
                        §e/war info §7- View your current war status
                        §e/war top §7- View top performing war parties
                    
                        §6--- Rules & Mechanics ---
                        §7• Only party leaders can declare wars
                        §7• Declaring war disables the target’s protections and barriers
                        §7• Destroy war blocks that spawn in the defenders territory
                        §7• Logins from offline players on either team are blocked
                    
                        §6--- Objectives ---
                        §7• At start, a §eWar Block §7spawns in the defender's claim
                        §7• Attackers must destroy it to steal a claim
                        §7• The block will respawn in a new chunk up to a max 3 per defender
                        §7• When all blocks are destroyed, attackers win
                        §7• When time runs out, or all attacker lives are lost, defenders win
                        §7• After a war ends, defenders are given war insurance
                        §7• Insurance lasts for 3 days and protects from war
                    
                        §6--- Dynamic War Scaling ---
                        §7• Duration: §e3 minutes §7per online defender
                        §7• Attacker Lives: §e3 attacker lives §7per online defender
                        §7• War Block Spawns: §e3 per defender
                    
                        §6--- Buff System ---
                        §7• If teams are imbalanced, the team with less players gains §ebuffs
                        §7• Buff strength scales with the player gap
                        """)
                    );
                }
                return 1;
            });

            warCommand.then(literal("declare")
                    .then(argument("party", StringArgumentType.string())
                            .suggests((context, builder) -> {
                                ServerPlayer player = context.getSource().getPlayer();
                                if (player == null) return builder.buildFuture();

                                IServerPartyAPI party = OPAPC.getPartyManager().getPartyByOwner(player.getUUID());
                                if (party == null) return builder.buildFuture();

                                PartyClaimsComponent comp = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard());
                                PartyClaim attackingClaim = comp.getClaim(party.getId());
                                if (attackingClaim == null) return builder.buildFuture();

                                for (PartyClaim claim : comp.getAllClaims().values()) {
                                    if (!claim.isWarInsured() && !claim.getPartyId().equals(attackingClaim.getPartyId())) {
                                        builder.suggest(claim.getPartyName());
                                    }
                                }

                                return builder.buildFuture();
                            })
                            .then(argument("warp", BoolArgumentType.bool())
                                    .executes(ctx -> {
                                        ServerPlayer player = ctx.getSource().getPlayer();
                                        if (player == null) {
                                            ctx.getSource().sendFailure(Component.literal("Must be a player to use this command."));
                                            return 0;
                                        }

                                        IServerPartyAPI attackingParty = OPAPC.getPartyManager().getPartyByOwner(player.getUUID());
                                        if (attackingParty == null) {
                                            ctx.getSource().sendFailure(Component.literal("Must own a party to declare a war"));
                                            return 0;
                                        }

                                        PartyClaim attackingClaim = OPAPCComponents.PARTY_CLAIMS
                                                .get(OPAPC.getServer().getScoreboard()).getClaim(attackingParty.getId());
                                        if (attackingClaim == null) {
                                            ctx.getSource().sendFailure(Component.literal("Your party must own a claim to declare war"));
                                            return 0;
                                        }

                                        String inputName = StringArgumentType.getString(ctx, "party");
                                        boolean shouldWarp = BoolArgumentType.getBool(ctx, "warp");

                                        IPartyManagerAPI pm = OPAPC.getPartyManager();
                                        IPlayerConfigManagerAPI cm = OPAPC.getPlayerConfigs();

                                        IServerPartyAPI defendingParty = null;
                                        for (IServerPartyAPI party : pm.getAllStream().toList()) {
                                            UUID ownerId = party.getOwner().getUUID();
                                            IPlayerConfigAPI ownerConfig = cm.getLoadedConfig(ownerId);
                                            String partyName = ownerConfig.getEffective(PlayerConfigOptions.PARTY_NAME);

                                            if (partyName.equalsIgnoreCase(inputName)) {
                                                defendingParty = party;
                                                break;
                                            }
                                        }

                                        if (defendingParty == null) {
                                            ctx.getSource().sendFailure(Component.literal("No party with that name was found."));
                                            return 0;
                                        }

                                        PartyClaim defendingClaim = OPAPCComponents.PARTY_CLAIMS
                                                .get(OPAPC.getServer().getScoreboard()).getClaim(defendingParty.getId());
                                        if (defendingClaim == null) {
                                            ctx.getSource().sendFailure(Component.literal("That party does not own a party claim"));
                                            return 0;
                                        }

                                        if (defendingClaim.isWarInsured()) {
                                            ctx.getSource().sendFailure(Component.literal("That party is currently insured against wars"));
                                            return 0;
                                        }

                                        for (WarData war : WarManager.INSTANCE.getActiveWars()) {
                                            if (war.getAttackingParty().getId().equals(attackingParty.getId())
                                                    || war.getDefendingParty().getId().equals(attackingParty.getId())) {
                                                player.sendSystemMessage(Component.literal("You are already in a war!"));
                                                return 0;
                                            }
                                            if (war.getDefendingParty().getId().equals(defendingParty.getId())
                                                    || war.getAttackingParty().getId().equals(defendingParty.getId())) {
                                                player.sendSystemMessage(Component.literal("This party is already in a war."));
                                                return 0;
                                            }
                                        }

                                        if (defendingParty.getOnlineMemberStream().toList().isEmpty()) {
                                            player.sendSystemMessage(Component.literal("There's no one online to defend that claim."));
                                            return 0;
                                        }

                                        WarManager.INSTANCE.declareWar(attackingParty, defendingParty, shouldWarp);
                                        return 1;
                                    })
                            )
                    )
            );

            warCommand.then(literal("info")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayer();
                        if (player != null) {
                            WarManager.INSTANCE.displayWarInfo(player);
                        }
                        return 1;
                    })
            );

            dispatcher.register(warCommand);

        });
    }

}
