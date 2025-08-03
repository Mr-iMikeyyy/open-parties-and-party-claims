package com.madmike.opapc.war;

import com.madmike.opapc.war.data.WarData;
import com.madmike.opapc.war.state.IWarState;
import com.madmike.opapc.war.state.WarDeclaredState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import xaero.pac.common.server.parties.party.api.IServerPartyAPI;

public class War {
    private IWarState state;
    private final WarData data;

    public IWarState getState() {
        return state;
    }

    public War(WarData data) {
        this.data = data;
        this.state = new WarDeclaredState();
    }

    public void setState(IWarState state) {
        this.state = state;
    }

    public WarData getData() {
        return data;
    }

    public void tick() {
        state.tick(this);
    }

    public void onAttackerDeath(ServerPlayer player) {
        state.onAttackerDeath(player, this);
    }

    public void onDefenderDeath(ServerPlayer player) {
        state.onDefenderDeath(player, this);
    }

    public void onBlockBroken(BlockPos pos) {
        state.onWarBlockBroken(pos, this);
    }

    public boolean isPlayerParticipant(ServerPlayer player) {
        return data.isPlayerParticipant(player);
    }

    public boolean isPartyParticipant(IServerPartyAPI party) {
        return data.isPartyParticipant(party);
    }

    public void onRequestInfo(ServerPlayer player) {
        player.sendSystemMessage(data.getInfo());
    }

    public void end(EndOfWarType type) {
        state.end(this, type);
    }
}
