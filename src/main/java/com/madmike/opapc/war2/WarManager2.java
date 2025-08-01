package com.madmike.opapc.war2;

import com.madmike.opapc.war2.data.WarData2;
import com.madmike.opapc.war2.event.bus.WarEventBus;
import com.madmike.opapc.war2.event.events.WarDeclaredEvent;
import com.madmike.opapc.war2.state.WarEndedState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class WarManager2 {
    private final List<War> activeWars = new ArrayList<>();

    public static WarManager2 INSTANCE = new WarManager2();

    public List<War> getActiveWars() {
        return activeWars;
    }

    public void declareWar(IServerPartyAPI attackingParty, IServerPartyAPI defendingParty, boolean warp) {
        WarData2 data = new WarData2(attackingParty, defendingParty, warp);
        War war = new War(data);  // starts in PreparingState
        activeWars.add(war);
        WarEventBus.post(new WarDeclaredEvent(war));
    }

    public void tickAll() {
        Iterator<War> it = activeWars.iterator();
        while (it.hasNext()) {
            War war = it.next();
            war.tick();

            // cleanup if ended
            if (war.getState() instanceof WarEndedState) {
                it.remove();
            }
        }
    }

    public void handlePlayerDeath(ServerPlayer player) {
        for (War war : activeWars) {
            WarData2 data = war.getData();
            if (data.getAttackingPlayers().contains(player)) {
                war.onAttackerDeath(player);
                break;
            }
            if (data.getDefendingPlayers().contains(player)) {
                war.onDefenderDeath(player);
            }
        }
    }

    public void handleWarBlockBroken(BlockPos pos) {
        for (War war : activeWars) {
            if (war.getData().getWarBlockPosition().equals(pos)) {
                war.onBlockBroken(pos);
                break; // assuming a player is only in one war
            }
        }
    }

    public void displayWarInfo(ServerPlayer player) {
        Optional<WarData2> warOpt = getActiveWars().stream()
                .filter(war -> war.isParticipant(player))
                .findFirst();

        if (warOpt.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7You are not currently in a war."));
            return;
        }

        WarData2 war = warOpt.get();
        player.sendSystemMessage(war.getInfo());
    }
}
