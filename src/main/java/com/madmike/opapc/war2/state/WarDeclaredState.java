package com.madmike.opapc.war2.state;

import com.madmike.opapc.OPAPCConfig;
import com.madmike.opapc.war2.EndOfWarType;
import com.madmike.opapc.war2.War;
import com.madmike.opapc.war2.event.bus.WarEventBus;
import com.madmike.opapc.war2.event.events.WarEndedEvent;
import com.madmike.opapc.war2.event.events.WarStartedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class WarDeclaredState implements IWarState {
    private final long declareTime;
    private final long warPreparationPeriodSeconds;

    public WarDeclaredState() {
        this.declareTime = System.currentTimeMillis();
        this.warPreparationPeriodSeconds = OPAPCConfig.warPreparationPeriodSeconds * 1000L;
    }

    @Override
    public void tick(War war) {
        long elapsed = System.currentTimeMillis() - declareTime;
        long remaining = warPreparationPeriodSeconds - elapsed;

        // Broadcast every 5 seconds
        if (remaining > 0 && remaining % 5000 < 50) {
            war.getData().broadcastToWar(Component.literal("§eWar begins in " + (remaining / 1000) + " seconds!"));
        }

        if (elapsed >= warPreparationPeriodSeconds) {
            war.setState(new WarStartedState());
            war.getData().setStartTime(System.currentTimeMillis());
            WarEventBus.post(new WarStartedEvent(war));
        }
    }

    @Override
    public void onAttackerDeath(ServerPlayer player, War war) {

    }

    @Override
    public void onDefenderDeath(ServerPlayer player, War war) {

    }

    @Override
    public void onWarBlockBroken(BlockPos pos, War war) {
        // Nothing yet
    }


    @Override
    public void end(War war, EndOfWarType type) {
        war.setState(new WarEndedState(type));
        WarEventBus.post(new WarEndedEvent(war, type));
    }
}
