package com.madmike.opapc.trade.components.scoreboard;

import com.madmike.opapc.OPAPCComponents;
import com.madmike.opapc.trade.data.OfflineSale;
import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Scoreboard;

import java.util.*;

public class OfflineSalesComponent implements Component {

    private final Scoreboard scoreboard;
    private final MinecraftServer server;

    private final Map<UUID, List<OfflineSale>> offlineSales = new HashMap<>();

    public OfflineSalesComponent(Scoreboard scoreboard, MinecraftServer server) {
        this.scoreboard = scoreboard;
        this.server = server;
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        offlineSales.clear();
        ListTag list = tag.getList("OfflineSales", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag saleTag = list.getCompound(i);
            OfflineSale sale = OfflineSale.fromNbt(saleTag);
            offlineSales.computeIfAbsent(sale.sellerID(), k -> new ArrayList<>()).add(sale);
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        ListTag list = new ListTag();
        for (List<OfflineSale> sales : offlineSales.values()) {
            for (OfflineSale sale : sales) {
                list.add(sale.toNbt());
            }
        }
        tag.put("OfflineSales", list);
    }

    /**
     * Add a new offline sale for the player.
     */
    public void addSale(UUID sellerID, long profit) {
        OfflineSale sale = new OfflineSale(sellerID, profit);
        offlineSales.computeIfAbsent(sellerID, k -> new ArrayList<>()).add(sale);
    }

    /**
     * Get all offline sales for a specific player.
     */
    public List<OfflineSale> getSales(UUID sellerID) {
        return offlineSales.getOrDefault(sellerID, Collections.emptyList());
    }

    /**
     * Check if the player has any offline sales.
     */
    public boolean hasSales(UUID sellerID) {
        return offlineSales.containsKey(sellerID);
    }

    /**
     * Clear all offline sales for the player and sync.
     */
    public void clearSales(UUID sellerID) {
        if (offlineSales.remove(sellerID) != null) {
            OPAPCComponents.OFFLINE_SALES.sync(scoreboard);
        }
    }

    /**
     * Return the entire offline sales map for persistence or advanced queries.
     */
    public Map<UUID, List<OfflineSale>> getAllOfflineSales() {
        return offlineSales;
    }
}
