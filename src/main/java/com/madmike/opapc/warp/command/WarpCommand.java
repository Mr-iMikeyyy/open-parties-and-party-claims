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

package com.madmike.opapc.warp.command;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import com.madmike.opapc.raid.RaidManager;
import com.madmike.opapc.raid.data.RaidData;
import com.madmike.opapc.util.SafeWarpHelper;
import com.madmike.opapc.war.War;
import com.madmike.opapc.war.WarManager;
import com.madmike.opapc.war.data.WarData;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import xaero.pac.common.parties.party.ally.api.IPartyAllyAPI;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.*;

import static com.madmike.opapc.util.CommandFailureHandler.fail;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class WarpCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register(((commandDispatcher, commandBuildContext, commandSelection) -> {

            //region Warp
            LiteralArgumentBuilder<CommandSourceStack> warpCommand = literal("warp").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayer();
                if (player != null) {
                    player.sendSystemMessage(Component.literal("""
                            §6====== Warp Command Help ======
                            
                            §6--- If you are not in a party ---
                            §e/warp home §7- Warp to your respawn point
                            §e/warp ambush <party> §7- Warp outside any party claim
                            
                            §6--- If you are in a party ---
                            §e/warp <player> §7- Warp to a party member
                            §e/warp party §7- Warp to your party's claim
                            §e/warp <ally> §7- Warp to an ally's claim
                            
                            §6--- Only available to Party Leaders ---
                            §e/warp party set §7- Set your party's warp point
                            """)
                    );
                    return 1;
                }
                return 0;
            });
            //endregion

            //region Home
            warpCommand.then(literal("home")
                    .requires(ctx -> {
                        //Ensure no party
                        ServerPlayer player = ctx.getPlayer();
                        if (player != null) {
                            return OPAPC.getPartyManager().getPartyByMember(player.getUUID()) == null;
                        }
                        return false;
                    })
                    .executes(ctx -> {


                        ServerPlayer player = ctx.getSource().getPlayer();

                        //Check if player
                        if (player == null) return 0;

                        IServerPartyAPI party = OpenPACServerAPI.get(ctx.getSource().getServer())
                                .getPartyManager().getPartyByMember(player.getUUID());

                        //Ensure not in party
                        if (party != null) {
                            player.sendSystemMessage(Component.literal("Only players not in a party have access to the /home command."));
                            return 0;
                        }

                        //Ensure not in combat
                        if (OPAPCComponents.COMBAT_COOLDOWN.get(player).isInCombat()) {
                            player.sendSystemMessage(Component.literal("Your still in combat! Please wait " + OPAPCComponents.COMBAT_COOLDOWN.get(player).getRemainingTimeSeconds() + " seconds!"));
                            return 0;
                        }

                        //Ensure not on cooldown
                        if (OPAPCComponents.WARP_COOLDOWN.get(player).hasCooldown()) {
                            player.sendSystemMessage(Component.literal("You are on cooldown from teleporting. Please wait " + OPAPCComponents.WARP_COOLDOWN.get(player).getFormattedRemainingTime() + "."));
                            return 0;
                        }

                        //Check if in raid
                        if (RaidManager.INSTANCE.isPlayerInRaid(player.getUUID())) {
                            player.sendSystemMessage(Component.literal("You cannot use /home while in a raid!"));
                            return 0;
                        }

                        BlockPos respawnPos = player.getRespawnPosition();
                        ResourceKey<Level> respawnDimension = player.getRespawnDimension();

                        if (respawnPos == null) {
                            respawnPos = player.serverLevel().getSharedSpawnPos();
                            respawnDimension = player.serverLevel().dimension();
                        }

                        MinecraftServer server = player.getServer();
                        if (server == null) {
                            return 0;
                        }

                        ServerLevel targetWorld = server.getLevel(respawnDimension);
                        if (targetWorld != null) {
                            player.teleportTo(
                                    targetWorld,
                                    respawnPos.getX() + 0.5,
                                    respawnPos.getY(),
                                    respawnPos.getZ() + 0.5,
                                    player.getYRot(),
                                    player.getXRot()
                            );
                        }
                        OPAPCComponents.WARP_COOLDOWN.get(player).onWarp();
                        return 1;
                    })
            );
            //endregion

            //region Ambush
            warpCommand.then(literal("ambush")
                    .requires(ctx -> {
                        ServerPlayer player = ctx.getPlayer();
                        if (player != null) {
                            return OPAPC.getPartyManager().getPartyByMember(player.getUUID()) == null;
                        }
                        return false;
                    })
                    .then(argument("party", StringArgumentType.string())
                            .suggests((ctx, builder) -> {
                                for (PartyClaim claim : OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard()).getAllClaims()) {
                                    builder.suggest(claim.getPartyName());
                                }
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {

                                ServerPlayer player = ctx.getSource().getPlayer();

                                //Check if player
                                if (player == null) return 0;

                                IServerPartyAPI party = OPAPC.getPartyManager().getPartyByMember(player.getUUID());

                                //Ensure not in party
                                if (party != null) {
                                    player.sendSystemMessage(Component.literal("Only players without a party have access to the /home command."));
                                    return 0;
                                }

                                //Ensure not in combat
                                if (OPAPCComponents.COMBAT_COOLDOWN.get(player).isInCombat()) {
                                    player.sendSystemMessage(Component.literal("Your still in combat! Please wait " + OPAPCComponents.COMBAT_COOLDOWN.get(player).getRemainingTimeSeconds() + " seconds!"));
                                    return 0;
                                }

                                //Ensure not on cooldown
                                if (OPAPCComponents.WARP_COOLDOWN.get(player).hasCooldown()) {
                                    player.sendSystemMessage(Component.literal("You are on cooldown from teleporting. Please wait " + OPAPCComponents.WARP_COOLDOWN.get(player).getFormattedRemainingTime() + "."));
                                    return 0;
                                }

                                //Check if in raid
                                if (RaidManager.INSTANCE.playerIsInRaid(player.getUUID())) {
                                    player.sendSystemMessage(Component.literal("You can't warp while in a raid."));
                                    return 0;
                                }

                                //All Checks passed, warp player outside party claim
                                String chosenPartyName = ctx.getArgument("party", String.class);
                                for (Map.Entry<UUID, PartyClaim> claim : OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard()).getAllClaims().entrySet()) {
                                    if (claim.getValue().getPartyName().equalsIgnoreCase(chosenPartyName)) {

                                        List<ChunkPos> claimedChunks = claim.getValue().getClaimedChunksList();

                                        Random rand = new Random();
                                        ChunkPos randomChunk = claimedChunks.get(rand.nextInt(claimedChunks.size()));

                                        int direction = rand.nextInt(4);
                                        boolean goodChunkNotFound = true;


                                        switch (direction) {
                                            case 0:
                                                while (goodChunkNotFound) {
                                                    
                                                }
                                        }
                                        break;
                                    }
                                }

                                player.sendSystemMessage(Component.literal("Problem occurred trying to find the claim given"));
                                return 0;
                            })
                    )
            );
            //endregion

            //region Player
            warpCommand.then(argument("player", StringArgumentType.string())
                    .requires(ctx -> {
                        ServerPlayer player = ctx.getPlayer();
                        if (player != null) {
                            return OPAPC.getPartyManager().getPartyByMember(player.getUUID()) != null;
                        }
                        return false;
                    })
                    .suggests((ctx, builder) -> {

                        ServerPlayer player = ctx.getSource().getPlayer();
                        if (player == null) {
                            return builder.buildFuture();
                        }

                        IServerPartyAPI party = OPAPC.getPartyManager().getPartyByMember(player.getUUID());
                        if (party == null) return builder.buildFuture();

                        List<ServerPlayer> onlinePlayers = party.getOnlineMemberStream().toList();
                        for (ServerPlayer onlinePlayer : onlinePlayers) {
                            builder.suggest(onlinePlayer.getGameProfile().getName());
                        }

                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayer();
                        if (player == null) {
                            ctx.getSource().sendSystemMessage(Component.literal("Must be a player to use this command"));
                            return 0;
                        }

                        //Ensure not in combat
                        if (OPAPCComponents.COMBAT_COOLDOWN.get(player).isInCombat()) {
                            player.sendSystemMessage(Component.literal("Your still in combat! Please wait " + OPAPCComponents.COMBAT_COOLDOWN.get(player).getRemainingTimeSeconds() + " seconds!"));
                            return 0;
                        }

                        //Ensure not on cooldown
                        if (OPAPCComponents.WARP_COOLDOWN.get(player).hasCooldown()) {
                            player.sendSystemMessage(Component.literal("You are on cooldown from teleporting. Please wait " + OPAPCComponents.WARP_COOLDOWN.get(player).getFormattedRemainingTime() + "."));
                            return 0;
                        }

                        IServerPartyAPI party = OPAPC.getPartyManager().getPartyByMember(player.getUUID());
                        if (party == null) {
                            player.sendSystemMessage(Component.literal("Only players that are in a party can use this command"));
                            return 0;
                        }

                        if (WarManager.INSTANCE.isWarActive()) {
                            if (WarManager.INSTANCE.findWarByParty(party) != null) {
                                return fail(player, "You cannot warp to a party member during a war!");
                            }
                        }

                        for (RaidData raid : RaidManager.INSTANCE.getActiveRaids()) {
                            if (raid.getDefendingParty().equals(party)) {
                                player.sendSystemMessage(Component.literal("Cannot teleport to party members while your base is being raided"));
                                return 0;
                            }
                        }

                        String targetName = ctx.getArgument("player", String.class);

                        for (ServerPlayer onlinePlayer : party.getOnlineMemberStream().toList()) {
                            if (onlinePlayer.getGameProfile().getName().equals(targetName)) {
                                if (OPAPCComponents.COMBAT_COOLDOWN.get(onlinePlayer).isInCombat()) {
                                    BlockPos blockPos = onlinePlayer.blockPosition();
                                    player.teleportTo(onlinePlayer.serverLevel(), blockPos.getX(), blockPos.getY(), blockPos.getZ(), onlinePlayer.getYRot(), onlinePlayer.getXRot());
                                }
                                else {
                                    player.sendSystemMessage(Component.literal("That player is in combat!"));
                                    return 0;
                                }
                            }
                        }

                        player.sendSystemMessage(Component.literal("Could not find target player"));
                        return 0;
                    })
            );
            //endregion

            //region Party
            warpCommand.then(literal("party")
                    .requires(ctx -> {
                        ServerPlayer player = ctx.getPlayer();
                        if (player != null) {
                            return OPAPC.getPartyManager().getPartyByMember(player.getUUID()) != null;
                        }
                        return false;
                    })
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayer();
                        if (player == null) {
                            ctx.getSource().sendSystemMessage(Component.literal("Must be a player to use this command"));
                            return 0;
                        }

                        IServerPartyAPI party = OPAPC.getPartyManager().getPartyByMember(player.getUUID());
                        if (party == null) {
                            player.sendSystemMessage(Component.literal("Only players that are in a party can use this command"));
                            return 0;
                        }


                        if (WarManager.INSTANCE.isWarActive()) {
                            War war = WarManager.INSTANCE.findWarByParty(party);
                            if (war != null) {
                                if (war.getData().getDefenderIds().contains(player.getUUID())) {
                                    SafeWarpHelper.warpPlayerToOverworldPos(player, war.getData().getDefendingClaim().getWarpPos());
                                    return 1;
                                }
                                else {
                                    return fail(player, "You can't warp to your party claim while attacking another claim.");
                                }
                            }
                        }

                        for (RaidData raid : RaidManager.INSTANCE.getActiveRaids()) {
                            if (raid.getDefendingParty().equals(party)) {
                                BlockPos warpPos = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard()).getClaim(party.getId()).getWarpPos();
                                player.teleportTo(OPAPC.getServer().overworld(), warpPos.getX(), warpPos.getY(), warpPos.getZ(), player.getYRot(), player.getXRot());
                                return 1;
                            }
                        }

                        //Ensure not in combat
                        if (OPAPCComponents.COMBAT_COOLDOWN.get(player).isInCombat()) {
                            player.sendSystemMessage(Component.literal("Your still in combat! Please wait " + OPAPCComponents.COMBAT_COOLDOWN.get(player).getRemainingTimeSeconds() + " seconds!"));
                            return 0;
                        }

                        //Ensure not on cooldown
                        if (OPAPCComponents.WARP_COOLDOWN.get(player).hasCooldown()) {
                            player.sendSystemMessage(Component.literal("You are on cooldown from teleporting. Please wait " + OPAPCComponents.WARP_COOLDOWN.get(player).getFormattedRemainingTime() + "."));
                            return 0;
                        }

                        BlockPos warpPos = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard()).getClaim(party.getId()).getWarpPos();
                        player.teleportTo(OPAPC.getServer().overworld(), warpPos.getX() + 0.5, warpPos.getY(), warpPos.getZ() + 0.5, player.getYRot(), player.getXRot());

                        return 1;
                    })
                    .then(literal("set")
                            .requires(ctx -> {
                                ServerPlayer player = ctx.getPlayer();
                                if (player == null) {
                                    return false;
                                }

                                IServerPartyAPI party = OPAPC.getPartyManager().getPartyByOwner(player.getUUID());
                                if (party == null) {
                                    return false;
                                }

                                return OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard()).getClaim(party.getId()) != null;
                            })
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayer();
                                if (player == null) {
                                    ctx.getSource().sendSystemMessage(Component.literal("Must be a player to use this command"));
                                    return 0;
                                }

                                IServerPartyAPI party = OPAPC.getPartyManager().getPartyByOwner(player.getUUID());
                                if (party == null) {
                                    player.sendSystemMessage(Component.literal("Only party leaders can use this command"));
                                    return 0;
                                }

                                PartyClaim claim = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard()).getClaim(party.getId());
                                if (claim == null) {
                                    player.sendSystemMessage(Component.literal("Only parties with claims can make a warp point"));
                                    return 0;
                                }

                                ChunkPos chunk = player.chunkPosition();
                                if (!claim.getClaimedChunksList().contains(chunk)) {
                                    player.sendSystemMessage(Component.literal("You cannot set the warp point outside of your claim"));
                                    return 0;
                                }

                                for (WarData war : WarManager.INSTANCE.getActiveWars()) {
                                    if (war.getDefendingClaim().equals(claim) || war.getAttackingClaim().equals(claim)) {
                                        player.sendSystemMessage(Component.literal("You cannot set the warp point while in a war"));
                                        return 0;
                                    }
                                }

                                for (RaidData raid : RaidManager.INSTANCE.getActiveRaids()) {
                                    if (raid.getDefendingClaim().equals(claim)) {
                                        player.sendSystemMessage(Component.literal("You cannot set the warp point while your claim is being raided"));
                                        return 0;
                                    }
                                }

                                claim.setWarpPos(player.blockPosition());
                                return 1;
                            })
                    )
            );

            //endregion

            //region Ally
            warpCommand.then(argument("ally", StringArgumentType.string())
                    .requires(ctx -> {
                        ServerPlayer player = ctx.getPlayer();
                        if (player != null) {
                            return OPAPC.getPartyManager().getPartyByMember(player.getUUID()) != null;
                        }
                        return false;
                    })
                    .suggests((ctx, builder) -> {

                        ServerPlayer player = ctx.getSource().getPlayer();
                        if (player == null) {
                            return builder.buildFuture();
                        }

                        IServerPartyAPI party = OPAPC.getPartyManager().getPartyByMember(player.getUUID());
                        if (party == null) return builder.buildFuture();

                        List<IPartyAllyAPI> allyParties = party.getAllyPartiesStream().toList();
                        for (IPartyAllyAPI allyParty : allyParties) {
                            PartyClaim claim = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard()).getClaim(allyParty.getPartyId());
                            if (claim != null) {
                                builder.suggest(claim.getPartyName());
                            }
                        }

                        return builder.buildFuture();
                    })
                    .executes(ctx -> {

                        ServerPlayer player = ctx.getSource().getPlayer();
                        if (player == null) {
                            ctx.getSource().sendSystemMessage(Component.literal("Must be a player to use this command"));
                            return 0;
                        }

                        IServerPartyAPI party = OPAPC.getPartyManager().getPartyByMember(player.getUUID());
                        if (party == null) {
                            player.sendSystemMessage(Component.literal("Only players that are in a party can use this command"));
                            return 0;
                        }

                        String allyPartyName = ctx.getArgument("ally", String.class);

                        PartyClaim targetClaim = null;

                        for (PartyClaim claim : OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard()).getAllClaims()) {
                            if (claim.getPartyName().equals(allyPartyName)) {
                                targetClaim = claim;
                                break;
                            }
                        }

                        if (targetClaim == null) {
                            player.sendSystemMessage(Component.literal("Could not find that claim"));
                            return 0;
                        }

                        BlockPos warpPos = targetClaim.getWarpPos();

                        for (WarData war : WarManager.INSTANCE.getActiveWars()) {
                            if (war.getDefendingClaim().equals(targetClaim)) {
                                player.teleportTo(OPAPC.getServer().overworld(), warpPos.getX() + 0.5, warpPos.getY(), warpPos.getZ() + 0.5, player.getYRot(), player.getXRot());
                                return 1;
                            }
                        }

                        for (RaidData raid : RaidManager.INSTANCE.getActiveRaids()) {
                            if (raid.getDefendingClaim().equals(targetClaim)) {
                                player.sendSystemMessage(Component.literal("That claim is being raided!"));
                                return 0;
                            }
                        }

                        //Ensure not in combat
                        if (OPAPCComponents.COMBAT_COOLDOWN.get(player).isInCombat()) {
                            player.sendSystemMessage(Component.literal("Your still in combat! Please wait " + OPAPCComponents.COMBAT_COOLDOWN.get(player).getRemainingTimeSeconds() + " seconds!"));
                            return 0;
                        }

                        //Ensure not on cooldown
                        if (OPAPCComponents.WARP_COOLDOWN.get(player).hasCooldown()) {
                            player.sendSystemMessage(Component.literal("You are on cooldown from teleporting. Please wait " + OPAPCComponents.WARP_COOLDOWN.get(player).getFormattedRemainingTime() + "."));
                            return 0;
                        }

                        player.teleportTo(OPAPC.getServer().overworld(), warpPos.getX() + 0.5, warpPos.getY(), warpPos.getZ() + 0.5, player.getYRot(), player.getXRot());
                        return 1;
                    })
            );
            //endregion

            commandDispatcher.register(warpCommand);

        }));
    }
}
