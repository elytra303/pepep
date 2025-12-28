package rich.screens.clickgui.impl.autobuy.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class AutoBuySettingsManager {
    private static AutoBuySettingsManager instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path settingsPath;
    private final Path enabledPath;
    private Map<String, SavedSettings> settingsMap = new HashMap<>();
    private Map<String, Boolean> enabledMap = new HashMap<>();

    private AutoBuySettingsManager() {
        Path configDir = Paths.get("config", "rich", "autobuy");
        try {
            Files.createDirectories(configDir);
        } catch (IOException ignored) {}
        settingsPath = configDir.resolve("settings.json");
        enabledPath = configDir.resolve("enabled.json");
        loadFromFile();
    }

    public static AutoBuySettingsManager getInstance() {
        if (instance == null) {
            instance = new AutoBuySettingsManager();
        }
        return instance;
    }

    private void loadFromFile() {
        try {
            if (Files.exists(settingsPath)) {
                String json = Files.readString(settingsPath);
                Type type = new TypeToken<Map<String, SavedSettings>>(){}.getType();
                Map<String, SavedSettings> loaded = gson.fromJson(json, type);
                if (loaded != null) {
                    settingsMap = loaded;
                }
            }
        } catch (IOException ignored) {}

        try {
            if (Files.exists(enabledPath)) {
                String json = Files.readString(enabledPath);
                Type type = new TypeToken<Map<String, Boolean>>(){}.getType();
                Map<String, Boolean> loaded = gson.fromJson(json, type);
                if (loaded != null) {
                    enabledMap = loaded;
                }
            }
        } catch (IOException ignored) {}
    }

    private void saveToFile() {
        try {
            String json = gson.toJson(settingsMap);
            Files.writeString(settingsPath, json);
        } catch (IOException ignored) {}

        try {
            String json = gson.toJson(enabledMap);
            Files.writeString(enabledPath, json);
        } catch (IOException ignored) {}
    }

    public void loadSettings(String itemName, AutoBuyItemSettings settings) {
        SavedSettings saved = settingsMap.get(itemName);
        if (saved != null) {
            settings.setBuyBelow(saved.buyBelow);
            settings.setMinQuantity(saved.minQuantity);
        }
    }

    public void saveSettings(String itemName, AutoBuyItemSettings settings) {
        SavedSettings saved = new SavedSettings();
        saved.buyBelow = settings.getBuyBelow();
        saved.minQuantity = settings.getMinQuantity();
        settingsMap.put(itemName, saved);
        saveToFile();
    }

    public void saveEnabledState(String itemName, boolean enabled) {
        enabledMap.put(itemName, enabled);
        saveToFile();
    }

    public boolean getEnabledState(String itemName) {
        return enabledMap.getOrDefault(itemName, true);
    }

    public boolean hasEnabledState(String itemName) {
        return enabledMap.containsKey(itemName);
    }

    private static class SavedSettings {
        int buyBelow;
        int minQuantity = 1;
    }
}