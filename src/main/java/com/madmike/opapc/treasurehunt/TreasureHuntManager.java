package com.madmike.opapc.treasurehunt;

public class TreasureHuntManager {

    public static final TreasureHuntManager INSTANCE = new TreasureHuntManager();

    private static long nextEventTime = 0;

    private TreasureHuntManager() {}
}
