package com.madmike.opapc.war2.state;

import com.madmike.opapc.war2.EndOfWarType;
import com.madmike.opapc.war2.War;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class WarEndedState implements IWarState {
    private final EndOfWarType endType;

    public WarEndedState(EndOfWarType endType) {
        this.endType = endType;
    }

    @Override
    public void tick(War war) {
        // no-op
    }

    @Override
    public void onAttackerDeath(ServerPlayer player, War war) { }

    @Override
    public void onDefenderDeath(ServerPlayer player, War war) { }

    @Override
    public void onWarBlockBroken(BlockPos pos, War war) { }

    @Override
    public void end(War war, EndOfWarType type) {
        // already ended
    }
}
