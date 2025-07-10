package com.madmike.opapc.party.components.scoreboard;

import com.madmike.opapc.components.OPAPCComponents;
import com.madmike.opapc.data.parties.PartyName;
import com.madmike.opapc.net.packets.TradeScreenRefreshS2CSender;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyNamesComponent implements dev.onyxstudios.cca.api.v3.component.Component, AutoSyncedComponent {

    private final Scoreboard provider;
    private final Map<UUID, PartyName> partyNameHashMap = new HashMap<>();
    private final MinecraftServer server;

    public PartyNamesComponent(Scoreboard provider, MinecraftServer server) {
        this.provider = provider;
        this.server = server;
    }

    public Map<UUID, PartyName> getPartyNameHashMap() {
        return partyNameHashMap;
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

    public void removeParty(UUID id) { //TODO OOOOOOOOOOOOOOOOOOOOOOOOOO
        if (partyNameHashMap.remove(id) != null) {
            OPAPCComponents.PARTY_NAMES.sync(this.provider);
            TradeScreenRefreshS2CSender.sendRebuildTabsToAll(server);
        }
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        partyNameHashMap.clear();
        ListTag list = tag.getList("KnownParties", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            PartyName party = PartyName.fromNbt(list.getCompound(i));
            partyNameHashMap.put(party.getPartyId(), party);
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        ListTag list = new ListTag();
        for (PartyName party : partyNameHashMap.values()) {
            list.add(party.toNbt());
        }
        tag.put("KnownParties", list);
    }
}
