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

package com.madmike.opapc.trade.command;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.OPAPCConfig;
import com.madmike.opapc.trade.components.player.UnlockedStoreSlotsComponent;
import com.madmike.opapc.trade.components.scoreboard.OffersComponent;
import com.madmike.opapc.trade.data.Offer;
import com.madmike.opapc.trade.data.SellerInfo;
import com.madmike.opapc.util.CurrencyUtil;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.*;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class TradeCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register(((commandDispatcher, commandBuildContext, commandSelection) -> {
            LiteralArgumentBuilder<CommandSourceStack> tradeCommand = literal("trade").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayer();
                if (player != null) {
                    player.sendSystemMessage(Component.literal("""
                            §6====== Trade Command Help ======
                            
                            §e/trade upgrade §7- Increase the max slots available for you to sell
                            §e/trade profile §7- View your seller profile
                            §e/trade top §7- View the top performing sellers
                            §e/trade sell <gold> <silver> <bronze> §7- Sell the item you are holding
                            """)
                    );
                }
                return 1;
            });

            tradeCommand.then(literal("upgrade")
                    .executes(ctx -> {

                        ServerPlayer player = ctx.getSource().getPlayer();
                        if (player == null) {
                            ctx.getSource().sendFailure(Component.literal("Only players can use this command."));
                            return 0;
                        }

                        UnlockedStoreSlotsComponent unlockedSlotsComponent = OPAPCComponents.UNLOCKED_STORE_SLOTS.get(player);
                        int unlockedSlots = unlockedSlotsComponent.getUnlockedSlots();
                        if (unlockedSlots >= OPAPCConfig.maxStoreSlotsPerPlayer) {
                            player.sendSystemMessage(Component.literal("You’ve reached the maximum number of unlocked slots.").withStyle(ChatFormatting.GRAY));
                            return 0;
                        }

                        CurrencyComponent wallet = ModComponents.CURRENCY.get(player);

                        int cost = (unlockedSlots + 1) * 10_000; // 1 gold = 10,000 bronze

                        if (wallet.getValue() >= cost) {
                            wallet.modify(-cost);
                            unlockedSlotsComponent.increment(1);
                            player.sendSystemMessage(Component.literal("Upgraded your available sell slots by 1! It is now " + (unlockedSlots + 1)).withStyle(ChatFormatting.GOLD));
                        } else {
                            CurrencyUtil.CoinBreakdown needed = CurrencyUtil.fromTotalBronze(cost);
                            player.sendSystemMessage(
                                    Component.literal("Not enough funds to upgrade. You need G: " + needed.gold() +
                                                    ", S: " + needed.silver() +
                                                    ", B: " + needed.bronze())
                                            .withStyle(ChatFormatting.RED)
                            );
                        }
                        return 1;
                    })
            );

            tradeCommand.then(literal("profile")
                    .executes(ctx -> {

                        ServerPlayer player = ctx.getSource().getPlayer();
                        if (player == null) {
                            ctx.getSource().sendFailure(Component.literal("Only players can use this command."));
                            return 0;
                        }

                        long total = OPAPCComponents.SELLERS.get(OPAPC.getServer().getScoreboard()).getSellerInfo(player.getUUID()).totalSales();
                        CurrencyUtil.CoinBreakdown coins = CurrencyUtil.fromTotalBronze(total);

                        player.sendSystemMessage(
                                Component.literal("Your total sales are: G: " + coins.gold() + ", S: " + coins.silver() + ", B: " + coins.bronze() + ".")
                        );

                        return 1;
                    })
            );

            tradeCommand.then(literal("top")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayer();
                        if (player == null) {
                            ctx.getSource().sendFailure(Component.literal("Only players can use this command"));
                            return 0;
                        }
                        List<SellerInfo> top = OPAPCComponents.SELLERS
                                .get(OPAPC.getServer().getScoreboard())
                                .getAllSellers()
                                .stream()
                                .sorted(Comparator.comparingLong(SellerInfo::totalSales).reversed())
                                .limit(10)
                                .toList();

                        if (top.isEmpty()) {
                            player.sendSystemMessage(Component.literal("No sellers found."));
                            return 1;
                        }

                        player.sendSystemMessage(Component.literal("Top 10 Sellers:"));
                        for (int i = 0; i < top.size(); i++) {
                            SellerInfo seller = top.get(i);
                            String line = String.format(
                                    "%d. %s - %s",
                                    i + 1,
                                    seller.name(),
                                    CurrencyUtil.formatPrice(seller.totalSales(), false, false).getString()
                            );
                            player.sendSystemMessage(Component.literal(line));
                        }
                        return 1;
                    })
            );

            tradeCommand.then(literal("sell")
                    .then(argument("gold", IntegerArgumentType.integer(0))
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayer();
                                if (player == null) {
                                    ctx.getSource().sendFailure(Component.literal("Only players can use this command"));
                                    return 0;
                                }
                                int gold = IntegerArgumentType.getInteger(ctx, "gold");
                                long price = CurrencyUtil.toTotalBronze(gold, 0, 0);
                                return handleSellCommand(player, price, ctx.getSource().getServer());
                            })
                            .then(argument("silver", IntegerArgumentType.integer(0))
                                    .executes(ctx -> {
                                        ServerPlayer player = ctx.getSource().getPlayer();
                                        if (player == null) {
                                            ctx.getSource().sendFailure(Component.literal("Only players can use this command"));
                                            return 0;
                                        }
                                        int gold = IntegerArgumentType.getInteger(ctx, "gold");
                                        int silver = IntegerArgumentType.getInteger(ctx, "silver");
                                        long price = CurrencyUtil.toTotalBronze(gold, silver, 0);
                                        return handleSellCommand(player, price, ctx.getSource().getServer());
                                    })
                                    .then(argument("bronze", IntegerArgumentType.integer(0))
                                            .executes(ctx -> {
                                                ServerPlayer player = ctx.getSource().getPlayer();
                                                if (player == null) {
                                                    ctx.getSource().sendFailure(Component.literal("Only players can use this command"));
                                                    return 0;
                                                }
                                                int gold = IntegerArgumentType.getInteger(ctx, "gold");
                                                int silver = IntegerArgumentType.getInteger(ctx, "silver");
                                                int bronze = IntegerArgumentType.getInteger(ctx, "bronze");
                                                long price = CurrencyUtil.toTotalBronze(gold, silver, bronze);
                                                return handleSellCommand(player, price, ctx.getSource().getServer());
                                            })
                                    )
                            )
                    )

            );

            commandDispatcher.register(tradeCommand);

        }));
    }

    public static int handleSellCommand(ServerPlayer player, long price, MinecraftServer server) {
        if (price <= 0) {
            player.sendSystemMessage(Component.literal("Price needs to be larger than 0").withStyle(ChatFormatting.RED));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.literal("You're not holding any item to sell.").withStyle(ChatFormatting.RED));
            return 0;
        }

        OffersComponent offers = OPAPCComponents.OFFERS.get(server.getScoreboard());
        long usedSlots = offers.getOffers().values().stream()
                .filter(e -> player.getUUID().equals(e.getOfferId()))
                .count();

        UnlockedStoreSlotsComponent unlockedSlotsComponent = OPAPCComponents.UNLOCKED_STORE_SLOTS.get(player);
        int unlocked = unlockedSlotsComponent.getUnlockedSlots();

        if (unlocked <= usedSlots) {
            player.sendSystemMessage(Component.literal("You don't have any available sell slots left.").withStyle(ChatFormatting.RED));
            return 0;
        }

        IServerPartyAPI party = OpenPACServerAPI.get(server).getPartyManager().getPartyByMember(player.getUUID());

        ItemStack listedItem = stack.copy();
        player.getMainHandItem().setCount(0); // remove the item

        Offer offer = new Offer(
                UUID.randomUUID(),
                player.getUUID(),
                listedItem,
                price,
                (party == null ? null : party.getId())
        );

        offers.addOffer(offer);

        CurrencyUtil.CoinBreakdown bd = CurrencyUtil.fromTotalBronze(price);
        player.sendSystemMessage(Component.literal(String.format(
                "Listed item for %d gold, %d silver, %d bronze.",
                bd.gold(), bd.silver(), bd.bronze()
        )).withStyle(ChatFormatting.GOLD));

        return 1;
    }
}
