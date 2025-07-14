package com.madmike.opapc.raid;

import com.madmike.opapc.raid.data.RaidData;

import java.util.ArrayList;
import java.util.List;

public class RaidManager {
    public static final RaidManager INSTANCE = new RaidManager();

    private final List<RaidData> activeRaids = new ArrayList<>();

    private RaidManager() {}

    public void startRaid() {

    }
}
