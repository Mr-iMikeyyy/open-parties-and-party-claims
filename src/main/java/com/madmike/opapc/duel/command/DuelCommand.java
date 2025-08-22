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

package com.madmike.opapc.duel.command;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.duel.DuelManager;
import com.madmike.opapc.duel.components.scoreboard.DuelMapsComponent;
import com.madmike.opapc.duel.data.DuelMap;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.List;
import java.util.Locale;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class DuelCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, selection) -> {

            LiteralArgumentBuilder<CommandSourceStack> duelCommand = literal("duel").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayer();
                if (player != null) {
                    player.sendSystemMessage(Component.literal("""
                        §6====== Duel Command Help ======

                        §e/duel challenge <player> <map> <wager> §7- Challenge a player to a Duel, wager (in gold) is optional
                        §e/duel cancel §7- Cancel your outgoing Duel challenges
                        §e/duel accept §7- Accept your latest incoming challenge
                        §e/duel deny §7- Deny your latest incoming challenge
                        §e/duel stats §7- View your Duel stats
                        §e/duel top §7- View top performing duelers
                        
                        §6--- Rules ---
                        
                        §7• Be the first to 3 kills, winner takes all
                        §7• You cannot challenge party members to duels
                        
                        §6--- Server Owners ---
                        
                        §e/duel maps add <mapName> §7- Add a map
                        §e/duel maps rename <currentName> <newName> §7- Rename an existing map
                        §e/duel maps spawn add <mapName> §7- Adds a spawn point where you are standing (balances sides)
                        §e/duel maps spawn remove <mapName> <index> §7- Removes the spawn index from both sides if present
                        """));
                }
                return 1;
            });

            /* ========== /duel challenge <player> <map> [wager] ========== */

            duelCommand.then(literal("challenge")
                    .then(argument("player", StringArgumentType.string())
                            .suggests((context, builder) -> {
                                ServerPlayer self = context.getSource().getPlayer();
                                if (self == null) return builder.buildFuture();

                                var party = OPAPC.getPartyManager().getPartyByMember(self.getUUID());
                                for (ServerPlayer other : OPAPC.getServer().getPlayerList().getPlayers()) {
                                    if (other.getUUID().equals(self.getUUID())) continue;
                                    if (party != null) {
                                        var otherParty = OPAPC.getPartyManager().getPartyByMember(other.getUUID());
                                        if (otherParty != null && otherParty.getId().equals(party.getId())) {
                                            continue; // skip party members
                                        }
                                    }
                                    builder.suggest(other.getName().getString());
                                }
                                return builder.buildFuture();
                            })
                            .then(argument("map", StringArgumentType.string())
                                    .suggests((context, builder) -> {
                                        var server = OPAPC.getServer();
                                        if (server == null) return builder.buildFuture();

                                        DuelMapsComponent comp = OPAPCComponents.DUEL_MAPS.get(server.getScoreboard());
                                        if (!comp.getAll().isEmpty()) {
                                            for (DuelMap map : comp.getAll()) {
                                                if (!map.getPlayer1Spawns().isEmpty() && !map.getPlayer2Spawns().isEmpty()) {
                                                    builder.suggest(map.getName());
                                                }
                                            }
                                        }
                                        builder.suggest("Random");
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> executeChallenge(ctx.getSource(),
                                            StringArgumentType.getString(ctx, "player"),
                                            StringArgumentType.getString(ctx, "map"),
                                            0L))
                                    .then(argument("wager", IntegerArgumentType.integer(0))
                                            .executes(ctx -> executeChallenge(ctx.getSource(),
                                                    StringArgumentType.getString(ctx, "player"),
                                                    StringArgumentType.getString(ctx, "map"),
                                                    IntegerArgumentType.getInteger(ctx, "wager"))))
                            )
                    )
            );

            /* ========== /duel accept | /duel deny | /duel cancel ========== */

            duelCommand.then(literal("accept").executes(ctx -> {
                var server = OPAPC.getServer();
                ServerPlayer self = ctx.getSource().getPlayer();
                if (server == null || self == null) return 0;

                var res = DuelManager.INSTANCE.acceptLatestFor(self, server, RandomSource.create());
                self.sendSystemMessage(Component.literal(res.message));
                return res.started ? 1 : 0;
            }));

            duelCommand.then(literal("deny").executes(ctx -> {
                ServerPlayer self = ctx.getSource().getPlayer();
                if (self == null) return 0;
                int removed = DuelManager.INSTANCE.denyIncomingChallenges(self.getUUID());
                self.sendSystemMessage(Component.literal(removed > 0
                        ? "Denied pending challenge(s)."
                        : "You have no pending challenges."));
                return removed > 0 ? 1 : 0;
            }));

            duelCommand.then(literal("cancel").executes(ctx -> {
                ServerPlayer self = ctx.getSource().getPlayer();
                if (self == null) return 0;
                int removed = DuelManager.INSTANCE.cancelOutgoingChallenges(self.getUUID());
                self.sendSystemMessage(Component.literal(removed > 0
                        ? "Canceled your outgoing challenge(s)."
                        : "You have no outgoing challenges."));
                return removed > 0 ? 1 : 0;
            }));

            /* ========== /duel stats | /duel top (placeholders) ========== */

            duelCommand.then(literal("stats").executes(ctx -> {
                ServerPlayer self = ctx.getSource().getPlayer();
                if (self != null) {
                    self.sendSystemMessage(OPAPCComponents.DUEL_STATS.get(self).getPrintedStats());
                }
                return 1;
            }));

            duelCommand.then(literal("top").executes(ctx -> {
                ServerPlayer self = ctx.getSource().getPlayer();
                if (self != null) {
                    self.sendSystemMessage(Component.literal("Duel leaderboard coming soon."));
                }
                return 1;
            }));

            /* ========== /duel maps ... (admin) ========== */

            var maps = literal("maps");

            // /duel maps add <mapName>
            maps.then(literal("add")
                    .then(argument("mapName", StringArgumentType.string())
                            .executes(ctx -> {
                                var server = OPAPC.getServer();
                                ServerPlayer self = ctx.getSource().getPlayer();
                                if (server == null || self == null) return 0;

                                DuelMapsComponent comp = OPAPCComponents.DUEL_MAPS.get(server.getScoreboard());
                                String name = StringArgumentType.getString(ctx, "mapName");

                                if (comp.exists(name)) {
                                    self.sendSystemMessage(Component.literal("A map with that name already exists."));
                                    return 0;
                                }
                                comp.add(new DuelMap(name), false);
                                self.sendSystemMessage(Component.literal("Added duel map: " + name));
                                return 1;
                            })
                    )
            );

            // /duel maps rename <currentName> <newName>
            maps.then(literal("rename")
                    .then(argument("currentName", StringArgumentType.string())
                            .then(argument("newName", StringArgumentType.string())
                                    .executes(ctx -> {
                                        var server = OPAPC.getServer();
                                        ServerPlayer self = ctx.getSource().getPlayer();
                                        if (server == null || self == null) return 0;

                                        DuelMapsComponent comp = OPAPCComponents.DUEL_MAPS.get(server.getScoreboard());
                                        String cur = StringArgumentType.getString(ctx, "currentName");
                                        String neu = StringArgumentType.getString(ctx, "newName");

                                        var opt = comp.get(cur);
                                        if (opt.isEmpty()) {
                                            self.sendSystemMessage(Component.literal("Map not found: " + cur));
                                            return 0;
                                        }
                                        DuelMap m = opt.get();
                                        comp.remove(cur);
                                        m.setName(neu);
                                        comp.add(m, false);

                                        self.sendSystemMessage(Component.literal("Renamed map '" + cur + "' to '" + neu + "'."));
                                        return 1;
                                    })
                            )
                    )
            );

            // /duel maps spawn add <mapName>
            maps.then(literal("spawn")
                    .then(literal("add")
                            .then(argument("mapName", StringArgumentType.string())
                                    .suggests((context, builder) -> suggestReadyMaps(builder))
                                    .executes(ctx -> {
                                        var server = OPAPC.getServer();
                                        ServerPlayer self = ctx.getSource().getPlayer();
                                        if (server == null || self == null) return 0;

                                        DuelMapsComponent comp = OPAPCComponents.DUEL_MAPS.get(server.getScoreboard());
                                        String mapName = StringArgumentType.getString(ctx, "mapName");
                                        var opt = comp.get(mapName);
                                        if (opt.isEmpty()) {
                                            self.sendSystemMessage(Component.literal("Map not found: " + mapName));
                                            return 0;
                                        }
                                        DuelMap m = opt.get();

                                        BlockPos here = self.blockPosition();
                                        // Balance: add to the shorter side to keep pairs aligned
                                        if (m.getPlayer1Spawns().size() <= m.getPlayer2Spawns().size()) {
                                            m.addPlayer1Spawn(here);
                                            self.sendSystemMessage(Component.literal("Added spawn to P1 for map '" + m.getName() + "' at " + posStr(here)));
                                        } else {
                                            m.addPlayer2Spawn(here);
                                            self.sendSystemMessage(Component.literal("Added spawn to P2 for map '" + m.getName() + "' at " + posStr(here)));
                                        }
                                        // Replace in component to ensure persistence/index
                                        comp.add(m, true);
                                        return 1;
                                    })
                            )
                    )
                    // /duel maps spawn remove <mapName> <index>
                    .then(literal("remove")
                            .then(argument("mapName", StringArgumentType.string())
                                    .suggests((context, builder) -> suggestReadyMaps(builder))
                                    .then(argument("index", IntegerArgumentType.integer(0))
                                            .executes(ctx -> {
                                                var server = OPAPC.getServer();
                                                ServerPlayer self = ctx.getSource().getPlayer();
                                                if (server == null || self == null) return 0;

                                                DuelMapsComponent comp = OPAPCComponents.DUEL_MAPS.get(server.getScoreboard());
                                                String mapName = StringArgumentType.getString(ctx, "mapName");
                                                int idx = IntegerArgumentType.getInteger(ctx, "index");

                                                var opt = comp.get(mapName);
                                                if (opt.isEmpty()) {
                                                    self.sendSystemMessage(Component.literal("Map not found: " + mapName));
                                                    return 0;
                                                }
                                                DuelMap m = opt.get();

                                                boolean removed = false;
                                                if (idx >= 0 && idx < m.getPlayer1Spawns().size()) {
                                                    m.getPlayer1Spawns().remove(idx);
                                                    removed = true;
                                                }
                                                if (idx >= 0 && idx < m.getPlayer2Spawns().size()) {
                                                    m.getPlayer2Spawns().remove(idx);
                                                    removed = true;
                                                }

                                                if (!removed) {
                                                    self.sendSystemMessage(Component.literal("No spawn at index " + idx + " on map '" + m.getName() + "'."));
                                                    return 0;
                                                }

                                                comp.add(m, true);
                                                self.sendSystemMessage(Component.literal("Removed spawn index " + idx + " from map '" + m.getName() + "'."));
                                                return 1;
                                            })
                                    )
                            )
                    )
            );

            duelCommand.then(maps);

            dispatcher.register(duelCommand);
        });
    }

    /* ================= helpers ================= */

    private static int executeChallenge(CommandSourceStack src, String targetName, String mapName, long wagerInt) {
        var server = OPAPC.getServer();
        ServerPlayer challenger = src.getPlayer();
        if (server == null || challenger == null) return 0;

        ServerPlayer opponent = findPlayerCaseInsensitive(targetName);
        if (opponent == null) {
            challenger.sendSystemMessage(Component.literal("Player not found: " + targetName));
            return 0;
        }

        // Enforce "no party member" rule (safety net; suggestions already filter)
        var myParty = OPAPC.getPartyManager().getPartyByMember(challenger.getUUID());
        if (myParty != null) {
            var otherParty = OPAPC.getPartyManager().getPartyByMember(opponent.getUUID());
            if (otherParty != null && otherParty.getId().equals(myParty.getId())) {
                challenger.sendSystemMessage(Component.literal("You cannot challenge party members to duels."));
                return 0;
            }
        }

        long wager = Math.max(0L, wagerInt);

        var res = DuelManager.INSTANCE.requestChallenge(challenger, opponent, mapName, wager);
        if (!res.ok()) {
            challenger.sendSystemMessage(Component.literal(res.message()));
            return 0;
        }

        String niceMap = mapName == null ? "Random" : mapName;
        String msgChallenger = String.format(Locale.ROOT,
                "Challenge sent to %s · Map: %s · Wager: %d · Expires in 30s.",
                opponent.getName().getString(), niceMap, wager);
        String msgOpponent = String.format(Locale.ROOT,
                "%s challenged you to a duel · Map: %s · Wager: %d · Use /duel accept or /duel deny.",
                challenger.getName().getString(), niceMap, wager);

        challenger.sendSystemMessage(Component.literal(msgChallenger));
        opponent.sendSystemMessage(Component.literal(msgOpponent));
        return 1;
    }

    private static com.mojang.brigadier.suggestion.SuggestionsBuilder suggestReadyMaps(com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        var server = OPAPC.getServer();
        if (server == null) return builder;
        DuelMapsComponent comp = OPAPCComponents.DUEL_MAPS.get(server.getScoreboard());
        if (comp != null) {
            for (DuelMap map : comp.getAll()) {
                if (!map.getPlayer1Spawns().isEmpty() && !map.getPlayer2Spawns().isEmpty()) {
                    builder.suggest(map.getName());
                }
            }
        }
        builder.suggest("Random");
        return builder;
    }

    private static ServerPlayer findPlayerCaseInsensitive(String name) {
        for (ServerPlayer p : OPAPC.getServer().getPlayerList().getPlayers()) {
            if (p.getName().getString().equalsIgnoreCase(name)) return p;
        }
        return null;
    }

    private static String posStr(BlockPos p) {
        return "(" + p.getX() + ", " + p.getY() + ", " + p.getZ() + ")";
    }
}
