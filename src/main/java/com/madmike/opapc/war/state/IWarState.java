package com.madmike.opapc.war.state;

import com.madmike.opapc.war.EndOfWarType;
import com.madmike.opapc.war.War;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public interface IWarState {
    void tick(War war);
    void onAttackerDeath(ServerPlayer player, War war);
    void onDefenderDeath(ServerPlayer player, War war);
    void onWarBlockBroken(BlockPos pos, War war);
    void end(War war, EndOfWarType type);
}
