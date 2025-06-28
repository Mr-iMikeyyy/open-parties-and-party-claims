package com.madmike.opapc.command;

import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.data.parties.claims.PartyClaim;
import com.madmike.opapc.util.CurrencyUtil;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import xaero.pac.common.parties.party.api.IPartyAPI;
import xaero.pac.common.parties.party.member.api.IPartyMemberAPI;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import static com.madmike.opapc.command.commands.claims.AbandonCommandHandler.handleAbandonCommand;
import static com.madmike.opapc.command.commands.tele.GuildCommandHandler.handleGuildCommand;
import static com.madmike.opapc.command.commands.tele.HomeCommandHandler.handleHomeCommand;
import static com.madmike.opapc.command.commands.trades.SellCommandHandler.handleSellCommand;
import static com.madmike.opapc.command.commands.trades.Top3CommandHandler.handleTop3Command;
import static com.madmike.opapc.command.commands.trades.TotalsCommandHandler.handleTotalsCommand;
import static com.madmike.opapc.command.commands.trades.UpgradeCommandHandler.handleUpgradeCommand;
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

                                handleTotalsCommand(player, ctx.getSource().getServer());
                                return 1;
                            })
                    )
                    .then(literal("top3"))
                    .executes(ctx -> {
                        if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity player)) {
                            ctx.getSource().sendError(Text.literal("Only players can use /totals."));
                            return 0;
                        }

                        handleTop3Command(player, ctx.getSource().getServer());
                        return 1;
                    })
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
            dispatcher.register(literal("home")
                    .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;

                        IServerPartyAPI party = OpenPACServerAPI.get(ctx.getSource().getServer()).getPartyManager().getPartyByMember(player.getUuid());

                        if (party != null) {
                            player.sendMessage(Text.literal("Only players not in a party have access to the /home command."));
                            return 0;
                        }

                        handleHomeCommand(player);

                        return 1;
                    })
            );
            dispatcher.register(literal("guild")
                    .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;

                        IServerPartyAPI party = OpenPACServerAPI.get(ctx.getSource().getServer()).getPartyManager().getPartyByMember(player.getUuid());

                        if (party == null) {
                            player.sendMessage(Text.literal("Only players that are in a party have access to the /guild command."));
                            return 0;
                        }

                        PartyClaim claim = OPAPCComponents.PARTY_CLAIMS.get(player.getScoreboard()).getClaim(party.getId());
                        if (claim == null) {
                            player.sendMessage(Text.literal("You're party hasn't set up a claim yet. The party leader needs to place a guild block to start a party claim."));
                            return 0;
                        }

                        handleGuildCommand(player, claim, ctx.getSource().getServer());

                        return 1;
                    })
            );
            dispatcher.register(literal("abandon")
                    .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player == null) return 0;

                        player.sendMessage(Text.literal("""
                                §cWarning: Using [/abandon confirm] will:
                                - Destroy your current party claim block
                                - Unclaim your party's land
                                - Delete your party claim progress
                                
                                §eOnly the party leader can perform this action, and you must be near your Party Claim Block."""));

                        return 1;
                    })
                    .then(literal("confirm").executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        MinecraftServer server = ctx.getSource().getServer();
                        if (player == null) return 0;

                        OpenPACServerAPI api = OpenPACServerAPI.get(server);
                        IPartyAPI party = api.getPartyManager().getPartyByMember(player.getUuid());

                        if (party == null) {
                            player.sendMessage(Text.literal("§cYou are not in a party."));
                            return 0;
                        }

                        IPartyMemberAPI owner = party.getOwner();
                        if (!player.getUuid().equals(owner.getUUID())) {
                            player.sendMessage(Text.literal("§cOnly the party leader can abandon the party claim."));
                            return 0;
                        }

                        PartyClaim claim = OPAPCComponents.PARTY_CLAIMS.get(server.getScoreboard()).getClaim(party.getId());
                        if (claim == null || claim.getPcbBlockPos() == null) {
                            player.sendMessage(Text.literal("§cNo active Party Claim Block found."));
                            return 0;
                        }

                        BlockPos playerPos = player.getBlockPos();
                        BlockPos pcbPos = claim.getPcbBlockPos();

                        if (!player.getWorld().getChunk(playerPos).getPos().equals(player.getWorld().getChunk(pcbPos).getPos())) {
                            player.sendMessage(Text.literal("§cYou must be in the same chunk as the Party Claim Block to confirm abandonment."));
                            return 0;
                        }

                        // Handle the abandon logic
                        handleAbandonCommand(owner, party.getId(), api, server);
                        player.sendMessage(Text.literal("§aParty claim abandoned successfully."));
                        return 1;
                    }))
            );
        });
    }
}
