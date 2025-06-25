package com.madmike.opapc.components.scoreboard.trades;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.data.trades.Offer;
import com.madmike.opapc.net.packets.TradeScreenRefreshS2CSender;
import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import xaero.pac.common.server.api.OpenPACServerAPI;
import xaero.pac.common.server.parties.party.api.IPartyManagerAPI;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OffersComponent implements Component, AutoSyncedComponent {

    private final Map<UUID, Offer> offers = new HashMap<>();
    private final Scoreboard provider;
    private final MinecraftServer server;

    public OffersComponent(Scoreboard provider, MinecraftServer server) {
        this.provider = provider;
        this.server = server; // Removed unused 'server' parameter
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
            ServerPlayerEntity seller = server.getPlayerManager().getPlayer(offer.getSellerId());
            if (seller != null) {
                seller.giveItemStack(offer.getItem());
            }
            offers.remove(offerId);
            OPAPCComponents.OFFERS.sync(provider);
            TradeScreenRefreshS2CSender.sendRefreshToAll(server);
        }
    }

    public void buyOffer(UUID offerId, PlayerEntity buyer) {
        IPartyManagerAPI partyManager = OpenPACServerAPI.get(server).getPartyManager();
        UUID buyerId = buyer.getUuid();
        IServerPartyAPI buyerParty = partyManager.getPartyByMember(buyerId);

        if (offers.containsKey(offerId)) {
            Offer offer = offers.get(offerId);
            UUID sellerId = offer.getSellerId();
            IServerPartyAPI sellerParty = offer.getPartyId() != null
                    ? partyManager.getPartyById(offer.getOfferId())
                    : null;

            double multiplier = 1.0;

            // Determine economic relationship
            boolean buyerInParty = buyerParty != null;
            boolean sellerInParty = sellerParty != null;

            if (!buyerInParty && !sellerInParty) {
                multiplier = 0.5; // scallywag to scallywag
            } else if (buyerInParty && sellerInParty && buyerParty.isAlly(sellerParty.getId())) {
                multiplier = 0.5; // allies
            } else if (buyerInParty != sellerInParty) {
                multiplier = 2.0; // one is a scallywag, the other is not
            }

            long adjustedPrice = Math.round(offer.getPrice() * multiplier);
            CurrencyComponent buyerWallet = ModComponents.CURRENCY.get(buyer);

            // Check if buyer can afford
            if (buyerWallet.getValue() < adjustedPrice) {
                buyer.sendMessage(Text.literal("You can't afford this (" + adjustedPrice + " coins).").formatted(Formatting.RED), false);
                return;
            }

            // Transfer item
            ItemStack stack = offer.getItem().copy();
            if (!buyer.getInventory().insertStack(stack)) {
                buyer.sendMessage(Text.literal("Not enough inventory space."), false);
                return;
            }

            // Currency exchange
            buyerWallet.modify(-adjustedPrice);

            ServerPlayerEntity seller = server.getPlayerManager() != null ? server.getPlayerManager().getPlayer(sellerId) : null;

            if (seller != null) {
                ModComponents.CURRENCY.get(seller).modify(adjustedPrice);
            } else {
                OPAPCComponents.OFFLINE_SALES.get(provider).addSale(sellerId, adjustedPrice);
            }

            //Update Seller Record
            OPAPCComponents.SELLERS.get(provider).addSale(sellerId, adjustedPrice);

            // Remove offer and sync
            removeOffer(offerId);

            buyer.sendMessage(Text.literal("Purchase successful for " + adjustedPrice + " coins!").formatted(Formatting.GOLD), false);
            return;
        }

        buyer.sendMessage(Text.literal("Offer not found.").formatted(Formatting.RED), false);
    }

    public Offer getOffer(UUID offerId) {
        return offers.get(offerId);
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        offers.clear();
        NbtList list = tag.getList("Offers", NbtElement.COMPOUND_TYPE);
        for (NbtElement e : list) {
            Offer offer = Offer.fromNbt((NbtCompound) e);
            offers.put(offer.getOfferId(), offer);
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        NbtList list = new NbtList();
        for (Offer offer : offers.values()) {
            list.add(offer.toNbt());
        }
        tag.put("Offers", list);
    }

    public void updatePartyForPlayer(UUID playerId, @Nullable UUID newPartyId) {
        List<Offer> offersToChange = offers.values().stream().filter(e -> e.getSellerId().equals(playerId)).toList();
        if (!offersToChange.isEmpty()) {
            offersToChange.forEach(e -> {
                e.setPartyId(newPartyId);
            });
            OPAPCComponents.OFFERS.sync(this.provider);
        }
    }
}
