package com.madmike.opapc.event;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.components.scoreboard.trades.OfflineSalesComponent;
import com.madmike.opapc.data.trades.OfflineSale;
import com.madmike.opapc.util.CurrencyUtil;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

import java.util.*;

public class EventManager {
    public static void register() {

//        ServerTickEvents.END_SERVER_TICK.register(server -> {
//            if (server.getTicks() % 60 != 0) return; // ~3s at 20 TPS
//
//            IPartyManagerAPI partyManager = OpenPACServerAPI.get(server).getPartyManager();
//            List<IServerPartyAPI> allPartiesList = partyManager.getAllStream().toList();
//
//            // ✅ Cache player -> party lookup for this tick
//            Map<UUID, UUID> memberToParty = new HashMap<>();
//            for (IServerPartyAPI party : allPartiesList) {
//                UUID partyId = party.getId();
//                for (UUID member : party.getMemberInfoStream().map(IPartyMemberAPI::getUUID).toList()) {
//                    memberToParty.put(member, partyId);
//                }
//            }
//
//            // ✅ Update each player's known party if changed
//            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
//                PartyComponent comp = PlayerComponents.PARTY.get(player);
//                UUID playerId = player.getUuid();
//
//                UUID currentParty = comp.getPartyId();
//                UUID actualParty = memberToParty.get(playerId); // cached
//
//                if (!Objects.equals(currentParty, actualParty)) {
//                    comp.setPartyId(actualParty);
//                    ScoreboardComponents.OFFERS.get(server.getScoreboard()).updatePartyForPlayer(playerId, actualParty);
//                }
//            }
//
//            // ✅ Update known party names
//            KnownPartiesComponent comp = ScoreboardComponents.KNOWN_PARTIES.get(server.getScoreboard());
//            for (IServerPartyAPI party : allPartiesList) {
//                comp.addOrUpdatePartyName(new KnownParty(party.getId(), party.getDefaultName()));
//            }
//        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            UUID playerId = player.getUuid();

            OfflineSalesComponent component = OPAPCComponents.OFFLINE_SALES.get(server.getScoreboard());
            List<OfflineSale> sales = component.getSales(playerId);

            if (!sales.isEmpty()) {
                long totalProfit = sales.stream()
                        .mapToLong(OfflineSale::profitAmount)
                        .sum();

                // Give currency
                CurrencyComponent wallet = ModComponents.CURRENCY.get(player);
                wallet.modify(totalProfit);

                // Notify player
                Text coins = CurrencyUtil.formatPrice(totalProfit, false, false);
                player.sendMessage(Text.literal("§6You made " + coins + " coins while you were away!"), false);

                // Remove sales
                component.clearSales(playerId);
            }
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity) {
                OPAPCComponents.COMBAT_TIMER.get(player).onDamaged();
            }
            return ActionResult.PASS;
        });
    }
}
