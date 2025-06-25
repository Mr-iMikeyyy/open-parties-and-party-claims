package com.madmike.opapc.components.scoreboard.trades;

import com.madmike.opapc.data.trades.Seller;
import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SellersComponent implements Component {

    private final Map<UUID, Seller> sellers = new HashMap<>();

    public Map<UUID, Seller> getSellers() {
        return sellers;
    }

    public Seller getSeller(UUID playerId) {
        return sellers.get(playerId);
    }

    public void addSale(UUID playerId, String name, long saleAmount) {
        sellers.merge(playerId, new Seller(name, saleAmount), (old, update) ->
                new Seller(update.name(), old.totalSales() + update.totalSales())
        );
    }

    @Override
    public void readFromNbt(NbtCompound nbt) {
        sellers.clear();
        NbtList list = nbt.getList("Sellers", NbtElement.COMPOUND_TYPE);

        for (NbtElement element : list) {
            NbtCompound sellerNbt = (NbtCompound) element;
            UUID id = sellerNbt.getUuid("Id");
            String name = sellerNbt.getString("Name");
            long totalSales = sellerNbt.getLong("Sales");
            sellers.put(id, new Seller(name, totalSales));
        }
    }

    @Override
    public void writeToNbt(NbtCompound nbt) {
        NbtList list = new NbtList();

        for (Map.Entry<UUID, Seller> entry : sellers.entrySet()) {
            NbtCompound sellerNbt = new NbtCompound();
            sellerNbt.putUuid("Id", entry.getKey());
            sellerNbt.putString("Name", entry.getValue().name());
            sellerNbt.putLong("Sales", entry.getValue().totalSales());
            list.add(sellerNbt);
        }

        nbt.put("Sellers", list);
    }
}
