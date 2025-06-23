package com.madmike.opapc.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class OPAPCConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "opapc.json");
    private static OPAPCConfig instance;

    public int maxClaimsPerPlayer = 5;
    public boolean enableTariffs = true;
    public double scallywagDiscount = 0.5;

    public static OPAPCConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                instance = GSON.fromJson(reader, OPAPCConfig.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load OPATR config", e);
            }
        } else {
            instance = new OPAPCConfig();
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save OPATR config", e);
        }
    }
}
