package com.autopickup.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages per-player auto-pickup enabled state, persisted by UUID.
 * State is stored in players.yml under the plugin data folder.
 */
public class PlayerStateManager {

    private static final String FILE_NAME = "players.yml";
    private static final String KEY_PLAYERS = "players";

    private final File dataFolder;
    private final Logger logger;
    private volatile boolean defaultEnabled;
    private final Map<UUID, Boolean> stateByUuid = new ConcurrentHashMap<>();
    private File stateFile;
    private FileConfiguration stateConfig;
    private boolean dirty;

    /**
     * @param dataFolder plugin data folder (e.g. from getDataFolder())
     * @param defaultEnabled default state for players not yet in storage
     * @param logger plugin logger for errors
     */
    public PlayerStateManager(File dataFolder, boolean defaultEnabled, Logger logger) {
        this.dataFolder = dataFolder;
        this.defaultEnabled = defaultEnabled;
        this.logger = logger;
        this.stateFile = new File(dataFolder, FILE_NAME);
    }

    /**
     * Returns whether auto-pickup is enabled for the given player.
     * Uses default for unknown UUIDs.
     */
    public boolean isEnabled(UUID uuid) {
        return stateByUuid.getOrDefault(uuid, defaultEnabled);
    }

    /**
     * Updates the default state used for players not yet in storage.
     * Call this after reloading config so new players see the updated default.
     */
    public void setDefaultEnabled(boolean defaultEnabled) {
        this.defaultEnabled = defaultEnabled;
    }

    /**
     * Sets the auto-pickup state for the given player and marks data dirty for save.
     */
    public void setEnabled(UUID uuid, boolean enabled) {
        stateByUuid.put(uuid, enabled);
        dirty = true;
    }

    /**
     * Loads state from players.yml. Creates empty file if missing.
     */
    public void load() {
        stateByUuid.clear();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        if (!stateFile.exists()) {
            try {
                stateFile.createNewFile();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not create " + FILE_NAME, e);
            }
        }
        stateConfig = YamlConfiguration.loadConfiguration(stateFile);
        if (stateConfig.contains(KEY_PLAYERS) && stateConfig.isConfigurationSection(KEY_PLAYERS)) {
            for (String key : stateConfig.getConfigurationSection(KEY_PLAYERS).getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    boolean value = stateConfig.getBoolean(KEY_PLAYERS + "." + key);
                    stateByUuid.put(uuid, value);
                } catch (IllegalArgumentException ignored) {
                    // skip invalid UUID keys
                }
            }
        }
        dirty = false;
    }

    /**
     * Saves state to players.yml if dirty.
     */
    public void save() {
        if (!dirty || stateConfig == null) {
            return;
        }
        stateConfig.set(KEY_PLAYERS, null);
        for (Map.Entry<UUID, Boolean> e : stateByUuid.entrySet()) {
            stateConfig.set(KEY_PLAYERS + "." + e.getKey().toString(), e.getValue());
        }
        try {
            stateConfig.save(stateFile);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not save " + FILE_NAME, e);
        }
        dirty = false;
    }
}
