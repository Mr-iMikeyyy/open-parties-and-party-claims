package com.madmike.opapc.war2;

import com.madmike.opapc.war2.data.WarData2;
import com.madmike.opapc.war2.state.IWarState;
import com.madmike.opapc.war2.state.WarDeclaredState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class War {
    private IWarState state;
    private final WarData2 data;

    public IWarState getState() {
        return state;
    }

    public War(WarData2 data) {
        this.data = data;
        this.state = new WarDeclaredState();
    }

    public void setState(IWarState state) {
        this.state = state;
    }

    public WarData2 getData() {
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

    public boolean isParticipant(ServerPlayer player) {
        return data.isParticipant(player);
    }

    public void onRequestInfo(ServerPlayer player) {
        player.sendSystemMessage(data.getInfo());
    }

    public void end(EndOfWarType type) {
        state.end(this, type);
    }
}
