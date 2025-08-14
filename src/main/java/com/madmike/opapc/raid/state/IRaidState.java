package com.madmike.opapc.raid.state;

import com.madmike.opapc.raid.EndOfRaidType;
import com.madmike.opapc.raid.Raid;
import net.minecraft.server.level.ServerPlayer;

public interface IRaidState {
    void tick(Raid raid);
    void onRaiderDeath(ServerPlayer raider, Raid raid);
    void end(Raid raid, EndOfRaidType type);
}
