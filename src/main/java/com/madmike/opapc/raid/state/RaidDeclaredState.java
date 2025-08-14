package com.madmike.opapc.raid.state;

import com.madmike.opapc.raid.EndOfRaidType;
import com.madmike.opapc.raid.Raid;
import net.minecraft.server.level.ServerPlayer;

public class RaidDeclaredState implements IRaidState{
    @Override
    public void tick(Raid raid) {
        
    }

    @Override
    public void onRaiderDeath(ServerPlayer raider, Raid raid) {

    }

    @Override
    public void end(Raid raid, EndOfRaidType type) {

    }
}
