package com.madmike.opapc.command.commands;

import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.components.player.trades.UnlockedStoreSlotsComponent;
import com.madmike.opapc.components.scoreboard.trades.OffersComponent;
import com.madmike.opapc.data.trades.Offer;
import com.madmike.opapc.util.CurrencyUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.UUID;

public class SellCommandHandler {

    public static int handleSellCommand(ServerPlayerEntity player, long price, MinecraftServer server) {
        if (price <= 0) {
            player.sendMessage(Text.literal("Price needs to be larger than 0").formatted(Formatting.RED));
            return 0;
        }
        ItemStack stack = player.getMainHandStack();

        if (stack.isEmpty()) {
            player.sendMessage(Text.literal("You're not holding any item to sell.").formatted(Formatting.RED), false);
            return 0;
        }

        OffersComponent offers = OPAPCComponents.OFFERS.get(server.getScoreboard());
        long usedSlots = offers.getOffers().values().stream().filter(e -> player.getUuid().equals(e.getOfferId())).count();

        UnlockedStoreSlotsComponent unlockedSlotsComponent = OPAPCComponents.UNLOCKED_STORE_SLOTS.get(player);
        int unlocked = unlockedSlotsComponent.getUnlockedSlots();

        if (unlocked <= usedSlots) {
            player.sendMessage(Text.literal("You don't have any available sell slots left.").formatted(Formatting.RED), false);
            return 0;
        }

        IServerPartyAPI party = OpenPACServerAPI.get(server).getPartyManager().getPartyByMember(player.getUuid());

        ItemStack listedItem = stack.copy();
        player.getMainHandStack().setCount(0);// remove the item

        Offer offer = new Offer(
                UUID.randomUUID(),
                player.getUuid(),
                listedItem,
                price,
                (party == null ? null : party.getId())
        );

        offers.addOffer(offer);

        CurrencyUtil.CoinBreakdown bd = CurrencyUtil.fromTotalBronze(price);
        player.sendMessage(Text.literal(String.format(
                        "Listed item for %d gold, %d silver, %d bronze.",
                        bd.gold(), bd.silver(), bd.bronze()))
                .formatted(Formatting.GOLD), false);
        return 1;
    }
}
