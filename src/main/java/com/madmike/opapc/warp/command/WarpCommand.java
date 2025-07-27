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

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class WarpCommand {
    public static void registerWarpCommand() {
        CommandRegistrationCallback.EVENT.register(((commandDispatcher, commandBuildContext, commandSelection) -> {

            LiteralArgumentBuilder<CommandSourceStack> warpCommand = literal("warp").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayer();
                if (player != null) {
                    player.sendSystemMessage(Component.literal("""
                            §6====== Warp Command Help ======
                            
                            §6--- Only available to Lone Wolves ---
                            §e/warp home §7- Warp to your respawn point
                            §e/warp ambush <party> §7- Warp somewhere random near a party claim
                            
                            §6--- Only available to Party Members ---
                            §e/warp <player> §7- Warp to party members
                            §e/warp guild §7- Warp to your party claim's guild point
                            §e/warp guild set §7- Party leaders can use this to set the guild point
                            """)
                    );
                    return 1;
                }
                return 0;
            });

            warpCommand.then(literal("home")
                    .requires(source -> OPAPC.getPartyManager().getPartyByMember(source.getPlayer().getUUID()) == null)
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

                        //TODO check if in a claim or raid and deny if so
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
                    .requires(source -> OPAPC.getPartyManager().getPartyByMember(source.getPlayer().getUUID()) == null)
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

                                //TODO check if in a claim or raid and deny if so

                                String chosenPartyName = ctx.getArgument("party", String.class);

                                for (Map.Entry<UUID, PartyClaim> claim : OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard()).getAllClaims().entrySet()) {
                                    if (claim.getValue().getPartyName().equalsIgnoreCase(chosenPartyName)) {
                                        IPartyAPI party = OPAPC.getPartyManager().getPartyById(claim.getKey());

                                        UUID ownerId = party.getOwner().getUUID();
                                        IPlayerClaimInfoAPI playerInfo = OPAPC.getClaimsManager().getPlayerInfo(ownerId);

                                        Optional<IPlayerClaimPosListAPI> firstRegion = playerInfo.getDimension(Level.OVERWORLD.location()).getStream();
                                        if (firstRegion.isEmpty()) return 0;

                                        Optional<ChunkPos> firstChunk = firstRegion.get().getStream().findFirst();
                                        if (firstChunk.isEmpty()) return 0;

                                        ChunkPos startingPos = firstChunk.get();


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
