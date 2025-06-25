package com.madmike.opapc.command;

import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.util.CurrencyUtil;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static com.madmike.opapc.command.SellCommandHandler.handleSellCommand;
import static com.madmike.opapc.command.TotalsCommandHandler.handleTotalsCommand;
import static com.madmike.opapc.command.UpgradeCommandHandler.handleUpgradeCommand;
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

                                handleTotalsCommand(player, player.getServer());
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
}
