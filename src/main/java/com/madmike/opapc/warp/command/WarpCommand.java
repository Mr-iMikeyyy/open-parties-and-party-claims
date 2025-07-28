package com.madmike.opapc.warp.command;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import com.madmike.opapc.raid.RaidManager;
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
import xaero.pac.common.claims.player.api.IPlayerClaimInfoAPI;
import xaero.pac.common.claims.player.api.IPlayerClaimPosListAPI;
import xaero.pac.common.parties.party.api.IPartyAPI;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.*;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class WarpCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register(((commandDispatcher, commandBuildContext, commandSelection) -> {

            LiteralArgumentBuilder<CommandSourceStack> warpCommand = literal("warp").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayer();
                if (player != null) {
                    player.sendSystemMessage(Component.literal("""
                            §6====== Warp Command Help ======
                            
                            §6--- Only available to Lone Wolves ---
                            §e/warp home §7- Warp to your respawn point
                            §e/warp ambush <party> §7- Warp outside a party claim
                            
                            §6--- Only available to Party Members ---
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
                        if (OPAPCComponents.COMBAT_TIMER.get(player).isInCombat()) {
                            player.sendSystemMessage(Component.literal("Your still in combat! Please wait " + OPAPCComponents.COMBAT_TIMER.get(player).getRemainingTimeSeconds() + " seconds!"));
                            return 0;
                        }

                        //Ensure not on cooldown
                        if (OPAPCComponents.WARP_TIMER.get(player).hasCooldown()) {
                            player.sendSystemMessage(Component.literal("You are on cooldown from teleporting. Please wait " + OPAPCComponents.WARP_TIMER.get(player).getFormattedRemainingTime() + "."));
                            return 0;
                        }

                        //TODO check if in a raid and deny if so
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
                        OPAPCComponents.WARP_TIMER.get(player).onWarp();
                        return 1;
                    })
            );


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
                                for (PartyClaim claim : OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard()).getAllClaims().values()) {
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
                                if (OPAPCComponents.COMBAT_TIMER.get(player).isInCombat()) {
                                    player.sendSystemMessage(Component.literal("Your still in combat! Please wait " + OPAPCComponents.COMBAT_TIMER.get(player).getRemainingTimeSeconds() + " seconds!"));
                                    return 0;
                                }

                                //Ensure not on cooldown
                                if (OPAPCComponents.WARP_TIMER.get(player).hasCooldown()) {
                                    player.sendSystemMessage(Component.literal("You are on cooldown from teleporting. Please wait " + OPAPCComponents.WARP_TIMER.get(player).getFormattedRemainingTime() + "."));
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

        }));
    }
}
