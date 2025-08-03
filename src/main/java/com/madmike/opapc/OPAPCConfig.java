/*
 * Copyright (C) 2025 Mr-iMikeyyy (and contributors)
 *
 * This file is part of OPAPC (Open Parties and Party Claims).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of version 3 of the GNU Lesser General Public License
 * (LGPL-3.0-only) as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.madmike.opapc;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OPAPCConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("opapc.toml");

    private static CommentedFileConfig config;

    public static int maxClaimsPerParty;
    public static int maxStoreSlotsPerPlayer;
    public static boolean enableTariffs;
    public static double discount;
    public static double markup;
    public static int warpCooldownCombatSeconds;
    public static int warpCooldownSeconds;
    public static int partyRejoinCooldownHours;
    public static boolean shouldBroadcastWarDeclarationsServerWide;
    public static boolean shouldBroadcastWarResultsToServer;
    public static int warInsuranceDurationDays;
    public static int raidInsuranceDurationDays;
    public static List<String> restartTimesRaw;
    public static List<LocalTime> restartTimes = new ArrayList<>();
    public static String restartTimezoneRaw;
    public static ZoneId restartZoneId = ZoneId.systemDefault(); // fallback if not set

    public static void load() {
        config = CommentedFileConfig.builder(CONFIG_PATH).autosave().build();
        config.load();

        config.setComment("maxClaimsPerParty", "Maximum number of claims allowed per party");
        maxClaimsPerParty = config.getOrElse("maxClaimsPerParty", 300);

        config.setComment("maxStoreSlotsPerPlayer", "Maximum number of item slots one can earn in the store");
        maxStoreSlotsPerPlayer = config.getOrElse("maxStoreSlotsPerPlayer", 30);

        config.setComment("enableTariffs", "Enable tariff system for trades");
        enableTariffs = config.getOrElse("enableTariffs", true);

        config.setComment("discount", "Discount to apply when a discount is warranted, i.e. between lone wolves or between parties and it's allies");
        discount = config.getOrElse("discount", 0.5);

        config.setComment("markup", "Markup to apply when a markup is warranted, i.e. when a lone wolf buys from a party or vice versa");
        markup = config.getOrElse("discount", 2.0);

        config.setComment("warpCooldownCombatSeconds", "Duration (in seconds) that a player cannot warp after taking damage");
        warpCooldownCombatSeconds = config.getOrElse("warpCooldownCombatSeconds", 60);

        config.setComment("warpCooldownSeconds", "Duration (in seconds) that a player is not allowed to warp after warping");
        warpCooldownSeconds = config.getOrElse("warpCooldownSeconds", 300);

        config.setComment("partyRejoinCooldownHours", "Duration (in hours) that a player is not allowed to rejoin a party after leaving");
        partyRejoinCooldownHours = config.getOrElse("partyRejoinCooldownHours", 72);

        config.setComment("shouldBroadcastWarDeclarationsServerWide", "Should war declarations be seen by everybody");
        shouldBroadcastWarDeclarationsServerWide = config.getOrElse("shouldBroadcastWarDeclarationsServerWide", true);

        config.setComment("shouldBroadcastWarResultsToServer", "Should end war results be shown to everybody");
        shouldBroadcastWarResultsToServer = config.getOrElse("shouldBroadcastWarResultsToServer", true);

        config.setComment("raidInsuranceDurationDays", "How long (in days) insurance lasts for raids");
        raidInsuranceDurationDays = config.getOrElse("raidInsuranceDurationDays", 3);

        config.setComment("warInsuranceDurationDays", "How long (in days) insurance lasts for wars, insurance for wars only given after losing a war");
        warInsuranceDurationDays = config.getOrElse("warInsuranceDurationDays", 3);

        config.setComment("restartTimes", "List of daily server restart times in HH:mm format (e.g., \"04:00\", \"12:00\", \"20:00\"). The mod will block wars and duels 30 min before these times.");
        restartTimesRaw = config.getOrElse("restartTimes", List.of());
        restartTimes = restartTimesRaw.stream().map(timeStr -> {
            try {
                return LocalTime.parse(timeStr);
            } catch (DateTimeParseException e) {
                OPAPC.LOGGER.warn("Invalid restart time format in config: " + timeStr + " (expected HH:mm). Skipping.");
                return null;
            }
        }).filter(Objects::nonNull).toList();

        config.setComment("restartTimezone", "Timezone ID for restartTimes, e.g., \"America/New_York\" or \"Europe/London\". Uses system default if invalid or unset.");
        restartTimezoneRaw = config.getOrElse("restartTimezone", ZoneId.systemDefault().toString());
        try {
            restartZoneId = ZoneId.of(restartTimezoneRaw);
        } catch (DateTimeException e) {
            OPAPC.LOGGER.warn("Invalid timezone ID in config: " + restartTimezoneRaw + ". Falling back to system default.");
            restartZoneId = ZoneId.systemDefault();
        }

        config.save();
    }
}
