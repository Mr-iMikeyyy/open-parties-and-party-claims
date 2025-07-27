package com.madmike.opapc.partyclaim.command;

import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.partyclaim.components.scoreboard.PartyClaimsComponent;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import com.madmike.opapc.util.ClaimAdjacencyChecker;
import com.madmike.opapc.war.WarManager;
import com.madmike.opapc.war.data.WarData;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.claims.player.api.IPlayerClaimPosListAPI;
import xaero.pac.common.claims.result.api.ClaimResult;
import xaero.pac.common.server.claims.player.api.IServerPlayerClaimInfoAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.List;

import static com.madmike.opapc.util.NetherClaimAdjuster.mirrorOverworldClaimsToNether;
import static net.minecraft.commands.Commands.literal;

public class PartyClaimCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register(((commandDispatcher, commandBuildContext, commandSelection) -> {
            LiteralArgumentBuilder<CommandSourceStack> partyClaimCommand = literal("partyclaim").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayer();
                if (player != null) {
                    player.sendSystemMessage(Component.literal("""
                            §6====== Party Claim Command Help ======
                            
                            §6--- Only available to Party Leaders ---
                            §e/partyclaim claim §7- Attempt to claim a chunk
                            §e/partyclaim unclaim §7- Attempt to un-claim a chunk
                            §e/partyclaim abandon confirm §7- Erase entirely your party claim
                            
                            §6--- Only available to Party Members ---
                            §e/partyclaim info §7- Get details of your party's claim
                            §e/partyclaim donate §7- Increase your party's max claims by 1
                            """)
                    );
                    return 1;
                }
                return 0;
            });

            partyClaimCommand.then(literal("claim")
                    .requires(ctx -> {
                        ServerPlayer player = ctx.getPlayer();
                        if (player != null) {
                            return OPAPC.getPartyManager().getPartyByOwner(player.getUUID()) != null;
                        }
                        return false;
                    })
                    .executes(ctx -> {

                        ServerPlayer player = ctx.getSource().getPlayer();
                        //Check if player
                        if (player == null) {
                            ctx.getSource().sendFailure(Component.literal("This command must be run by a player."));
                            return 0;
                        }

                        //Check if in overworld
                        if (!player.level().dimension().location().equals(Level.OVERWORLD.location())) {
                            ctx.getSource().sendFailure(Component.literal("You are only allowed to claim in the Overworld"));
                            return 0;
                        }

                        IServerPartyAPI party = OPAPC.getPartyManager().getPartyByOwner(player.getUUID());

                        if (party == null) {
                            ctx.getSource().sendFailure(Component.literal("Only party leaders can use this command"));
                            return 0;
                        }

                        // Check if in war
                        List<WarData> wars = WarManager.INSTANCE.getActiveWars();
                        for (WarData war : wars) {
                            if (war.getDefendingParty().equals(party) || war.getAttackingParty().equals(party)) {
                                ctx.getSource().sendFailure(Component.literal("Cannot claim chunks while in a war!"));
                                return 0;
                            }
                        }

                        PartyClaimsComponent comp = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard());
                        PartyClaim partyClaim = comp.getClaim(party.getId());

                        //Check if no party claim exists yet for party, if no claims then allow
                        if (partyClaim == null) {
                            ClaimResult<IPlayerChunkClaimAPI> result = OPAPC.getClaimsManager().tryToClaim(Level.OVERWORLD.location(), player.getUUID(), 0, player.chunkPosition().x, player.chunkPosition().z, player.chunkPosition().x, player.chunkPosition().z, false);
                            player.sendSystemMessage(result.getResultType().message);
                            if (result.getResultType().success) {
                                comp.createClaim(party.getId());
                                mirrorOverworldClaimsToNether(player.getUUID());
                                return 1;
                            }
                        }

                        IServerPlayerClaimInfoAPI info = OPAPC.getClaimsManager().getPlayerInfo(player.getUUID());
                        int totalOverworldClaims = info.getDimension(Level.OVERWORLD.location())
                                .getStream()
                                .mapToInt(IPlayerClaimPosListAPI::getCount)
                                .sum();

                        // Check if party has enough bought claims
                        if (partyClaim != null && totalOverworldClaims >= partyClaim.getBoughtClaims()) {
                            ctx.getSource().sendFailure(Component.literal("You've run out of party claims."));
                            return 0;
                        }

                        //Check if new chunk is adjacent to an old chunk
                        if (ClaimAdjacencyChecker.isNotAdjacentToExistingClaim(player.getUUID(), player.chunkPosition())) {
                            ctx.getSource().sendFailure(Component.literal("Claim must be adjacent to an existing party claim."));
                            return 0;
                        }

                        // All checks passed, try to claim!
                        ClaimResult<IPlayerChunkClaimAPI> result = OPAPC.getClaimsManager().tryToClaim(Level.OVERWORLD.location(), player.getUUID(), 0, player.chunkPosition().x, player.chunkPosition().z, player.chunkPosition().x, player.chunkPosition().z, false);

                        player.sendSystemMessage(result.getResultType().message, true);

                        if (result.getResultType().success) {
                            mirrorOverworldClaimsToNether(player.getUUID());
                            return 1;
                        }

                        return 0;
                    })
            );

            partyClaimCommand.then(literal("unclaim")
                    .requires(ctx -> {
                        ServerPlayer player = ctx.getPlayer();
                        if (player != null) {
                            return OPAPC.getPartyManager().getPartyByOwner(player.getUUID()) != null;
                        }
                        return false;
                    })
                    .executes(ctx -> {

                        ServerPlayer player = ctx.getSource().getPlayer();
                        //Check if player
                        if (player == null) {
                            ctx.getSource().sendFailure(Component.literal("This command must be run by a player."));
                            return 0;
                        }

                        //Check if in overworld
                        if (!player.level().dimension().location().equals(Level.OVERWORLD.location())) {
                            ctx.getSource().sendFailure(Component.literal("You are only allowed to unclaim in the Overworld"));
                            return 0;
                        }

                        IServerPartyAPI party = OPAPC.getPartyManager().getPartyByOwner(player.getUUID());

                        if (party == null) {
                            ctx.getSource().sendFailure(Component.literal("Only party leaders can use this command"));
                            return 0;
                        }

                        // Check if in war
                        List<WarData> wars = WarManager.INSTANCE.getActiveWars();
                        for (WarData war : wars) {
                            if (war.getDefendingParty().equals(party) || war.getAttackingParty().equals(party)) {
                                ctx.getSource().sendFailure(Component.literal("Cannot unclaim chunks while in a war!"));
                                return 0;
                            }
                        }

                        PartyClaimsComponent comp = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard());
                        PartyClaim partyClaim = comp.getClaim(party.getId());

                        //Check if no party claim exists yet for party, if no claims then allow
                        if (partyClaim == null) {
                            ClaimResult<IPlayerChunkClaimAPI> result = OPAPC.getClaimsManager().tryToClaim(Level.OVERWORLD.location(), player.getUUID(), 0, player.chunkPosition().x, player.chunkPosition().z, player.chunkPosition().x, player.chunkPosition().z, false);
                            player.sendSystemMessage(result.getResultType().message);
                            if (result.getResultType().success) {
                                comp.createClaim(party.getId());
                                mirrorOverworldClaimsToNether(player.getUUID());
                                return 1;
                            }
                        }

                        IServerPlayerClaimInfoAPI info = OPAPC.getClaimsManager().getPlayerInfo(player.getUUID());
                        int totalOverworldClaims = info.getDimension(Level.OVERWORLD.location())
                                .getStream()
                                .mapToInt(IPlayerClaimPosListAPI::getCount)
                                .sum();

                        // Check if party has enough bought claims
                        if (partyClaim != null && totalOverworldClaims >= partyClaim.getBoughtClaims()) {
                            ctx.getSource().sendFailure(Component.literal("You've run out of party claims."));
                            return 0;
                        }

                        //Check if new chunk is adjacent to an old chunk
                        if (ClaimAdjacencyChecker.isNotAdjacentToExistingClaim(player.getUUID(), player.chunkPosition())) {
                            ctx.getSource().sendFailure(Component.literal("Claim must be adjacent to an existing party claim."));
                            return 0;
                        }

                        // All checks passed, try to claim!
                        ClaimResult<IPlayerChunkClaimAPI> result = OPAPC.getClaimsManager().tryToClaim(Level.OVERWORLD.location(), player.getUUID(), 0, player.chunkPosition().x, player.chunkPosition().z, player.chunkPosition().x, player.chunkPosition().z, false);

                        player.sendSystemMessage(result.getResultType().message, true);

                        if (result.getResultType().success) {
                            mirrorOverworldClaimsToNether(player.getUUID());
                            return 1;
                        }

                        return 0;
                    })
            );



        }));
    }
}
