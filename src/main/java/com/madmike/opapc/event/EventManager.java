package com.madmike.opapc.event;

import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.trade.data.OfflineSale;
import com.madmike.opapc.util.CurrencyUtil;
import com.madmike.opapc.war.WarManager;
import com.madmike.opapc.war.data.WarData;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTabs;

import java.util.*;

import static com.madmike.opapc.features.OPAPCFeatures.WAR_BLOCK_ITEM;


public class EventManager {
    public static void register() {

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            UUID playerId = player.getUUID();

            var component = OPAPCComponents.OFFLINE_SALES.get(server.getScoreboard());
            List<OfflineSale> sales = component.getSales(playerId);

            if (!sales.isEmpty()) {
                long totalProfit = sales.stream()
                        .mapToLong(OfflineSale::profitAmount)
                        .sum();

                // Give currency
                CurrencyComponent wallet = ModComponents.CURRENCY.get(player);
                wallet.modify(totalProfit);

                // Notify player
                Component coins = CurrencyUtil.formatPrice(totalProfit, false, false);
                player.sendSystemMessage(Component.literal("§6You made " + coins.getString() + " coins while you were away!"));

                // Remove sales
                component.clearSales(playerId);
            }

            OPAPCComponents.SELLERS.get(server.getScoreboard()).updateSellerNameIfChanged(playerId, player.getName().getString());
        });

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                OPAPCComponents.COMBAT_TIMER.get(serverPlayer).onDamaged();
            }
            return InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            if (RaidManager.getActiveRaids(serverPlayer)) {
                serverPlayer.sendSystemMessage(Component.literal("You cannot place blocks while raiding a claim."));
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) return true; // allow if not server player

            if (RaidManager.getActiveRaids(serverPlayer)) {
                serverPlayer.sendSystemMessage(Component.literal("You cannot break blocks while raiding a claim."));
                return false; // cancel the break
            }

            return true; // allow the break
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
            entries.accept(WAR_BLOCK_ITEM);
        });

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, amount) -> {

            //Checks if entity that died is a player
            if (entity instanceof ServerPlayer player) {

                //Checks if a duel is happening.
                if (dm.isDuelOngoing()) {

                    //Checks if the player is in a duel
                    if (dm.getDuelLives().containsKey(player.getUUID())) {

                        //Calculates score and teleports player
                        dm.onPlayerDeath(player);

                        //Cheats Death
                        return false;
                    }
                }


                List<WarData> activeWars = WarManager.INSTANCE.getActiveWars();

                if (!activeWars.isEmpty()) {
                    for (WarData war : activeWars) {
                        if (war.getDefendingPlayers().anyMatch(e -> e.getUUID().equals(player.getUUID()))) {
                            WarManager.INSTANCE.onPlayerDeath(war, player, false);
                            return false;
                        }
                        if (war.getAttackingPlayers().anyMatch(e -> e.getUUID().equals(player.getUUID()))) {
                            WarManager.INSTANCE.onPlayerDeath(war, player, true);
                            return false;
                        }
                    }
                }
            }
            return true; // Allows death
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            WarManager.INSTANCE.tick();
        });
    }
}
