package com.madmike.opapc.raid.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.literal;

public class RaidCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register(((commandDispatcher, commandBuildContext, commandSelection) -> {

            LiteralArgumentBuilder<CommandSourceStack> raidCommand = literal("raid").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayer();
                if (player != null) {
                    player.sendSystemMessage(Component.literal("""
                        §6====== Raid Command Help ======
                    
                        §e/raid declare <party> <warp> §7- Declare a raid on a party
                        §e/raid info §7- View your current raid status
                        §e/raid top §7- View top performing raiders
                    
                        §6--- Rules & Mechanics ---
                        §7• Limited amount of block interactions for raider
                        §7• Declaring war disables the target’s protections and barriers
                    
                        §6--- Objectives ---
                        §7• A §eWar Block §7spawns in the defender's claim
                        §7• Attackers must destroy it to steal a claim
                        §7• The block will respawn in a new chunk up to a max 3 per defender
                        §7• When all blocks are destroyed, attackers win
                        §7• When time runs out, or all attacker lives are lost, defenders win
                        §7• After a war ends, defenders are given war insurance
                        §7• Insurance lasts for 3 days
                    
                        §6--- Dynamic War Scaling ---
                        §7• Duration: §e3 minutes §7per online defender
                        §7• Attacker Lives: §e3 lives §7per online defender
                        §7• War Block Spawns: §e3 per defender
                    
                        §6--- Buff System ---
                        §7• If teams are imbalanced, the team with less players gains §ebuffs
                        §7• Buff strength scales with the player gap
                        """)
                    );
                }
                return 1;
            });

            commandDispatcher.register(raidCommand);

        }));
    }
}
