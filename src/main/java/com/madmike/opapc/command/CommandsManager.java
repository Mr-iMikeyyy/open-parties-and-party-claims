package com.madmike.opapc.command;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.components.player.trades.UnlockedStoreSlotsComponent;
import com.madmike.opapc.util.CurrencyUtil;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static com.madmike.opapc.command.SellCommandHandler.handleSellCommand;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CommandsManager {
    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("seller")
                    .then(literal("upgrade")
                            .executes(ctx -> {
                                if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) {
                                    ctx.getSource().sendError(Text.literal("Only players can use /upgrade."));
                                    return 0;
                                }

                                handleUpgradeCommand(player, ctx.getSource().getServer());
                                return 1;
                            })
                    )
                    .then(literal("totals")
                            .executes(ctx -> {
                                if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) {
                                    ctx.getSource().sendError(Text.literal("Only players can use /totals."));
                                    return 0;
                                }

                                long totals = OPAPCComponents.SELLERS.get(ctx.getSource().getServer().getScoreboard()).getSales(player.getUuid());
                                CurrencyUtil.CoinBreakdown coins = CurrencyUtil.fromTotalBronze(totals);

                                player.sendMessage(Text.literal("Your total sales are: G: " + coins.gold() + ", S: " + coins.silver() + ", B: " + coins.bronze() + "."));
                                return 1;
                            })
                    )
            );
            dispatcher.register(literal("sell")
                    .then(argument("gold", IntegerArgumentType.integer(0))
                            .executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                int gold = ctx.getArgument("gold", Integer.class);
                                long price = CurrencyUtil.toTotalBronze(gold, 0, 0);
                                return handleSellCommand(player, price, ctx.getSource().getServer());
                            })
                            .then(argument("silver", IntegerArgumentType.integer(0))
                                    .executes(ctx -> {
                                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                                        int gold = ctx.getArgument("gold", Integer.class);
                                        int silver = ctx.getArgument("silver", Integer.class);
                                        long price = CurrencyUtil.toTotalBronze(gold, silver, 0);
                                        return handleSellCommand(player, price, ctx.getSource().getServer());
                                    })
                                    .then(argument("bronze", IntegerArgumentType.integer(0))
                                            .executes(ctx -> {
                                                ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                int gold = ctx.getArgument("gold", Integer.class);
                                                int silver = ctx.getArgument("silver", Integer.class);
                                                int bronze = ctx.getArgument("bronze", Integer.class);
                                                long price = CurrencyUtil.toTotalBronze(gold, silver, bronze);
                                                return handleSellCommand(player, price, ctx.getSource().getServer());
                                            })
                                    )
                            )
                    )
            );
        });
    }



    private static void handleUpgradeCommand(ServerPlayerEntity player, MinecraftServer server) {
        UnlockedStoreSlotsComponent unlockedSlotsComponent = OPAPCComponents.UNLOCKED_STORE_SLOTS.get(player);
        int unlockedSlots = unlockedSlotsComponent.getUnlockedSlots();
        if (unlockedSlots >= MAX_SLOTS) {
            player.sendMessage(Text.literal("You’ve reached the maximum number of unlocked slots.").formatted(Formatting.GRAY));
            return;
        }

        CurrencyComponent wallet = ModComponents.CURRENCY.get(player);

        int cost = (unlockedSlots + 1) * 10000; // 1 gold = 10,000 bronze if using Numismatic default

        if (wallet.getValue() >= cost) {
            wallet.modify(-cost);
            unlockedSlotsComponent.increment(1);
            player.sendMessage(Text.literal("Upgraded your available sell slots by 1! It is now " + unlockedSlots).formatted(Formatting.GOLD));
        } else {
            CurrencyUtil.CoinBreakdown needed = CurrencyUtil.fromTotalBronze(cost);
            player.sendMessage(Text.literal("Not enough funds to upgrade. You need G: " + needed.gold() + ", S: " + needed.silver() + ", B: " + needed.bronze()).formatted(Formatting.RED));
        }
    }
}
