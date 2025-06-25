package com.madmike.opapc.components.scoreboard.trades;

import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.data.trades.OfflineSale;
import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;

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
    public void readFromNbt(NbtCompound tag) {
        offlineSales.clear();
        NbtList list = tag.getList("OfflineSales", NbtCompound.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound saleTag = list.getCompound(i);
            OfflineSale sale = OfflineSale.fromNbt(saleTag);
            offlineSales.computeIfAbsent(sale.sellerID(), k -> new ArrayList<>()).add(sale);
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        NbtList list = new NbtList();
        for (List<OfflineSale> sales : offlineSales.values()) {
            for (OfflineSale sale : sales) {
                list.add(sale.toNbt());
            }
        }
        tag.put("OfflineSales", list);
    }

    // Add a new sale to the list
    public void addSale(UUID sellerID, long profit) {
        OfflineSale sale = new OfflineSale(sellerID, profit);
        offlineSales.computeIfAbsent(sellerID, k -> new ArrayList<>()).add(sale);
        OPAPCComponents.OFFLINE_SALES.sync(scoreboard);
    }

    // Get all sales for a player
    public List<OfflineSale> getSales(UUID sellerID) {
        return offlineSales.getOrDefault(sellerID, Collections.emptyList());
    }

    // Check if a player has sales
    public boolean hasSales(UUID sellerID) {
        return offlineSales.containsKey(sellerID);
    }

    // Remove all sales for a player
    public void clearSales(UUID sellerID) {
        if (offlineSales.remove(sellerID) != null) {
            OPAPCComponents.OFFLINE_SALES.sync(scoreboard);
        }
    }

    // Get total sales map
    public Map<UUID, List<OfflineSale>> getAllOfflineSales() {
        return offlineSales;
    }
}
