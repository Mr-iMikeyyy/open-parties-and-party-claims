package com.madmike.opapc.war.components.scoreboard;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.server.level.ServerPlayer;

public class WarTimer implements Component, AutoSyncedComponent {

    @Override
    public boolean shouldSyncWith(ServerPlayer player) {
        return WarManager
    }
}
