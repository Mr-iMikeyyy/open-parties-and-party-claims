package com.madmike.opapc.war;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.OPAPCConfig;
import com.madmike.opapc.party.components.scoreboard.PartyClaimsComponent;
import com.madmike.opapc.party.data.PartyClaim;
import com.madmike.opapc.war.data.WarData;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.sun.jdi.connect.Connector;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.server.api.OpenPACServerAPI;
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

    public static void registerWarCommand() {

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralArgumentBuilder<CommandSourceStack> warCommand = literal("war").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayer();
                // Display active wars for the player’s party
                WarManager.INSTANCE.displayWarInfo(player);
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

                                Map<UUID, PartyClaim> allClaims = comp.getAllClaims();
                                List<UUID> idsToLookUp = new ArrayList<>();

                                for (Map.Entry<UUID, PartyClaim> entry : allClaims.entrySet()) {
                                    if (!entry.getValue().isWarInsured()
                                            && entry.getValue().getBoughtClaims() >= attackingClaim.getBoughtClaims()
                                            && !entry.getValue().getPartyId().equals(attackingClaim.getPartyId())) {
                                        idsToLookUp.add(entry.getKey());
                                    }
                                }

                                for (UUID id : idsToLookUp) {
                                    builder.suggest(OPAPC.getPlayerConfigs().getLoadedConfig(
                                            OPAPC.getPartyManager().getPartyById(id).getOwner().getUUID()
                                    ).getEffective(PlayerConfigOptions.PARTY_NAME));
                                }

                                return builder.buildFuture();
                            })
                            .then(argument("teleport", BoolArgumentType.bool())
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
                                        boolean shouldTeleport = BoolArgumentType.getBool(ctx, "teleport");

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

                                        if (attackingClaim.getBoughtClaims() >= defendingClaim.getBoughtClaims()
                                                && OPAPCConfig.canOnlyAttackLargerClaims) {
                                            player.sendSystemMessage(Component.literal("You can only declare war on parties with more claims than you."));
                                            return 0;
                                        }

                                        if (defendingParty.getOnlineMemberStream().findAny().isEmpty()) {
                                            player.sendSystemMessage(Component.literal("There's no one online to defend that claim."));
                                            return 0;
                                        }

                                        WarManager.INSTANCE.declareWar(attackingParty, defendingParty, shouldTeleport);
                                        return 1;
                                    })
                            )
                    )
            );


            warCommand.then(literal("forfeit")).executes(ctx -> {
                //Check if player
                ServerPlayer player = ctx.getSource().getPlayer();
                if (player == null) {
                    ctx.getSource().sendSystemMessage(Component.literal("Must be a player to execute that command."));
                    return 0;
                }

                //Check if owner of party
                IServerPartyAPI party = OPAPC.getPartyManager().getPartyByOwner(player.getUUID());
                if (party == null) {
                    ctx.getSource().sendSystemMessage(Component.literal("Must be an owner of a party"));
                    return 0;
                }

                //Check if party is in a war
                for (WarData war : WarManager.INSTANCE.getActiveWars()) {
                    if (war.getDefendingParty().getId().equals(party.getId()) || war.getAttackingParty().getId().equals(party.getId())) {
                        WarManager.INSTANCE.endWar(war, WarManager.EndOfWarType.FORFEIT);
                    }
                }


            });

            dispatcher.register(literal("war")
                    .then(literal("declare")
                            .then(argument("party", StringArgumentType.string()).suggests((context, builder) -> {

                                //Check if player
                                ServerPlayer player = context.getSource().getPlayer();
                                if (player == null) {
                                    return builder.buildFuture();
                                }

                                //Check if owner of party
                                IServerPartyAPI party = OPAPC.getPartyManager().getPartyByOwner(context.getSource().getPlayer().getUUID());
                                if (party == null) {
                                    return builder.buildFuture();
                                }

                                PartyClaimsComponent comp = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard());
                                PartyClaim attackingClaim = comp.getClaim(party.getId());
                                Map<UUID, PartyClaim> allClaims = comp.getAllClaims();
                                List<UUID> idsToLookUp = new ArrayList<>();

                                for (Map.Entry<UUID, PartyClaim> entry : allClaims.entrySet()) {
                                    if (!entry.getValue().isWarInsured() && entry.getValue().getBoughtClaims() >= attackingClaim.getBoughtClaims() && !entry.getValue().getPartyId().equals(attackingClaim.getPartyId())) {
                                        idsToLookUp.add(entry.getKey());
                                    }
                                }

                                for (UUID id : idsToLookUp) {
                                    builder.suggest(comp.getPartyName(id));
                                }

                                return builder.buildFuture();
                            }))
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayer();
                                if (player == null) {
                                    ctx.getSource().sendFailure(Component.literal("Must be a player to use this command."));
                                    return 0;
                                }
                                OpenPACServerAPI api = OpenPACServerAPI.get(ctx.getSource().getServer());
                                IPartyManagerAPI pm = api.getPartyManager();
                                IServerPartyAPI attackingParty = pm.getPartyByOwner(player.getUUID());

                                if (attackingParty == null) {
                                    ctx.getSource().sendFailure(Component.literal("Must own a party to declare a war"));
                                    return 0;
                                }

                                List<WarData> activeWars = WarManager.INSTANCE.getActiveWars();

                                for (WarData war : activeWars) {
                                    if (war.getAttackingParty().getId().equals(attackingParty.getId())) {
                                        player.sendSystemMessage(Component.literal("You are already in a war!"));
                                        return false;
                                    }
                                    if (war.getDefendingParty().getId().equals(defenderPartyId)) {
                                        player.sendSystemMessage(Component.literal("This party is already under attack."));
                                        return false;
                                    }
                                }

                                if (attackerClaims >= defenderClaims && OPAPCConfig.canOnlyAttackLargerClaims) {
                                    player.sendSystemMessage(Component.literal("You can only declare war on parties with more claims than you."));
                                    return false;
                                }

                                UUID attackerPartyId =
                                        UUID defenderPartyId = PartyArgumentType.getParty(ctx, "party").getId();
                                int attackerClaims = PartyAPI.getClaimsBought(attackerPartyId);
                                int defenderClaims = PartyAPI.getClaimsBought(defenderPartyId);
                                if (WarManager.INSTANCE.canDeclareWar(attackerPartyId, defenderPartyId, attackerClaims, defenderClaims, config, player)) {
                                    WarManager.INSTANCE.declareWar(attackerPartyId, defenderPartyId, player.getWorld(), config);
                                    player.sendMessage(Text.literal("War declared on party: " + defenderPartyId));
                                }
                                return 1;
                            }))
                    .then(literal("info")
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayer();
                                // Display active wars for the player’s party
                                WarManager.INSTANCE.displayWarInfo(player);
                                return 1;
                            }))
                    .then(literal("forfeit")
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayer();
                                UUID defenderPartyId = PartyArgumentType.getParty(ctx, "party").getId();
                                WarManager.INSTANCE.endWar(defenderPartyId, player.getWorld());
                                player.sendSystemMessage(Component.literal("Ended war with party: " + defenderPartyId));
                                return 1;
                            })));
        })
    }

}
