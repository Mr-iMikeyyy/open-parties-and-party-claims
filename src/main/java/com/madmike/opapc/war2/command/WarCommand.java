package com.madmike.opapc.war2.command;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.util.CommandFailureHandler;
import com.madmike.opapc.util.PartyLookup;
import com.madmike.opapc.util.ServerRestartChecker;
import com.madmike.opapc.war2.WarManager2;
import com.madmike.opapc.war2.command.util.WarSuggestionProvider;
import com.madmike.opapc.war2.command.util.WarValidationResult;
import com.madmike.opapc.war2.command.util.WarValidator;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

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
                    
                        §e/war declare <party> <warp> §7- Declare war on a party
                        §e/war info §7- View your current war status
                        §e/war top §7- View top performing war parties
                        
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
                        §7• Attacker Lives: §e3 attacker lives §7per online defender
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
                        if (player == null) { return false; }
                        return OPAPC.getPartyManager().getPartyByOwner(player.getUUID()) != null;
                    })
                    .then(argument("party", StringArgumentType.string())
                            .suggests(WarSuggestionProvider::suggestTargets)
                            .then(argument("warp", BoolArgumentType.bool())
                                    .executes(ctx -> {

                                        ServerPlayer player = ctx.getSource().getPlayer();
                                        if (player == null) return fail(ctx, "Must be a player to use this command.");

                                        if (!ServerRestartChecker.isSafeToStartEventNow())
                                            return fail(player, "Cannot declare war because the server is going to restart soon");

                                        IServerPartyAPI attackingParty = PartyLookup.getOwnerParty(player);
                                        if (attackingParty == null)
                                            return fail(player, "Must own a party to declare a war");

                                        String targetName = StringArgumentType.getString(ctx, "party");
                                        IServerPartyAPI defendingParty = PartyLookup.findByName(targetName);
                                        if (defendingParty == null)
                                            return fail(player, "No party with that name was found.");

                                        WarValidationResult result = WarValidator.validateDeclaration(attackingParty, defendingParty);
                                        if (!result.isValid())
                                            return fail(player, result.getErrorMessage());

                                        boolean shouldWarp = BoolArgumentType.getBool(ctx, "warp");
                                        WarManager2.INSTANCE.declareWar(attackingParty, defendingParty, shouldWarp);

                                        return 1;
                                    })
                            )
                    )
            );
            //endregion

            //region Info
            warCommand.then(literal("info")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayer();
                        if (player == null) {
                            return CommandFailureHandler.fail(ctx, "Must be a player to use this command.");
                        }

                        WarManager2.INSTANCE.displayWarInfo(player);
                        return 1;
                    })
            );
            //endregion


            dispatcher.register(warCommand);

        });
    }

}
