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

package com.madmike.opapc.bounty.command;

import com.glisco.numismaticoverhaul.ModComponents;
import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.OPAPCConfig;
import com.madmike.opapc.bounty.components.scoreboard.BountyBoardComponent;
import com.madmike.opapc.util.CurrencyUtil;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

import static com.madmike.opapc.util.CommandFailureHandler.fail;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class BountyCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((commandDispatcher, commandBuildContext, commandSelection) -> {

            LiteralArgumentBuilder<CommandSourceStack> bountyCommand = literal("bounty").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayer();
                if (player != null) {
                    player.sendSystemMessage(Component.literal("""
                            §6====== Bounty Command Help ======
                            
                            §e/war declare <party> §7- Declare war on a party
                            §e/war warp §7- (Def & Merc Only) Free warp to the defending claim
                            §e/war enlist <party> §7- (Def Ally Only) Help defend an ally's claim
                            §e/war info §7- View your current war status
                            §e/war top §7- View top performing war parties
                            §e/war insurance §7- View your claim's insurance info
                            §e/war merc info <player> §7- View a mercenary's record and fee
                            §e/war merc hire <player> §7- (Def Only) Hire the mercenary
                            §e/war merc kick <player> §7- (Def Only) Kick a mercenary
                            §e/war merc set <bool> §7- (Merc Only) Set hiring status
                            §e/war merc set fee <gold> §7- (Merc Only) Set hiring fee
                            
                            §6--- Objectives ---
                            §7• Declare war by getting close to an enemy's claim
                            §7• All members of attacking party will be teleported to party leader
                            §7• Defenders get time to set up defences, hire mercenaries and gather allies.
                            §7• After prep, the defending parties claim protections are dropped
                            §7• Destroy the war blocks that spawn in the defenders territory
                            §7• Destroying war blocks unclaims the defenders chunk it is in
                            §7• It also awards the attacking party a bonus claim
                            §7• If defending, kill all attackers OR
                            §7• Hold them off until time runs out
                            """)
                    );
                }
                return 1;
            });

            bountyCommand.then(literal("view")
                    .executes(ctx -> {
                        ServerPlayer self = ctx.getSource().getPlayer();
                        if (self == null) {
                            return fail(ctx, "Only a player can use this command");
                        }
                        BountyBoardComponent board = OPAPCComponents.BOUNTY_BOARD.get(ctx.getSource().getServer().getScoreboard());
                        long bounty = board.getBounty(self.getUUID());
                        ctx.getSource().sendSystemMessage(Component.literal("§6Your bounty: §e" + CurrencyUtil.fromTotalBronze(bounty).gold()));
                        return 1;
                    })

                    .then(literal("all")
                            .executes(ctx -> {
                                BountyBoardComponent board = OPAPCComponents.BOUNTY_BOARD.get(ctx.getSource().getServer().getScoreboard());
                                var all = board.getAllBounties().entrySet().stream()
                                        .filter(e -> e.getValue() > 0)
                                        .sorted(Map.Entry.<UUID, Long>comparingByValue(Comparator.reverseOrder()))
                                        .toList();

                                if (all.isEmpty()) {
                                    ctx.getSource().sendSystemMessage(Component.literal("§7No active bounties."));
                                    return 1;
                                }

                                ctx.getSource().sendSystemMessage(Component.literal("§6Active Bounties:"));

                                for (int i = 0; i < all.size(); i++) {
                                    var e = all.get(i);
                                    String name = OPAPCComponents.PLAYER_NAMES.get(OPAPC.scoreboard()).getPlayerNameById(e.getKey());
                                    ctx.getSource().sendSystemMessage(Component.literal("  §e" + (i + 1) + ". §f" + name + " §7- §6" + CurrencyUtil.fromTotalBronze(e.getValue()).gold() fmtCoins(e.getValue())));
                                }
                                return 1;
                            })
                    )
            );

                    // /bounty add <player> <amount>
                    bountyCommand.then(literal("add")
                            .then(argument("target", EntityArgument.player())
                                    .then(argument("amount", IntegerArgumentType.integer(1))
                                            .executes(ctx -> {
                                                ServerPlayer setter = ctx.getSource().getPlayerOrException();
                                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                int amountInt = IntegerArgumentType.getInteger(ctx, "amount");
                                                long amount = amountInt * 10000L;

                                                // Only non-mercs may set bounties
                                                if (OPAPCComponents.MERC.get(setter).isMerc()) {
                                                    fail(ctx.getSource(), "Only non-mercs can set or contribute to bounties.");
                                                    return 0;
                                                }
                                                // Target must not be a merc
                                                if (OPAPCComponents.MERC.get(target).isMerc()) {
                                                    fail(ctx.getSource(), "You cannot place a bounty on a merc.");
                                                    return 0;
                                                }
                                                // No self-bounty
                                                if (setter.getUUID().equals(target.getUUID())) {
                                                    return fail(ctx.getSource(), "You cannot place a bounty on yourself.");
                                                }

                                                var currency = ModComponents.CURRENCY.get(setter);
                                                long bal = currency.getValue(); // assumes your component exposes a getter
                                                if (bal < amount) {
                                                    return fail(ctx.getSource(), "Insufficient funds. Needed: " +  fmtCoins(amount) + ", you have: " + fmtCoins(bal) + ".");
                                                }

                                                // Withdraw funds and add to bounty
                                                currency.modify(-amount);

                                                BountyBoardComponent board = OPAPCComponents.BOUNTIES.get(ctx.getSource().getServer().getScoreboard());
                                                board.addToBounty(target.getUUID(), amount);

                                                long newTotal = board.getBounty(target.getUUID());
                                                ctx.getSource().sendSystemMessage(Component.literal("§aAdded §e" + fmtCoins(amount) + " §ato bounty on §f" + target.getGameProfile().getName() + "§a. New total: §6" + fmtCoins(newTotal)));
                                                // Optional broadcast:
                                                // broadcast(ctx.getSource().getServer(), "§6Bounty Update: §f" + setter.getGameProfile().getName() + " §7added §e" + fmtCoins(amount) + " §7to §f" + target.getGameProfile().getName() + "§7.");
                                                return 1;
                                            })
                                    )
                            )
                    )
                    // /bounty track <player>
                    .then(literal("track")
                            .then(argument("target", EntityArgument.player())
                                    .executes(ctx -> {
                                        ServerPlayer tracker = ctx.getSource().getPlayerOrException();
                                        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");

                                        // Merc-only
                                        if (!OPAPCComponents.MERC.get(tracker).isMerc()) {
                                            fail(ctx.getSource(), "Only mercs can use /bounty track.");
                                            return 0;
                                        }

                                        // Require the target to actually have a bounty
                                        BountyBoardComponent board = OPAPCComponents.BOUNTIES.get(ctx.getSource().getServer().getScoreboard());
                                        long bounty = board.getBounty(target.getUUID());
                                        if (bounty <= 0L) {
                                            fail(ctx.getSource(), "That player does not currently have a bounty.");
                                            return 0;
                                        }

                                        // Pay the fee (10 gold)
                                        var wallet = ModComponents.CURRENCY.get(tracker);
                                        long bal = wallet.get();
                                        if (bal < TRACK_COST) {
                                            fail(ctx.getSource(), "Tracking costs §e" + fmtCoins(TRACK_COST) + "§7. You only have §e" + fmtCoins(bal) + "§7.");
                                            return 0;
                                        }
                                        wallet.modify(-TRACK_COST);

                                        // Dimension check
                                        if (!tracker.level().dimension().equals(target.level().dimension())) {
                                            ctx.getSource().sendSystemMessage(Component.literal("§7" + target.getGameProfile().getName() + " is in another dimension."));
                                            return 1;
                                        }

                                        // Direction + distance
                                        Vec3 a = tracker.position();
                                        Vec3 b = target.position();
                                        double dx = b.x - a.x;
                                        double dz = b.z - a.z;
                                        double dist = Math.sqrt(dx * dx + dz * dz);
                                        String dir = dirFrom(dx, dz);

                                        ctx.getSource().sendSystemMessage(Component.literal(
                                                "§6Tracking §f" + target.getGameProfile().getName() + "§6 → §e" + dir + " §7(~" + (int) dist + "m)"
                                        ));
                                        return 1;
                                    })
                            )
                    )
            )
            )
        });

    }

    /* ---------------- helpers ---------------- */

    // 8-way compass from dx/dz
    private static String dirFrom(double dx, double dz) {
        if (dx == 0 && dz == 0) return "Here";
        double angle = Math.atan2(-dz, dx); // rotate so +Z is south; adjust if you prefer
        double deg = Math.toDegrees(angle);
        // Normalize to [0,360)
        deg = (deg + 360.0) % 360.0;

        // 8 sectors (45° each), centered on E (0°)
        String[] names = {"E", "NE", "N", "NW", "W", "SW", "S", "SE"};
        int idx = (int) Math.floor((deg + 22.5) / 45.0) % 8;
        return names[idx];
    }

    // Optional: if you want a server-wide broadcast
    // private static void broadcast(MinecraftServer server, String msg) {
    //     server.getPlayerList().broadcastSystemMessage(Component.literal(msg), false);
    // }
}
