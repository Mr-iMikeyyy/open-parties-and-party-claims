package com.madmike.opapc.treasurehunt.events;

import com.madmike.opapc.treasurehunt.TreasureHuntManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class TreasureHuntEvents {
    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            if (server.getTickCount() >= TreasureHuntManager.INSTANCE.getNextEventTime()) {
                TreasureHuntManager.INSTANCE.setNextEventTime(server.getTickCount() + ()) nextEventTime = server.getTicks() + getRandomDelay();
                spawnGoldChest(server);
            }
        });
    }
}
