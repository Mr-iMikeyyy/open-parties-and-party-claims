package com.madmike.opapc.features.item;

import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.components.scoreboard.parties.PartyClaimsComponent;
import com.madmike.opapc.data.parties.claims.PartyClaim;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

public class PartyClaimBlockItem extends BlockItem {

    public PartyClaimBlockItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public ActionResult place(ItemPlacementContext context) {
        PlayerEntity player = context.getPlayer();
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            // Prevent placement from non-server players (e.g., fake players)
            return ActionResult.FAIL;
        }

        MinecraftServer server = serverPlayer.getServer();
        if (server == null) {
            // Defensive: should never happen but prevents NPEs
            return ActionResult.FAIL;
        }

        OpenPACServerAPI api = OpenPACServerAPI.get(server);
        IServerPartyAPI party = api.getPartyManager().getPartyByOwner(serverPlayer.getUuid());

        if (party == null) {
            // Player does not own a party, deny placement
            serverPlayer.sendMessage(Text.literal("You must own a party to place a claim block.").formatted(Formatting.RED), false);
            return ActionResult.FAIL;
        }

        PartyClaimsComponent claimsComponent = OPAPCComponents.PARTY_CLAIMS.get(serverPlayer.getScoreboard());
        PartyClaim claim = claimsComponent.getClaim(party.getId());

        if (claim != null) {
            // Party already has a claim, deny placement
            serverPlayer.sendMessage(Text.literal("Your party already has an active claim.").formatted(Formatting.RED), false);
            return ActionResult.FAIL;
        }

        // Create the party claim at the intended position
        claimsComponent.createClaim(party.getId(), context.getBlockPos());
        serverPlayer.sendMessage(Text.literal("Party claim created successfully!").formatted(Formatting.GREEN), false);

        // Proceed with normal block placement
        return super.place(context);
    }
}
