package net.fortuneblack05.typerando.Server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class TypePickerConfigs {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("typerando_config.json");

    // Default settings
    public int webServerPort = 8080;
    public boolean useServerPortOffset = false;
    public int portOffset = 1;

    public static TypePickerConfigs load() {
        if (Files.exists(CONFIG_FILE)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
                return GSON.fromJson(reader, TypePickerConfigs.class);
            } catch (Exception e) {
                System.err.println("[TypeRando] Failed to load config, using defaults: " + e.getMessage());
            }
        }

        // If the file doesn't exist or fails to load, create a default one
        TypePickerConfigs defaultConfig = new TypePickerConfigs();
        defaultConfig.save();
        return defaultConfig;
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (Exception e) {
            System.err.println("[TypeRando] Failed to save config: " + e.getMessage());
        }
    }
}