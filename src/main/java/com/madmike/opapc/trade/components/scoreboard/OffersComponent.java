package com.madmike.opapc.trade.components.scoreboard;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.trade.data.Offer;
import com.madmike.opapc.trade.packets.TradeScreenRefreshS2CSender;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;
import xaero.pac.common.server.api.OpenPACServerAPI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OffersComponent implements dev.onyxstudios.cca.api.v3.component.Component, AutoSyncedComponent {

    private final Map<UUID, Offer> offers = new HashMap<>();
    private final Scoreboard provider;
    private final MinecraftServer server;

    public OffersComponent(Scoreboard provider, MinecraftServer server) {
        this.provider = provider;
        this.server = server;
    }

    public Map<UUID, Offer> getOffers() {
        return offers;
    }

    public void addOffer(Offer offer) {
        offers.put(offer.getOfferId(), offer);
        OPAPCComponents.OFFERS.sync(provider);
        TradeScreenRefreshS2CSender.sendRefreshToAll(server);
    }

    public void removeOffer(UUID offerId) {
        Offer offer = offers.get(offerId);
        if (offer != null) {
            ServerPlayer seller = server.getPlayerList().getPlayer(offer.getSellerId());
            if (seller != null) {
                seller.getInventory().placeItemBackInInventory(offer.getItem());
            }
            offers.remove(offerId);
            OPAPCComponents.OFFERS.sync(provider);
            TradeScreenRefreshS2CSender.sendRefreshToAll(server);
        }
    }

    public void buyOffer(UUID offerId, Player buyer) {
        var partyManager = OpenPACServerAPI.get(server).getPartyManager();
        UUID buyerId = buyer.getUUID();
        var buyerParty = partyManager.getPartyByMember(buyerId);

        if (offers.containsKey(offerId)) {
            Offer offer = offers.get(offerId);
            UUID sellerId = offer.getSellerId();
            var sellerParty = offer.getPartyId() != null
                    ? partyManager.getPartyById(offer.getOfferId())
                    : null;

            double multiplier = 1.0;
            boolean buyerInParty = buyerParty != null;
            boolean sellerInParty = sellerParty != null;

            if (!buyerInParty && !sellerInParty) {
                multiplier = 0.5;
            } else if (buyerInParty && sellerInParty && buyerParty.isAlly(sellerParty.getId())) {
                multiplier = 0.5;
            } else if (buyerInParty != sellerInParty) {
                multiplier = 2.0;
            }

            long adjustedPrice = Math.round(offer.getPrice() * multiplier);
            CurrencyComponent buyerWallet = ModComponents.CURRENCY.get(buyer);

            if (buyerWallet.getValue() < adjustedPrice) {
                buyer.displayClientMessage(Component.literal("You can't afford this (" + adjustedPrice + " coins).").withStyle(ChatFormatting.RED), false);
                return;
            }

            ItemStack stack = offer.getItem().copy();
            if (!buyer.getInventory().add(stack)) {
                buyer.displayClientMessage(Component.literal("Not enough inventory space."), false);
                return;
            }

            buyerWallet.modify(-adjustedPrice);

            ServerPlayer seller = server.getPlayerList().getPlayer(sellerId);
            if (seller != null) {
                ModComponents.CURRENCY.get(seller).modify(adjustedPrice);
            } else {
                OPAPCComponents.OFFLINE_SALES.get(provider).addSale(sellerId, adjustedPrice);
            }

            OPAPCComponents.SELLERS.get(provider).addSale(sellerId, adjustedPrice);

            removeOffer(offerId);

            buyer.displayClientMessage(Component.literal("Purchase successful for " + adjustedPrice + " coins!").withStyle(ChatFormatting.GOLD), false);
            return;
        }

        buyer.displayClientMessage(Component.literal("Offer not found.").withStyle(ChatFormatting.RED), false);
    }

    public Offer getOffer(UUID offerId) {
        return offers.get(offerId);
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        offers.clear();
        ListTag list = tag.getList("Offers", Tag.TAG_COMPOUND);
        for (Tag e : list) {
            Offer offer = Offer.fromNbt((CompoundTag) e);
            offers.put(offer.getOfferId(), offer);
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Offer offer : offers.values()) {
            list.add(offer.toNbt());
        }
        tag.put("Offers", list);
    }

    public void updatePartyForPlayer(UUID playerId, @Nullable UUID newPartyId) {
        List<Offer> offersToChange = offers.values().stream()
                .filter(e -> e.getSellerId().equals(playerId))
                .toList();
        if (!offersToChange.isEmpty()) {
            offersToChange.forEach(e -> e.setPartyId(newPartyId));
            OPAPCComponents.OFFERS.sync(this.provider);
        }
    }
}
