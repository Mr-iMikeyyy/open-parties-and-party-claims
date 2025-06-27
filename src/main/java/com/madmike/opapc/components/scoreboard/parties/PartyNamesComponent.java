package com.madmike.opapc.components.scoreboard.parties;

import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.data.parties.PartyName;
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

public class PartyNamesComponent implements Component, AutoSyncedComponent {

    private final Scoreboard provider;
    private final Map<UUID, PartyName> partyNameHashMap = new HashMap<>();
    private final MinecraftServer server;

    public PartyNamesComponent(Scoreboard provider, MinecraftServer server) {
        this.provider = provider;
        this.server = server;
    }

    public Collection<PartyName> getPartyNameHashMap() {
        return partyNameHashMap.values();
    }

    public @Nullable PartyName get(UUID id) {
        return partyNameHashMap.get(id);
    }

    public void addOrUpdatePartyName(PartyName newParty) {
        UUID id = newParty.getPartyId();
        PartyName existing = partyNameHashMap.get(id);

        if (existing == null || !existing.getName().equals(newParty.getName())) {
            partyNameHashMap.put(id, newParty);
            OPAPCComponents.PARTY_NAMES.sync(provider);
            TradeScreenRefreshS2CSender.sendRebuildTabsToAll(server);
        }
    }

    public void removeParty(UUID id) {
        if (partyNameHashMap.remove(id) != null) {
            OPAPCComponents.PARTY_NAMES.sync(this.provider);
            TradeScreenRefreshS2CSender.sendRebuildTabsToAll(server);
        }
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        partyNameHashMap.clear();
        NbtList list = tag.getList("KnownParties", NbtCompound.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            PartyName party = PartyName.fromNbt(list.getCompound(i));
            partyNameHashMap.put(party.getPartyId(), party);
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        NbtList list = new NbtList();
        for (PartyName party : partyNameHashMap.values()) {
            list.add(party.toNbt());
        }
        tag.put("KnownParties", list);
    }
}
