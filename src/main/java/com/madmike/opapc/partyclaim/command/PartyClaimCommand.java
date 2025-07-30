package com.madmike.opapc.partyclaim.command;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.partyclaim.components.scoreboard.PartyClaimsComponent;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import com.madmike.opapc.util.ClaimAdjacencyChecker;
import com.madmike.opapc.util.CurrencyUtil;
import com.madmike.opapc.war.WarManager;
import com.madmike.opapc.war.data.WarData;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.claims.player.api.IPlayerClaimInfoAPI;
import xaero.pac.common.claims.player.api.IPlayerClaimPosListAPI;
import xaero.pac.common.claims.result.api.ClaimResult;
import xaero.pac.common.parties.party.api.IPartyAPI;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;
import xaero.pac.common.server.claims.player.api.IServerPlayerClaimInfoAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
                            §e/partyclaim abandon confirm §7- Delete your party claim and progress
                            
                            §6--- Only available to Party Members ---
                            §e/partyclaim info §7- Get details of your party's claim and donations
                            §e/partyclaim top §7- Increase your party's max claims by 1
                            §e/partyclaim donate §7- Use inside your party's or an ally's claim to purchase the next claim
                            """)
                    );
                    return 1;
                }
                return 0;
            });

            //region Claim
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
                                comp.getClaim(party.getId()).setWarpPos(player.blockPosition());
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
            //endregion

            //region Unclaim
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
                            player.sendSystemMessage(Component.literal("You are only allowed to unclaim in the Overworld"));
                            return 0;
                        }

                        IServerPartyAPI party = OPAPC.getPartyManager().getPartyByOwner(player.getUUID());

                        if (party == null) {
                            player.sendSystemMessage(Component.literal("Only party leaders can use this command"));
                            return 0;
                        }

                        // Check if in war
                        List<WarData> wars = WarManager.INSTANCE.getActiveWars();
                        for (WarData war : wars) {
                            if (war.getDefendingParty().equals(party) || war.getAttackingParty().equals(party)) {
                                player.sendSystemMessage(Component.literal("Cannot unclaim chunks while in a war!"));
                                return 0;
                            }
                        }

                        PartyClaimsComponent comp = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard());
                        PartyClaim partyClaim = comp.getClaim(party.getId());

                        //Check for party claim
                        if (partyClaim == null) {
                            player.sendSystemMessage(Component.literal("You don't have a claim to un-claim yet"));
                            return 0;
                        }

                        IServerPlayerClaimInfoAPI info = OPAPC.getClaimsManager().getPlayerInfo(player.getUUID());
                        int totalOverworldClaims = info.getDimension(Level.OVERWORLD.location())
                                .getStream()
                                .mapToInt(IPlayerClaimPosListAPI::getCount)
                                .sum();

                        //Check not last claim
                        if (totalOverworldClaims <= 1) {
                            player.sendSystemMessage(Component.literal("You can't un-claim your last claim, use /abandon instead"));
                            return 0;
                        }

                        //Check would not break adjacency
                        if (ClaimAdjacencyChecker.wouldBreakAdjacency(player.getUUID(), player.chunkPosition())) {
                            player.sendSystemMessage(Component.literal("You can't un-claim a claim that would split your territory in two"));
                            return 0;
                        }

                        //All checks passed, un-claim chunk
                        ClaimResult<IPlayerChunkClaimAPI> result = OPAPC.getClaimsManager().tryToUnclaim(Level.OVERWORLD.location(), player.getUUID(), player.chunkPosition().x, player.chunkPosition().z, player.chunkPosition().x, player.chunkPosition().z, false);
                        player.sendSystemMessage(result.getResultType().message);
                        if (result.getResultType().success) {
                            if (new ChunkPos(partyClaim.getWarpPos()).equals(player.chunkPosition())) {
                                partyClaim.setWarpPos(null);
                            }
                            mirrorOverworldClaimsToNether(player.getUUID());
                            return 1;
                        }
                        else {
                            return 0;
                        }
                    })
            );
            //endregion

            //region Abandon
            partyClaimCommand.then(literal("abandon")
                    .requires(ctx -> {
                        ServerPlayer player = ctx.getPlayer();
                        if (player != null) {
                            return OPAPC.getPartyManager().getPartyByOwner(player.getUUID()) != null;
                        }
                        return false;
                    })
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayer();
                        if (player == null) return 0;

                        player.sendSystemMessage(Component.literal("""
                            §cWarning: Using [/party abandon confirm] will:
                            - Destroy your current party claim
                            - Unclaim your party's land
                            - Delete all donations and party claim progress
                            - You will need to start all over
                            """));
                        return 1;
                    })
                    .then(literal("confirm").executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayer();
                        if (player == null) return 0;

                        IPartyAPI party = OPAPC.getPartyManager().getPartyByOwner(player.getUUID());

                        //Check if leader of a party
                        if (party == null) {
                            player.sendSystemMessage(Component.literal("You are not a leader of a party"));
                            return 0;
                        }

                        PartyClaim claim = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard()).getClaim(party.getId());

                        //Check if party claim exists
                        if (claim == null) {
                            player.sendSystemMessage(Component.literal("Your party doesn't have a claim to abandon yet"));
                            return 0;
                        }


                        //All checks passed, un-claim all chunks and destroy party claim
                        IPlayerClaimInfoAPI info = OPAPC.getClaimsManager().getPlayerInfo(player.getUUID());

                        List<ChunkPos> overworldClaims = new ArrayList<>();
                        List<ChunkPos> netherClaims = new ArrayList<>();

                        ResourceLocation overworld = Level.OVERWORLD.location();
                        ResourceLocation netherKey = Level.NETHER.location();

                        info.getDimension(overworld).getStream().forEach(e -> e.getStream().forEach(overworldClaims::add));
                        info.getDimension(netherKey).getStream().forEach(e -> e.getStream().forEach(netherClaims::add));

                        for (ChunkPos pos : overworldClaims) {
                            OPAPC.getClaimsManager().unclaim(overworld, pos.x, pos.z);
                        }

                        for (ChunkPos pos : netherClaims) {
                            OPAPC.getClaimsManager().unclaim(netherKey, pos.x, pos.z);
                        }

                        OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard()).removeClaim(party.getId());

                        player.sendSystemMessage(Component.literal("Party claim abandoned successfully."));
                        return 1;
                    }))
            );
            //endregion

            //region Donate
            partyClaimCommand.then(literal("donate")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayer();
                        if (player == null) {
                            ctx.getSource().sendFailure(Component.literal("This command must be run by a player."));
                            return 0;
                        }

                        if (!player.level().dimension().location().equals(Level.OVERWORLD.location())) {
                            ctx.getSource().sendFailure(Component.literal("You are only allowed to donate in the Overworld"));
                            return 0;
                        }

                        var donatersParty = OPAPC.getPartyManager().getPartyByMember(player.getUUID());
                        if (donatersParty == null) {
                            ctx.getSource().sendFailure(Component.literal("Must be in a party to donate."));
                            return 0;
                        }

                        IServerClaimsManagerAPI cm = OPAPC.getClaimsManager();

                        ChunkPos target = player.chunkPosition();
                        IPlayerChunkClaimAPI chunkClaim = cm.get(Level.OVERWORLD.location(), target.x, target.z);

                        if (chunkClaim == null) {
                            ctx.getSource().sendFailure(Component.literal("You must be in a claim to donate to it."));
                            return 0;
                        }

                        UUID owner = chunkClaim.getPlayerId();
                        IServerPartyAPI ownersParty = OPAPC.getPartyManager().getPartyByOwner(owner);
                        PartyClaim partyClaim = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard()).getClaim(ownersParty.getId());

                        boolean isAlly = ownersParty.isAlly(donatersParty.getId());
                        boolean isMember = ownersParty.getMemberInfo(player.getUUID()) != null;

                        if (!isAlly && !isMember) {
                            ctx.getSource().sendFailure(Component.literal("You must be a member of the party or an ally to donate to it."));
                            return 0;
                        }

                        CurrencyComponent wallet = ModComponents.CURRENCY.get(player);
                        long costOfNewClaim = (partyClaim.getBoughtClaims() + 1) * 10000L;

                        if (wallet.getValue() < costOfNewClaim) {
                            ctx.getSource().sendFailure(Component.literal("You don't have enough gold to donate. You need " + CurrencyUtil.fromTotalBronze(costOfNewClaim).gold() + " gold."));
                            return 0;
                        }

                        wallet.modify(-costOfNewClaim);
                        partyClaim.setBoughtClaims(partyClaim.getBoughtClaims() + 1);
                        partyClaim.addDonation(player.getUUID(), costOfNewClaim);
                        ctx.getSource().sendSystemMessage(Component.literal("Donated to the party Successfully! Party now owns " + partyClaim.getBoughtClaims() + " claims."));
                        return 1;
                    })
            );

            //endregion

            //region Top
            partyClaimCommand.then(literal("top")
                    .executes(ctx -> {

                        Map<UUID, PartyClaim> allClaims = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard()).getAllClaims();

                        // Create a sorted list (descending by getBoughtClaims)
                        List<Map.Entry<UUID, PartyClaim>> sortedClaims = new ArrayList<>(allClaims.entrySet());
                        sortedClaims.sort((a, b) -> Integer.compare(b.getValue().getBoughtClaims(), a.getValue().getBoughtClaims()));

                        // Limit to top 10
                        int limit = Math.min(sortedClaims.size(), 10);

                        ctx.getSource().sendSystemMessage(Component.literal("§aTop Parties by Bought Claims:"));
                        for (int i = 0; i < limit; i++) {
                            Map.Entry<UUID, PartyClaim> entry = sortedClaims.get(i);
                            UUID partyId = entry.getKey();
                            PartyClaim claim = entry.getValue();

                            String partyName = claim.getPartyName();
                            int claims = claim.getBoughtClaims();

                            ctx.getSource().sendSystemMessage(Component.literal(
                                    String.format("§e%d. §b%s §7- §a%d claims", i + 1, partyName, claims)
                            ));
                        }

                        return 1;
                    })
            );
            //endregion

            commandDispatcher.register(partyClaimCommand);

        }));
    }
}
