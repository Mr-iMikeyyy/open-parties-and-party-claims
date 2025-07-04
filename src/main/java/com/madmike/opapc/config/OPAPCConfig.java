package com.madmike.opapc.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Path;

public class OPAPCConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("opapc.toml");

    private static CommentedFileConfig config;

    public static int maxClaimsPerParty;
    public static int maxStoreSlotsPerPlayer;
    public static boolean enableTariffs;
    public static double scallywagDiscount;
    public static int combatDurationSeconds;
    public static int teleportCooldownInSeconds;

    public static void load() {
        config = CommentedFileConfig.builder(CONFIG_PATH).autosave().build();
        config.load();

        config.setComment("maxStoreSlotsPerPlayer", "Maximum number of offers available by default to the player");
        maxStoreSlotsPerPlayer = config.getOrElse("maxStoreSlotsPerPlayer", 5);

        config.setComment("maxClaimsPerParty", "Maximum number of claims allowed per party");
        maxClaimsPerParty = config.getOrElse("maxClaimsPerParty", 5);

        config.setComment("enableTariffs", "Enable tariff system for trades");
        enableTariffs = config.getOrElse("enableTariffs", true);

        config.setComment("scallywagDiscount", "Discount factor for trades with scallywags");
        scallywagDiscount = config.getOrElse("scallywagDiscount", 0.5);

        config.setComment("combatDurationSeconds", "Duration (in seconds) that a player stays in combat after taking damage");
        combatDurationSeconds = config.getOrElse("combatDurationSeconds", 60);

        config.setComment("teleportCooldownInSeconds", "Duration (in seconds) that a player stays in combat after taking damage");
        teleportCooldownInSeconds = config.getOrElse("teleportCooldownInSeconds", 300);

        config.save();
    }
}
