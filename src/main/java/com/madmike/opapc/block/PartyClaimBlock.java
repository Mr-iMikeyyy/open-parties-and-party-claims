package com.madmike.opapc.block;

import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.data.KnownParty;
import com.madmike.opapc.util.CurrencyUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;
import xaero.pac.common.server.player.config.api.IPlayerConfigAPI;

import java.util.UUID;

public class PartyClaimBlock extends Block {

    public PartyClaimBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        ItemStack stack = player.getStackInHand(hand);
        if (stack.isEmpty()) {
            player.sendMessage(Text.literal("Hold currency to donate."), false);
            return ActionResult.FAIL;
        }

        // Try get bronze value of item
        long value = CurrencyUtil.getValueOfItemStack(stack);
        if (value <= 0) {
            player.sendMessage(Text.literal("This item has no donation value."), false);
            return ActionResult.FAIL;
        }

        // Access party
        UUID playerId = player.getUuid();
        MinecraftServer server = player.getServer();
        IServerPartyAPI party = OpenPACServerAPI.get(server).getPartyManager().getPartyByMember(playerId);

        if (party == null) {
            player.sendMessage(Text.literal("You're not in a party."), false);
            return ActionResult.FAIL;
        }

        KnownParty claimComp = OPAPCComponents.KNOWN_PARTIES.get(server.getScoreboard()).get(party.getId());

        int currentBonus = claimComp.getClaims();
        int cost = (currentBonus + 1) * 10000;

        claimComp.(value);
        stack.decrement(1); // remove donated item

        player.sendMessage(Text.literal("Donated " + value + " bronze.").formatted(Formatting.GOLD));

        if (claimComp.getTotalDonated() >= cost) {
            claimComp.incrementBonusClaims();

            ServerPlayerEntity leader = party.getLeader() != null ? server.getPlayerManager().getPlayer(party.getLeader()) : null;
            if (leader != null) {
                IPlayerConfigAPI config = OpenPACServerAPI.get(server).getPlayerConfig();
                int newLimit = config.getMaxClaims(leader.getUuid()) + 1;
                config.setMaxClaims(leader.getUuid(), newLimit);

                leader.sendMessage(Text.literal("Your party earned a bonus claim!").formatted(Formatting.GREEN));
            }

            claimComp.addDonation(-cost); // remove cost from total
        }

        return ActionResult.SUCCESS;
    }
}
