package com.madmike.opapc.command;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.madmike.opapc.OPAPC;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.partyclaim.components.scoreboard.PartyClaimsComponent;
import com.madmike.opapc.OPAPCConfig;
import com.madmike.opapc.partyclaim.data.Donor;
import com.madmike.opapc.partyclaim.data.PartyClaim;
import com.madmike.opapc.war.data.WarData;
import com.madmike.opapc.util.CurrencyUtil;
import com.madmike.opapc.util.ClaimAdjacencyChecker;
import com.madmike.opapc.war.WarManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.claims.player.api.IPlayerClaimPosListAPI;
import xaero.pac.common.claims.result.api.ClaimResult;
import xaero.pac.common.parties.party.api.IPartyAPI;
import xaero.pac.common.parties.party.member.api.IPartyMemberAPI;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.claims.api.IServerClaimsManagerAPI;
import xaero.pac.common.server.claims.player.api.IServerPlayerClaimInfoAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;
import xaero.pac.common.server.player.config.api.PlayerConfigOptions;

import java.util.*;

import static com.madmike.opapc.command.commands.claims.ListPartyClaimsCommandHandler.handleListPartyClaimsCommand;
import static com.madmike.opapc.util.NetherClaimAdjuster.mirrorOverworldClaimsToNether;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class CommandsManager {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            //region Party Command

            LiteralArgumentBuilder<CommandSourceStack> partyCommand = literal("party").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayer();
                if (player == null) {
                    ctx.getSource().sendFailure(Component.literal("This command must be run by a player."));
                    return 0;
                }

                // Get the player's party ID
                IServerPartyAPI party = OPAPC.getPartyManager().getPartyByMember(player.getUUID());
                if (party == null) {
                    ctx.getSource().sendFailure(Component.literal("§cYou are not in a party."));
                    return 0;
                }

                UUID partyId = party.getId();


                // Get Party Claims Info
                PartyClaim claim = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard()).getAllClaims()
                        .get(partyId);
                int usedClaims = claim != null ? claim.getBoughtClaims() : 0;
                int maxClaims = OPAPCConfig.maxClaimsPerParty;

                // Get top 5 donators
                Map<UUID, Donor> donations = claim.getDonations();
                List<Map.Entry<UUID, Donor>> topDonators = new ArrayList<>(donations.entrySet());
                topDonators.sort((a, b) -> Long.compare(b.getValue().amount(), a.getValue().amount()));
                int limit = Math.min(topDonators.size(), 5);

                ctx.getSource().sendSystemMessage(Component.literal("§aParty Info for §b" + partyNameObj.getName()));

                ctx.getSource().sendSystemMessage(Component.literal(String.format(
                        "§eClaims Used: §a%d§7/§a%d", usedClaims, maxClaims
                )));

                if (limit > 0) {
                    ctx.getSource().sendSystemMessage(Component.literal("§eTop Donators:"));
                    for (int i = 0; i < limit; i++) {
                        Map.Entry<UUID, Donor> entry = topDonators.get(i);
                        String donatorName = OPAPC.getServer().getPlayerList().getPlayer(entry.getKey()) != null
                                ? OPAPC.getServer().getPlayerList().getPlayer(entry.getKey()).getName().getString()
                                : OPAPC.getServer().getProfileCache().get(entry.getKey()).map(GameProfile::getName).orElse("Unknown");

                        ctx.getSource().sendSystemMessage(Component.literal(
                                String.format("§b%d. §a%s §7- §6%d Gold", i + 1, donatorName, CurrencyUtil.fromTotalBronze(entry.getValue().amount()).gold())
                        ));
                    }
                } else {
                    ctx.getSource().sendSystemMessage(Component.literal("§7No donations recorded for this party."));
                }

                return 1;
            });

            partyCommand.then(literal("claim").executes(ctx -> {

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

                var party = OPAPC.getPartyManager().getPartyByMember(player.getUUID());

                //Check if in party
                if (party == null) {
                    ctx.getSource().sendFailure(Component.literal("Must be in a party and its leader to claim chunks."));
                    return 0;
                }

                //Check if party leader
                if (!party.getOwner().getUUID().equals(player.getUUID())) {
                    ctx.getSource().sendFailure(Component.literal("Only the party leader can claim or unclaim chunks."));
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

                ChunkPos target = player.chunkPosition();
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
                if (totalOverworldClaims >= partyClaim.getBoughtClaims()) {
                    ctx.getSource().sendFailure(Component.literal("You've run out of party claims."));
                    return 0;
                }

                //Check if new chunk is adjacent to an old chunk
                if (ClaimAdjacencyChecker.isNotAdjacentToExistingClaim(player, target)) {
                    ctx.getSource().sendFailure(Component.literal("Claim must be adjacent to an existing party claim."));
                    return 0;
                }

                handlePartyClaimCommand(player);

                return 1;
            }));

            partyCommand.then(literal("unclaim").executes(ctx -> {
                // Check if player
                ServerPlayer player = ctx.getSource().getPlayer();
                if (player == null) {
                    ctx.getSource().sendFailure(Component.literal("This command must be run by a player."));
                    return 0;
                }

                // Check if in overworld
                if (!player.level().dimension().location().equals(Level.OVERWORLD.location())) {
                    ctx.getSource().sendFailure(Component.literal("You are only allowed to claim in the Overworld"));
                    return 0;
                }

                MinecraftServer server = ctx.getSource().getServer();
                OpenPACServerAPI api = OpenPACServerAPI.get(server);
                IServerClaimsManagerAPI cm = api.getServerClaimsManager();
                var party = api.getPartyManager().getPartyByMember(player.getUUID());

                //Check if in party
                if (party == null) {
                    ctx.getSource().sendFailure(Component.literal("Must be in a party and its leader to unclaim chunks."));
                    return 0;
                }
                //Check if party leader
                if (!party.getOwner().getUUID().equals(player.getUUID())) {
                    ctx.getSource().sendFailure(Component.literal("Only the party leader can claim or unclaim chunks."));
                    return 0;
                }

                //TODO CHECK IF PLAYER IS IN WAR

                PartyClaim partyClaim = OPAPCComponents.PARTY_CLAIMS.get(server.getScoreboard()).getClaim(party.getId());

                // Make sure there is an existing claim
                if (partyClaim == null) {
                    ctx.getSource().sendFailure(Component.literal("You must have an existing claim to unclaim!"));
                    return 0;
                }

                ChunkPos target = player.chunkPosition();

                // Ensure unclaim does not break adjacency
                if (ClaimAdjacencyChecker.wouldBreakAdjacency(player, target, api)) {
                    ctx.getSource().sendFailure(Component.literal("You cannot unclaim a chunk that would split your party's territory."));
                    return 0;
                }

                handlePartyUnclaimCommand(cm, player, partyClaim);
                return 1;
            }));

            partyCommand.then(literal("abandon")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayer();
                        if (player == null) return 0;

                        player.sendSystemMessage(Component.literal("""
                            §cWarning: Using [/party abandon confirm] will:
                            - Destroy your current party claim block
                            - Unclaim your party's land
                            - Delete your party claim progress
                
                            §eOnly the party leader can perform this action, and you must be near your Party Claim Block.
                            """));
                        return 1;
                    })
                    .then(literal("confirm").executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayer();
                        if (player == null) return 0;

                        IPartyAPI party = OPAPC.getPartyManager().getPartyByMember(player.getUUID());

                        if (party == null) {
                            player.sendSystemMessage(Component.literal("You are not in a party."));
                            return 0;
                        }

                        IPartyMemberAPI owner = party.getOwner();
                        if (!player.getUUID().equals(owner.getUUID())) {
                            player.sendSystemMessage(Component.literal("Only the party leader can abandon the party claim."));
                            return 0;
                        }

                        handleAbandonCommand(owner, party.getId());
                        player.sendSystemMessage(Component.literal("Party claim abandoned successfully."));
                        return 1;
                    }))
            );

            partyCommand.then(literal("donate")
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
                        partyClaim.addDonation(player.getUUID(), player.getGameProfile().getName(), costOfNewClaim);
                        ctx.getSource().sendSystemMessage(Component.literal("Donated to the party Successfully! Party now owns " + partyClaim.getBoughtClaims() + " claims."));
                        return 1;
                    })
            );

            partyCommand.then(literal("top")
                    .executes(ctx -> {
                        MinecraftServer server = ctx.getSource().getServer();

                        Map<UUID, PartyClaim> allClaims = OPAPCComponents.PARTY_CLAIMS.get(server.getScoreboard()).getAllClaims();
                        Map<UUID, PartyName> partyNameHashMap = OPAPCComponents.PARTY_NAMES.get(server.getScoreboard()).getPartyNameHashMap();

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

                            String partyName = partyNameHashMap.getOrDefault(partyId, new PartyName(partyId, "Unknown")).getName();
                            int claims = claim.getBoughtClaims();

                            ctx.getSource().sendSystemMessage(Component.literal(
                                    String.format("§e%d. §b%s §7- §a%d claims", i + 1, partyName, claims)
                            ));
                        }

                        return 1;
                    })
            );

            partyCommand.then(literal("list").requires(souce -> souce.hasPermission(2))
                    .executes(ctx -> {
                        handleListPartyClaimsCommand(ctx);
                        return 1;
                    })
            );

            partyCommand.then(literal("setname").then(argument("newname", StringArgumentType.string())
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayer();
                        MinecraftServer server = ctx.getSource().getServer();
                        if (player == null) return 0;

                        var api = OpenPACServerAPI.get(server);
                        IPartyAPI party = api.getPartyManager().getPartyByOwner(player.getUUID());

                        if (party == null) {
                            player.sendSystemMessage(Component.literal("You are not a leader of a party."));
                            return 0;
                        }

                        String newName = ctx.getArgument("newname", String.class);

                        api.getPlayerConfigs().getLoadedConfig(player.getUUID()).tryToSet(PlayerConfigOptions.CLAIMS_NAME, newName);
                        OPAPCComponents.PARTY_NAMES.get(server.getScoreboard()).addOrUpdatePartyName(new PartyName(party.getId(), newName));

                        return 1;

                    }))
            );

            dispatcher.register(partyCommand);

            //endregion

            //region War Command



            LiteralArgumentBuilder<CommandSourceStack> warCommand = literal("war").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayer();
                // Display active wars for the player’s party
                WarManager.INSTANCE.displayWarInfo(player);
                return 1;
            });

            warCommand.then(literal("declare")
                    .then(argument("party", StringArgumentType.string()).suggests((context, builder) -> {

                        //Check if player
                        ServerPlayer player = context.getSource().getPlayer();
                        if (player == null) {
                            return builder.buildFuture();
                        }

                        //Check if owner of party
                        IServerPartyAPI party = OPAPC.getPartyManager().getPartyByOwner(context.getSource().getPlayer().getUUID());
                        if (party == null) {
                            return builder.buildFuture();
                        }

                        PartyClaimsComponent comp = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard());
                        PartyClaim attackingClaim = comp.getClaim(party.getId());
                        Map<UUID, PartyClaim> allClaims = comp.getAllClaims();
                        List<UUID> idsToLookUp = new ArrayList<>();

                        for (Map.Entry<UUID, PartyClaim> entry : allClaims.entrySet()) {
                            if (!entry.getValue().isWarInsured() && entry.getValue().getBoughtClaims() >= attackingClaim.getBoughtClaims() && !entry.getValue().getPartyId().equals(attackingClaim.getPartyId())) {
                                idsToLookUp.add(entry.getKey());
                            }
                        }

                        for (UUID id : idsToLookUp) {
                            builder.suggest(comp.getPartyName(id));
                        }

                        return builder.buildFuture();
                    }))
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayer();
                        if (player == null) {
                            ctx.getSource().sendFailure(Component.literal("Must be a player to use this command."));
                            return 0;
                        }
                        OpenPACServerAPI api = OpenPACServerAPI.get(ctx.getSource().getServer());
                        IPartyManagerAPI pm = api.getPartyManager();
                        IServerPartyAPI attackingParty = pm.getPartyByOwner(player.getUUID());

                        if (attackingParty == null) {
                            ctx.getSource().sendFailure(Component.literal("Must own a party to declare a war"));
                            return 0;
                        }

                        List<WarData> activeWars = WarManager.INSTANCE.getActiveWars();
                        String defendingPartyName = ctx.getArgument("party", String.class);

                        for (WarData war : activeWars) {
                            if (war.getAttackingParty().getId().equals(attackingParty.getId())) {
                                player.sendSystemMessage(Component.literal("You are already in a war!"));
                                return false;
                            }
                            if (war.getDefendingParty().getId().equals()) {
                                player.sendSystemMessage(Component.literal("This party is already under attack."));
                                return false;
                            }
                        }

                        if (attackerClaims >= defenderClaims && OPAPCConfig.canOnlyAttackLargerClaims) {
                            player.sendSystemMessage(Component.literal("You can only declare war on parties with more claims than you."));
                            return false;
                        }

                        UUID attackerPartyId =
                                UUID defenderPartyId = PartyArgumentType.getParty(ctx, "party").getId();
                        int attackerClaims = PartyAPI.getClaimsBought(attackerPartyId);
                        int defenderClaims = PartyAPI.getClaimsBought(defenderPartyId);
                        if (WarManager.INSTANCE.canDeclareWar(attackerPartyId, defenderPartyId, attackerClaims, defenderClaims, config, player)) {
                            WarManager.INSTANCE.declareWar(attackerPartyId, defenderPartyId, player.getWorld(), config);
                            player.sendMessage(Text.literal("War declared on party: " + defenderPartyId));
                        }
                        return 1;
                    }))

            dispatcher.register(literal("war")
                    .then(literal("declare")
                            .then(argument("party", StringArgumentType.string()).suggests((context, builder) -> {

                                //Check if player
                                ServerPlayer player = context.getSource().getPlayer();
                                if (player == null) {
                                    return builder.buildFuture();
                                }

                                //Check if owner of party
                                IServerPartyAPI party = OPAPC.getPartyManager().getPartyByOwner(context.getSource().getPlayer().getUUID());
                                if (party == null) {
                                    return builder.buildFuture();
                                }

                                PartyClaimsComponent comp = OPAPCComponents.PARTY_CLAIMS.get(OPAPC.getServer().getScoreboard());
                                PartyClaim attackingClaim = comp.getClaim(party.getId());
                                Map<UUID, PartyClaim> allClaims = comp.getAllClaims();
                                List<UUID> idsToLookUp = new ArrayList<>();

                                for (Map.Entry<UUID, PartyClaim> entry : allClaims.entrySet()) {
                                    if (!entry.getValue().isWarInsured() && entry.getValue().getBoughtClaims() >= attackingClaim.getBoughtClaims() && !entry.getValue().getPartyId().equals(attackingClaim.getPartyId())) {
                                        idsToLookUp.add(entry.getKey());
                                    }
                                }

                                for (UUID id : idsToLookUp) {
                                    builder.suggest(comp.getPartyName(id));
                                }

                                return builder.buildFuture();
                            }))
                                    .executes(ctx -> {
                                        ServerPlayer player = ctx.getSource().getPlayer();
                                        if (player == null) {
                                            ctx.getSource().sendFailure(Component.literal("Must be a player to use this command."));
                                            return 0;
                                        }
                                        OpenPACServerAPI api = OpenPACServerAPI.get(ctx.getSource().getServer());
                                        IPartyManagerAPI pm = api.getPartyManager();
                                        IServerPartyAPI attackingParty = pm.getPartyByOwner(player.getUUID());

                                        if (attackingParty == null) {
                                            ctx.getSource().sendFailure(Component.literal("Must own a party to declare a war"));
                                            return 0;
                                        }

                                        List<WarData> activeWars = WarManager.INSTANCE.getActiveWars();

                                        for (WarData war : activeWars) {
                                            if (war.getAttackingParty().getId().equals(attackingParty.getId())) {
                                                player.sendSystemMessage(Component.literal("You are already in a war!"));
                                                return false;
                                            }
                                            if (war.getDefendingParty().getId().equals(defenderPartyId)) {
                                                player.sendSystemMessage(Component.literal("This party is already under attack."));
                                                return false;
                                            }
                                        }

                                        if (attackerClaims >= defenderClaims && OPAPCConfig.canOnlyAttackLargerClaims) {
                                            player.sendSystemMessage(Component.literal("You can only declare war on parties with more claims than you."));
                                            return false;
                                        }

                                        UUID attackerPartyId =
                                        UUID defenderPartyId = PartyArgumentType.getParty(ctx, "party").getId();
                                        int attackerClaims = PartyAPI.getClaimsBought(attackerPartyId);
                                        int defenderClaims = PartyAPI.getClaimsBought(defenderPartyId);
                                        if (WarManager.INSTANCE.canDeclareWar(attackerPartyId, defenderPartyId, attackerClaims, defenderClaims, config, player)) {
                                            WarManager.INSTANCE.declareWar(attackerPartyId, defenderPartyId, player.getWorld(), config);
                                            player.sendMessage(Text.literal("War declared on party: " + defenderPartyId));
                                        }
                                        return 1;
                                    }))
                    .then(literal("info")
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayer();
                                // Display active wars for the player’s party
                                WarManager.INSTANCE.displayWarInfo(player);
                                return 1;
                            }))
                    .then(literal("forfeit")
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayer();
                                UUID defenderPartyId = PartyArgumentType.getParty(ctx, "party").getId();
                                WarManager.INSTANCE.endWar(defenderPartyId, player.getWorld());
                                player.sendSystemMessage(Component.literal("Ended war with party: " + defenderPartyId));
                                return 1;
                            })));

            //endregion

            //region Raid Command

            //todo raid respawn command for if a raider gets stuck

            //endregion

        });
    }
}
