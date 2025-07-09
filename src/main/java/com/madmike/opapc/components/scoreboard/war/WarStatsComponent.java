package com.madmike.opapc.components.scoreboard.war;

import com.madmike.opapc.data.war.WarStat;
import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WarStatsComponent implements ComponentV3 {
    private final Map<UUID, WarStat> warStats = new HashMap<>();
    private final Scoreboard provider;
    private final MinecraftServer server;

    public WarStatsComponent(Scoreboard scoreboard, MinecraftServer server) {
        this.provider = scoreboard;
        this.server = server;
    }

    public WarStat getOrCreate(UUID partyId) {
        return warStats.computeIfAbsent(partyId, WarStat::new);
    }

    public Map<UUID, WarStat> getAll() {
        return warStats;
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        warStats.clear();
        ListTag list = tag.getList("warStats", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag c = (CompoundTag) t;
            WarStat stat = WarStat.fromNbt(c);
            warStats.put(stat.getPartyId(), stat);
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        ListTag list = new ListTag();
        for (WarStat stat : warStats.values()) {
            list.add(stat.toNbt());
        }
        tag.put("warStats", list);
    }
}
