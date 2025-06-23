package com.madmike.opapc.components.scoreboard;

import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.data.KnownParty;
import com.madmike.opapc.net.packets.TradeScreenRefreshS2CSender;
import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KnownPartiesComponent implements Component, AutoSyncedComponent {

    private final Scoreboard provider;
    private final Map<UUID, KnownParty> knownParties = new HashMap<>();
    private final MinecraftServer server;

    public KnownPartiesComponent(Scoreboard provider, MinecraftServer server) {
        this.provider = provider;
        this.server = server;
    }

    public Collection<KnownParty> getKnownParties() {
        return knownParties.values();
    }

    public @Nullable KnownParty get(UUID id) {
        return knownParties.get(id);
    }

    public void addOrUpdateParty(KnownParty newParty) {
        UUID id = newParty.getPartyId();
        KnownParty existing = knownParties.get(id);

        if (existing == null || !existing.getName().equals(newParty.getName())) {
            knownParties.put(id, newParty);
            OPAPCComponents.KNOWN_PARTIES.sync(provider);
            TradeScreenRefreshS2CSender.sendRebuildTabsToAll(server);
        }
    }

    public void removeParty(UUID id) {
        if (knownParties.remove(id) != null) {
            OPAPCComponents.KNOWN_PARTIES.sync(this.provider);
            TradeScreenRefreshS2CSender.sendRebuildTabsToAll(server);
        }
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        knownParties.clear();
        NbtList list = tag.getList("KnownParties", NbtCompound.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            KnownParty party = KnownParty.fromNbt(list.getCompound(i));
            knownParties.put(party.getPartyId(), party);
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        NbtList list = new NbtList();
        for (KnownParty party : knownParties.values()) {
            list.add(party.toNbt());
        }
        tag.put("KnownParties", list);
    }
}
