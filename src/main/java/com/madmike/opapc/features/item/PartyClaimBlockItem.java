package com.madmike.opapc.features.item;

import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.components.scoreboard.parties.PartyClaimsComponent;
import com.madmike.opapc.data.parties.claims.PartyClaim;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

public class PartyClaimBlockItem extends BlockItem {

    public PartyClaimBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public @NotNull InteractionResult place(BlockPlaceContext context) {
        var player = context.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        MinecraftServer server = serverPlayer.getServer();
        if (server == null) {
            return InteractionResult.FAIL;
        }

        Level level = context.getLevel();
        if (!level.dimension().equals(Level.OVERWORLD)) {
            serverPlayer.sendSystemMessage(Component.literal("You can only place party claim blocks in the Overworld.").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        OpenPACServerAPI api = OpenPACServerAPI.get(server);
        IServerPartyAPI party = api.getPartyManager().getPartyByOwner(serverPlayer.getUUID());

        if (party == null) {
            serverPlayer.sendSystemMessage(Component.literal("You must own a party to place a claim block.").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        PartyClaimsComponent claimsComponent = OPAPCComponents.PARTY_CLAIMS.get(serverPlayer.getScoreboard());
        PartyClaim claim = claimsComponent.getClaim(party.getId());

        if (claim != null) {
            serverPlayer.sendSystemMessage(Component.literal("Your party already has an active claim.").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        IPlayerChunkClaimAPI chunk = api.getServerClaimsManager().get(Level.OVERWORLD.location(), context.getClickedPos());
        if (chunk != null) {
            serverPlayer.sendSystemMessage(Component.literal("This chunk is already claimed.").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        // Create the claim
        PartyClaim newClaim = claimsComponent.createClaim(party.getId());
        newClaim.setPcb(context.getClickedPos());
        api.getServerClaimsManager().claim(Level.OVERWORLD.location(), player.getUUID(), 0,
                context.getClickedPos().getX(), context.getClickedPos().getZ(), false);
        serverPlayer.sendSystemMessage(Component.literal("Party claim created successfully!").withStyle(ChatFormatting.GREEN));

        return super.place(context);
    }
}
