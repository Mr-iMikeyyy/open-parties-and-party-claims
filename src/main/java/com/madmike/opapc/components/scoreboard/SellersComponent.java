package com.madmike.opapc.components.scoreboard;

import com.madmike.opapc.data.Seller;
import dev.onyxstudios.cca.api.v3.component.Component;
import net.minecraft.nbt.NbtCompound;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SellersComponent implements Component {

    Map<UUID, Seller> sellers = new HashMap<>();


    @Override
    public void readFromNbt(NbtCompound nbtCompound) {

    }

    @Override
    public void writeToNbt(NbtCompound nbtCompound) {

    }
}
