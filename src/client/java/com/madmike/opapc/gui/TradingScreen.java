package com.madmike.opapc.gui;

import com.glisco.numismaticoverhaul.ModComponents;
import com.madmike.opapc.OPAPC;
import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.components.scoreboard.parties.PartyNamesComponent;
import com.madmike.opapc.data.parties.PartyName;
import com.madmike.opapc.data.trades.Offer;
import com.madmike.opapc.net.packets.BuyOfferC2SPacket;
import com.madmike.opapc.net.packets.RemoveOfferC2SPacket;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Scoreboard;
import xaero.pac.client.api.OpenPACClientAPI;
import xaero.pac.client.parties.party.api.IClientPartyAPI;
import xaero.pac.common.parties.party.ally.api.IPartyAllyAPI;


import java.util.*;
import java.util.function.Predicate;

import static com.madmike.opapc.util.CurrencyUtil.formatPrice;


public class TradingScreen extends BaseOwoScreen<FlowLayout> {

    public TradingScreen() {
        super(net.minecraft.network.chat.Component.literal("Trading Terminal"));
    }

    TradingScreenTab currentTab;
    UUID myOffersTabID = UUID.randomUUID();
    UUID scallywagsTabID = UUID.randomUUID();

    private FlowLayout offerListContainer;
    private FlowLayout tabBarContents;
    private TextBoxComponent searchBox;
    private FlowLayout walletContainer;
    private boolean onlyAffordable = false;
    private long walletAmount;

    @Override
    protected OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent.surface(Surface.VANILLA_TRANSLUCENT);

        tabBarContents = Containers.horizontalFlow(Sizing.content(), Sizing.content()).gap(4);
        List<TradingScreenTab> tabs = buildTabs();

        if (!tabs.isEmpty()) {
            for (TradingScreenTab tab : tabs) {
                tabBarContents.child(Components.button(net.minecraft.network.chat.Component.literal(tab.name()), b -> switchTab(tab)));
            }
        } else {
            tabBarContents.child(Components.label(net.minecraft.network.chat.Component.literal("No tabs to show")));
        }

        ScrollContainer<FlowLayout> tabBarScroll = Containers.horizontalScroll(Sizing.fill(100), Sizing.content(), tabBarContents);
        rootComponent.child(tabBarScroll);

        walletContainer = Containers.verticalFlow(Sizing.content(), Sizing.content());
        walletContainer.child(buildWallet());
        rootComponent.child(walletContainer);

        searchBox = Components.textBox(Sizing.fill(100));
        searchBox.setSuggestion("Search by item or seller...");
        searchBox.onChanged().subscribe(query -> refresh());
        rootComponent.child(searchBox);

        CheckboxComponent onlyAffordableCheckbox = Components.checkbox(net.minecraft.network.chat.Component.literal("Only show affordable")).onChanged(b -> {
            onlyAffordable = b;
            refresh();
        }).checked(onlyAffordable);
        rootComponent.child(onlyAffordableCheckbox);

        FlowLayout mainContent = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(100)).gap(10);
        offerListContainer = Containers.verticalFlow(Sizing.content(), Sizing.fill(100)).gap(4);
        ScrollContainer<FlowLayout> scrollOffers = Containers.verticalScroll(Sizing.fill(60), Sizing.fill(100), offerListContainer);
        mainContent.child(scrollOffers);
        rootComponent.child(mainContent);

        if (!tabs.isEmpty()) {
            switchTab(tabs.get(0));
        }
    }

    public Component buildWallet() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            long value = ModComponents.CURRENCY.get(client.player).getValue();
            walletAmount = value;
            net.minecraft.network.chat.Component text = formatPrice(value, false, false);
            return Components.label(text);
        }
        return null;
    }

    public void rebuildWallet() {
        walletContainer.clearChildren();
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            long value = ModComponents.CURRENCY.get(client.player).getValue();
            walletAmount = value;
            net.minecraft.network.chat.Component text = formatPrice(value, false, false);
            walletContainer.child(Components.label(text));
        }
    }

    public void rebuildTabs() {
        tabBarContents.clearChildren();
        List<TradingScreenTab> newTabs = buildTabs();
        if (!newTabs.isEmpty()) {
            for (TradingScreenTab tab : newTabs) {
                tabBarContents.child(Components.button(net.minecraft.network.chat.Component.literal(tab.name()), b -> switchTab(tab)));
            }
            if (!newTabs.contains(currentTab)) {
                currentTab = newTabs.get(0);
            }
        } else {
            tabBarContents.child(Components.label(net.minecraft.network.chat.Component.literal("No tabs to show")));
        }
        refresh();
    }

    private List<TradingScreenTab> buildTabs() {
        Minecraft shopper = Minecraft.getInstance();
        LocalPlayer player = shopper.player;
        List<TradingScreenTab> tabs = new ArrayList<>();

        if (player != null) {
            tabs.add(new TradingScreenTab("My Offers", myOffersTabID));
            IClientPartyAPI shopperParty = OpenPACClientAPI.get().getClientPartyStorage().getParty();
            assert Minecraft.getInstance().level != null;
            PartyNamesComponent partyNames = OPAPCComponents.PARTY_NAMES.get(Minecraft.getInstance().level.getScoreboard());

            if (shopperParty != null) {
                tabs.add(new TradingScreenTab(shopperParty.getDefaultName() + " (Party)", shopperParty.getId()));
                List<UUID> allyIDList = shopperParty.getAllyPartiesStream().map(IPartyAllyAPI::getPartyId).toList();
                allyIDList.forEach(e -> tabs.add(new TradingScreenTab(partyNames.get(e) + " (Ally)", e)));

                for (UUID partyID : partyNames.getPartyNameHashMap().keySet().stream().toList()) {
                    if (!allyIDList.contains(partyID) && !partyID.equals(shopperParty.getId())) {
                        tabs.add(new TradingScreenTab(Objects.requireNonNull(partyNames.get(partyID)).getName(), partyID));
                    }
                }
                tabs.add(new TradingScreenTab("Scallywag", scallywagsTabID));
            } else {
                tabs.add(new TradingScreenTab("Scallywag", scallywagsTabID));
                for (UUID partyID : partyNames.getPartyNameHashMap().keySet().stream().toList()) {
                    tabs.add(new TradingScreenTab(partyNames.get(partyID).getName(), partyID));
                }
            }
        }
        return tabs;
    }

    private void switchTab(TradingScreenTab tab) {
        offerListContainer.clearChildren();
        currentTab = tab;
        Map<UUID, Offer> allOffers = new HashMap<>();

        Level world = Minecraft.getInstance().level;
        if (world != null) {
            Scoreboard sb = world.getScoreboard();
            if (sb != null) {
                allOffers = OPAPCComponents.OFFERS.get(sb).getOffers();
            }
        }

        if (allOffers.isEmpty()) return;
        Predicate<Offer> matchesSearch = offer -> {
            String query = searchBox.getMessage().getString().toLowerCase().trim();
            String itemName = offer.getItem().getDisplayName().getString().toLowerCase();
            boolean matchesQuery = itemName.contains(query);
            boolean isAffordable = !onlyAffordable || offer.getPrice() <= walletAmount;
            return matchesQuery && isAffordable;
        };

        Minecraft shopper = Minecraft.getInstance();
        LocalPlayer player = shopper.player;
        if (player != null) {
            UUID shopperID = player.getUUID();
            if (tab.partyId().equals(myOffersTabID)) {
                List<Offer> offers = allOffers.values().stream().filter(e -> e.getSellerId().equals(shopperID)).toList();
                for (Offer offer : offers) {
                    offerListContainer.child(createOfferRow(offer, formatPrice(offer.getPrice(), false, false), false, true));
                }
                return;
            }

            OpenPACClientAPI opac = OpenPACClientAPI.get();
            IClientPartyAPI shopperParty = opac.getClientPartyStorage().getParty();

            if (shopperParty != null) {
                if (tab.partyId() == shopperParty.getId()) {
                    List<Offer> offers = allOffers.values().stream().filter(e -> shopperParty.getId().equals(e.getPartyId()) && !e.getSellerId().equals(shopperID)).filter(matchesSearch).toList();
                    for (Offer offer : offers) {
                        offerListContainer.child(createOfferRow(offer, formatPrice(offer.getPrice(), false, false), false, false));
                    }
                } else if (shopperParty.getAllyPartiesStream().map(IPartyAllyAPI::getPartyId).toList().contains(tab.partyId())) {
                    List<Offer> offers = allOffers.values().stream().filter(e -> tab.partyId().equals(e.getPartyId())).filter(matchesSearch).toList();
                    for (Offer offer : offers) {
                        offerListContainer.child(createOfferRow(offer, formatPrice(offer.getPrice(), true, false), true, false));
                    }
                } else if (tab.partyId() == scallywagsTabID) {
                    List<Offer> offers = allOffers.values().stream().filter(e -> e.getPartyId() == null).filter(matchesSearch).toList();
                    for (Offer offer : offers) {
                        offerListContainer.child(createOfferRow(offer, formatPrice(offer.getPrice(), false, true), true, false));
                    }
                } else if (tab.partyId() != null) {
                    List<Offer> offers = allOffers.values().stream().filter(e -> tab.partyId().equals(e.getPartyId())).filter(matchesSearch).toList();
                    for (Offer offer : offers) {
                        offerListContainer.child(createOfferRow(offer, formatPrice(offer.getPrice(), false, false), true, false));
                    }
                }
            } else {
                if (tab.partyId() == scallywagsTabID) {
                    List<Offer> offers = allOffers.values().stream().filter(e -> e.getPartyId() == null && !shopperID.equals(e.getSellerId())).filter(matchesSearch).toList();
                    for (Offer offer : offers) {
                        offerListContainer.child(createOfferRow(offer, formatPrice(offer.getPrice(), true, false), true, false));
                    }
                } else {
                    List<Offer> offers = allOffers.values().stream().filter(e -> tab.partyId().equals(e.getPartyId())).filter(matchesSearch).toList();
                    for (Offer offer : offers) {
                        offerListContainer.child(createOfferRow(offer, formatPrice(offer.getPrice(), false, true), true, false));
                    }
                }
            }
        }
    }

    private Component createOfferRow(Offer offer, net.minecraft.network.chat.Component priceText, boolean showBuyButton, boolean showRemoveButton) {
        OPAPC.LOGGER.info("Rendering offer row: {}", offer.getOfferId());
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(20)).gap(6);
        row.child(Components.item(offer.getItem()).showOverlay(true).setTooltipFromStack(true));
        row.child(Components.label(priceText).horizontalTextAlignment(HorizontalAlignment.CENTER));
        if (showBuyButton) row.child(Components.button(net.minecraft.network.chat.Component.literal("Buy"), b -> BuyOfferC2SPacket.send(offer.getOfferId())));
        if (showRemoveButton) {
            row.child(Components.button(net.minecraft.network.chat.Component.literal("Remove").withStyle(ChatFormatting.RED), b -> RemoveOfferC2SPacket.send(offer.getOfferId())));
        }
        return row;
    }

    public void refresh() {
        if (currentTab != null) {
            switchTab(currentTab);
            rebuildWallet();
        }
    }
}
