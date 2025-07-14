package com.madmike.opapc.command.commands.trades;

import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.trade.components.player.UnlockedStoreSlotsComponent;
import com.madmike.opapc.trade.components.scoreboard.OffersComponent;
import com.madmike.opapc.trade.data.Offer;
import com.madmike.opapc.util.CurrencyUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.UUID;

public class SellCommandHandler {

    public static int handleSellCommand(ServerPlayer player, long price, MinecraftServer server) {
        if (price <= 0) {
            player.sendSystemMessage(Component.literal("Price needs to be larger than 0").withStyle(ChatFormatting.RED));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.literal("You're not holding any item to sell.").withStyle(ChatFormatting.RED));
            return 0;
        }

        OffersComponent offers = OPAPCComponents.OFFERS.get(server.getScoreboard());
        long usedSlots = offers.getOffers().values().stream()
                .filter(e -> player.getUUID().equals(e.getOfferId()))
                .count();

        UnlockedStoreSlotsComponent unlockedSlotsComponent = OPAPCComponents.UNLOCKED_STORE_SLOTS.get(player);
        int unlocked = unlockedSlotsComponent.getUnlockedSlots();

        if (unlocked <= usedSlots) {
            player.sendSystemMessage(Component.literal("You don't have any available sell slots left.").withStyle(ChatFormatting.RED));
            return 0;
        }

        IServerPartyAPI party = OpenPACServerAPI.get(server).getPartyManager().getPartyByMember(player.getUUID());

        ItemStack listedItem = stack.copy();
        player.getMainHandItem().setCount(0); // remove the item

        Offer offer = new Offer(
                UUID.randomUUID(),
                player.getUUID(),
                listedItem,
                price,
                (party == null ? null : party.getId())
        );

        offers.addOffer(offer);

        CurrencyUtil.CoinBreakdown bd = CurrencyUtil.fromTotalBronze(price);
        player.sendSystemMessage(Component.literal(String.format(
                "Listed item for %d gold, %d silver, %d bronze.",
                bd.gold(), bd.silver(), bd.bronze()
        )).withStyle(ChatFormatting.GOLD));

        return 1;
    }
}
