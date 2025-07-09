package com.madmike.opapc.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.madmike.opapc.OPAPC;
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
    public static double scallywagDiscount;
    public static int combatDurationSeconds;
    public static int teleportCooldownInSeconds;
    public static boolean canOnlyAttackLargerClaims;
    public static int maxAttackerLives;
    public static int warDuration;
    public static int insuranceDurationDays;
    public static int unclaimBlocksPerWar;
    public static List<String> restartTimesRaw;
    public static List<LocalTime> restartTimes = new ArrayList<>();
    public static String restartTimezoneRaw;
    public static ZoneId restartZoneId = ZoneId.systemDefault(); // fallback if not set

    public static void load() {
        config = CommentedFileConfig.builder(CONFIG_PATH).autosave().build();
        config.load();

        config.setComment("maxStoreSlotsPerPlayer", "Maximum number of offers available by default to the player");
        maxStoreSlotsPerPlayer = config.getOrElse("maxStoreSlotsPerPlayer", 30);

        config.setComment("maxClaimsPerParty", "Maximum number of claims allowed per party");
        maxClaimsPerParty = config.getOrElse("maxClaimsPerParty", 5);

        config.setComment("enableTariffs", "Enable tariff system for trades");
        enableTariffs = config.getOrElse("enableTariffs", true);

        config.setComment("scallywagDiscount", "Discount factor for trades with scallywags");
        scallywagDiscount = config.getOrElse("scallywagDiscount", 0.5);

        config.setComment("combatDurationSeconds", "Duration (in seconds) that a player stays in combat after taking damage");
        combatDurationSeconds = config.getOrElse("combatDurationSeconds", 60);

        config.setComment("teleportCooldownInSeconds", "Duration (in seconds) that a player is not allowed to teleport after teleporting");
        teleportCooldownInSeconds = config.getOrElse("teleportCooldownInSeconds", 300);

        config.setComment("canOnlyAttackLargerClaims", "Restrict declaring wars against claims smaller than the attacker's");
        canOnlyAttackLargerClaims = config.getOrElse("canOnlyAttackLargerClaims", true);

        config.setComment("maxAttackerLives", "Number of lives the attackers have when invading a claim");
        maxAttackerLives = config.getOrElse("maxAttackerLives", 10);

        config.setComment("warDuration", "Duration (in minutes) that wars last");
        warDuration = config.getOrElse("warDuration", 10);

        config.setComment("insuranceDurationDays", "How long (in days) insurance lasts after claiming (default: 3 days)");
        insuranceDurationDays = config.getOrElse("insuranceDurationDays", 3);

        config.setComment("unclaimBlocksPerWar", "Max amount of unclaim blocks allowed to spawn per war.");
        insuranceDurationDays = config.getOrElse("unclaimBlocksPerWar", 10);

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
