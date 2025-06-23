package com.madmike.opapc.gui;

import com.glisco.numismaticoverhaul.ModComponents;
import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.components.scoreboard.KnownPartiesComponent;
import com.madmike.opapc.data.KnownParty;
import com.madmike.opapc.data.Offer;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import xaero.pac.client.api.OpenPACClientAPI;
import xaero.pac.client.parties.party.api.IClientPartyAPI;
import xaero.pac.common.parties.party.ally.api.IPartyAllyAPI;

import java.util.*;
import java.util.function.Predicate;


public class TradingScreen extends BaseOwoScreen<FlowLayout> {

    public TradingScreen() {
        super(Text.literal("Trading Terminal"));
    }

    TradingScreenTab currentTab;

    public TradingScreenTab getCurrentTab() {
        return this.currentTab;
    }

    UUID myOffersTabID = UUID.randomUUID();
    UUID scallywagsTabID = UUID.randomUUID();


    // Keep a reference to these containers to swap tab contents later
    private FlowLayout offerListContainer;
    private FlowLayout tabBarContents;
    private TextBoxComponent searchBox;
    private FlowLayout walletContainer;
    private boolean onlyAffordable = false;

    private long walletAmount;

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    //Called everytime the menu is opened
    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent.surface(Surface.VANILLA_TRANSLUCENT);

        // --- TAB BUTTON BAR ---
        tabBarContents = Containers.horizontalFlow(Sizing.content(), Sizing.content())
                .gap(4);

        List<TradingScreenTab> tabs = buildTabs();

        if (!tabs.isEmpty()) {
            for (TradingScreenTab tab : tabs) {
                tabBarContents.child(
                        Components.button(Text.literal(tab.name()), b -> switchTab(tab))
                );
            }
        } else {
            tabBarContents.child(Components.label(Text.literal(("No tabs to show"))));
        }


        ScrollContainer<FlowLayout> tabBarScroll = Containers.horizontalScroll(Sizing.fill(100), Sizing.content(), tabBarContents);
        rootComponent.child(tabBarScroll);

        // Wallet
        walletContainer = Containers.verticalFlow(Sizing.content(), Sizing.content());
        walletContainer.child(buildWallet());
        rootComponent.child(walletContainer);

        //SearchBox
        searchBox = Components.textBox(Sizing.fill(100));
        searchBox.setSuggestion("Search by item or seller...");
        searchBox.onChanged().subscribe(query -> refresh());
        rootComponent.child(searchBox);

        CheckboxComponent onlyAffordableCheckbox = Components.checkbox(Text.literal("Only show affordable")).onChanged(b -> {
            onlyAffordable = b;
            refresh();
        }).checked(onlyAffordable);
        rootComponent.child(onlyAffordableCheckbox);

        // --- CONTENT AREA ---
        FlowLayout mainContent = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(100)).gap(10);

        //Scrollable list of offers
        offerListContainer = Containers.verticalFlow(Sizing.content(), Sizing.fill(100)).gap(4);
        ScrollContainer<FlowLayout> scrollOffers = Containers.verticalScroll(Sizing.fill(60), Sizing.fill(100), offerListContainer);

        // Add to the main content row
        mainContent.child(scrollOffers);

        //Add Main Content to root
        rootComponent.child(mainContent);

        //Init on my offers tab
        if (!tabs.isEmpty()) {
            switchTab(tabs.get(0));
        }
    }

    public Component buildWallet() {
        if (client != null) {
            if (client.player != null) {
                long value = ModComponents.CURRENCY.get(client.player).getValue();
                walletAmount = value;
                Text text = formatPrice(value, false, false);
                return Components.label(text);
            }
        }
        return null;
    }

    public void rebuildWallet() {
        walletContainer.clearChildren();
        if (client != null) {
            if (client.player != null) {
                long value = ModComponents.CURRENCY.get(client.player).getValue();
                walletAmount = value;
                Text text = formatPrice(value, false, false);
                walletContainer.child(Components.label(text));
            }
        }
    }

    public void rebuildTabs() {
        tabBarContents.clearChildren();
        List<TradingScreenTab> newTabs = buildTabs();
        if (!newTabs.isEmpty()) {
            for (TradingScreenTab tab : newTabs) {
                tabBarContents.child(
                        Components.button(Text.literal(tab.name()), b -> switchTab(tab))
                );
            }
            if (!newTabs.contains(currentTab)) {
                currentTab = newTabs.get(0);
            }
        } else {
            tabBarContents.child(Components.label(Text.literal(("No tabs to show"))));
        }
        refresh();
    }

    private List<TradingScreenTab> buildTabs() {
        MinecraftClient shopper = MinecraftClient.getInstance();
        ClientPlayerEntity player = shopper.player;
        List<TradingScreenTab> tabs = new ArrayList<>();
        if (player != null) {

            //Add my offers
            tabs.add(new TradingScreenTab("My Offers", myOffersTabID));

            IClientPartyAPI shopperParty = OpenPACClientAPI.get().getClientPartyStorage().getParty();

            assert MinecraftClient.getInstance().world != null;
            KnownPartiesComponent knownParties = OPAPCComponents.KNOWN_PARTIES.get(MinecraftClient.getInstance().world.getScoreboard());


            if (shopperParty != null) {
                // Add my party tab
                tabs.add(new TradingScreenTab(shopperParty.getDefaultName() + " (Party)", shopperParty.getId()));

                //Add Ally parties
                List<UUID> allyIDList = shopperParty.getAllyPartiesStream().map(IPartyAllyAPI::getPartyId).toList();

                allyIDList.forEach(e ->
                        tabs.add(new TradingScreenTab(knownParties.get(e) + " (Ally)", e)));

                //Add The rest of the parties
                for (UUID partyID : knownParties.getKnownParties().stream().map(KnownParty::getPartyId).toList()) {
                    if (!allyIDList.contains(partyID) && !partyID.equals(shopperParty.getId())) {
                        tabs.add(new TradingScreenTab(Objects.requireNonNull(knownParties.get(partyID)).getName(), partyID));
                    }
                }
                // Add Scallywags
                tabs.add(new TradingScreenTab("Scallywag", scallywagsTabID));
            } else {
                // Add Scallywags
                tabs.add(new TradingScreenTab("Scallywag", scallywagsTabID));
                //Add The rest of the parties
                for (UUID partyID : knownParties.getKnownParties().stream().map(KnownParty::getPartyId).toList()) {
                    tabs.add(new TradingScreenTab(knownParties.get(partyID).getName(), partyID));
                }
            }
        }
        return tabs;
    }


    private void switchTab(TradingScreenTab tab) {
        offerListContainer.clearChildren();

        currentTab = tab;

        List<Offer> allOffers = new ArrayList<>();

        World world = MinecraftClient.getInstance().world;

        if (world != null) {
            Scoreboard sb = world.getScoreboard();
            if (sb != null) {
                allOffers = OPAPCComponents.OFFERS.get(MinecraftClient.getInstance().world.getScoreboard()).getOffers();
            }
        }

        if (allOffers.isEmpty()) return;

        Predicate<Offer> matchesSearch = offer -> {
            String query = searchBox.getText().toLowerCase().trim();
            String itemName = offer.getItem().getName().getString().toLowerCase();
            boolean matchesQuery = itemName.contains(query);
            boolean isAffordable = !onlyAffordable || offer.getPrice() <= walletAmount;
            return matchesQuery && isAffordable;
        };

        MinecraftClient shopper = MinecraftClient.getInstance();
        ClientPlayerEntity player = shopper.player;
        if (player != null) {
            UUID shopperID = player.getUuid();
            if (tab.partyId().equals(myOffersTabID)) {
                //MY offers
                List<TradeOffer> offers = allOffers.stream().filter(e -> e.sellerID().equals(player.getUuid())).filter(matchesSearch).toList();
                for (TradeOffer offer : offers) {
                    offerListContainer.child(createOfferRow(offer, formatPrice(offer.price(), false, false), false, true));
                }
                return;
            }

            OpenPACClientAPI opac = OpenPACClientAPI.get();
            IClientPartyAPI shopperParty = opac.getClientPartyStorage().getParty();


            if (shopperParty != null) { // in a party
                if (tab.partyId() == shopperParty.getId()) { // Party Offers
                    List<TradeOffer> offers = allOffers.stream().filter(e -> shopperParty.getId().equals(e.partyID()) && !e.sellerID().equals(shopperID)).filter(matchesSearch).toList();
                    for (TradeOffer offer : offers) {
                        offerListContainer.child(createOfferRow(offer, formatPrice(offer.price(), false, false), false, false));
                    }
                } else if (shopperParty.getAllyPartiesStream()// Ally Offers
                        .map(IPartyAllyAPI::getPartyId)
                        .toList()
                        .contains(tab.partyId())) {
                    List<TradeOffer> offers = allOffers.stream().filter(e -> tab.partyId().equals(e.partyID())).filter(matchesSearch).toList();
                    for (TradeOffer offer : offers) {
                        offerListContainer.child(createOfferRow(offer, formatPrice(offer.price(), true, false), true, false));
                    }
                } else if (tab.partyId() == scallywagsTabID) { //scallywag offers
                    List<TradeOffer> offers = allOffers.stream().filter(e -> e.partyID() == null).filter(matchesSearch).toList();
                    for (TradeOffer offer : offers) {
                        offerListContainer.child(createOfferRow(offer, formatPrice(offer.price(), false, true), true, false));
                    }
                } else if (tab.partyId() != null) {// other party offers

                    List<TradeOffer> offers = allOffers.stream().filter(e -> tab.partyId().equals(e.partyID())).filter(matchesSearch).toList();
                    for (TradeOffer offer : offers) {
                        offerListContainer.child(createOfferRow(offer, formatPrice(offer.price(), false, false), true, false));
                    }
                }
            } else { // not in a party
                if (tab.partyId() == scallywagsTabID) { //Scallywag
                    List<TradeOffer> offers = allOffers.stream().filter(e -> e.partyID() == null && !shopperID.equals(e.sellerID())).filter(matchesSearch).toList();
                    for (TradeOffer offer : offers) {
                        offerListContainer.child(createOfferRow(offer, formatPrice(offer.price(), true, false), true, false));
                    }
                } else { // Society
                    List<TradeOffer> offers = allOffers.stream().filter(e -> tab.partyId().equals(e.partyID())).filter(matchesSearch).toList();
                    for (TradeOffer offer : offers) {
                        offerListContainer.child(createOfferRow(offer, formatPrice(offer.price(), false, true), true, false));
                    }
                }
            }

        }
    }



    private Component createOfferRow(TradeOffer offer, Text priceText, boolean showBuyButton, boolean showRemoveButton) {
        OPATR.LOGGER.info("Rendering offer row: {}", offer.offerID());
        FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(20)).gap(6);
        row.child(Components.item(offer.item()).showOverlay(true).setTooltipFromStack(true));
        row.child(Components.label(priceText).horizontalTextAlignment(HorizontalAlignment.CENTER));
        if (showBuyButton) row.child(Components.button(Text.literal("Buy"), b -> BuyOfferC2SPacket.send(offer.offerID())));
        if (showRemoveButton) {
            row.child(Components.button(Text.literal("Remove").formatted(Formatting.RED), b -> {
                RemoveOfferC2SPacket.send(offer.offerID());
            }));
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
