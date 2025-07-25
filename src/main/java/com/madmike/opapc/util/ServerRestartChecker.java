package com.madmike.opapc.util;

import com.madmike.opapc.OPAPCConfig;

import java.time.Duration;
import java.time.LocalTime;

public class ServerRestartChecker {
    public static boolean isSafeToStartEventNow() {
        LocalTime now = LocalTime.now(OPAPCConfig.restartZoneId);

        for (LocalTime restartTime : OPAPCConfig.restartTimes) {
            long minutesUntilRestart = Duration.between(now, restartTime).toMinutes();
            if (minutesUntilRestart < 0) {
                minutesUntilRestart += 1440; // handle next-day restarts
            }
            if (minutesUntilRestart <= 30) {
                return false; // too close to restart
            }
        }
        return true; // safe to start
    }
}
