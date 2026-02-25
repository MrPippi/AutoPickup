package com.autopickup.manager;

import com.autopickup.model.FilterMode;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages per-player filter mode and item list, persisted to filters.yml.
 * FilterMode controls whether the item list acts as a whitelist, blacklist, or is ignored.
 */
public class FilterManager {

    private static final String FILE_NAME = "filters.yml";

    private final File dataFolder;
    private final Logger logger;
    private final Map<UUID, FilterMode> modeByUuid = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Material>> listByUuid = new ConcurrentHashMap<>();
    private File filterFile;
    private FileConfiguration filterConfig;
    private boolean dirty;

    public FilterManager(File dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.filterFile = new File(dataFolder, FILE_NAME);
    }

    // --- Getters / setters ---

    public FilterMode getMode(UUID uuid) {
        return modeByUuid.getOrDefault(uuid, FilterMode.NONE);
    }

    public void setMode(UUID uuid, FilterMode mode) {
        modeByUuid.put(uuid, mode);
        dirty = true;
    }

    /** Returns the mutable filter list for a player (never null). */
    public Set<Material> getFilterList(UUID uuid) {
        return listByUuid.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());
    }

    /** Toggles a material in the player's filter list. */
    public void toggleItem(UUID uuid, Material material) {
        Set<Material> list = getFilterList(uuid);
        if (!list.remove(material)) {
            list.add(material);
        }
        dirty = true;
    }

    /** Removes all materials from the player's filter list. */
    public void clearList(UUID uuid) {
        listByUuid.put(uuid, ConcurrentHashMap.newKeySet());
        dirty = true;
    }

    /**
     * Returns whether the given material should be auto-picked-up for this player,
     * taking the current filter mode into account.
     */
    public boolean shouldPickup(UUID uuid, Material material) {
        FilterMode mode = getMode(uuid);
        if (mode == FilterMode.NONE) return true;
        Set<Material> list = getFilterList(uuid);
        if (mode == FilterMode.WHITELIST) return list.contains(material);
        return !list.contains(material); // BLACKLIST
    }

    // --- Persistence ---

    public void load() {
        modeByUuid.clear();
        listByUuid.clear();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        if (!filterFile.exists()) {
            try {
                filterFile.createNewFile();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not create " + FILE_NAME, e);
            }
        }
        filterConfig = YamlConfiguration.loadConfiguration(filterFile);
        if (!filterConfig.isConfigurationSection("filters")) {
            dirty = false;
            return;
        }
        for (String uuidStr : filterConfig.getConfigurationSection("filters").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String modeStr = filterConfig.getString("filters." + uuidStr + ".mode", "NONE");
                FilterMode mode;
                try {
                    mode = FilterMode.valueOf(modeStr);
                } catch (IllegalArgumentException e) {
                    mode = FilterMode.NONE;
                }
                modeByUuid.put(uuid, mode);

                Set<Material> set = ConcurrentHashMap.newKeySet();
                for (String itemName : filterConfig.getStringList("filters." + uuidStr + ".items")) {
                    try {
                        set.add(Material.valueOf(itemName));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                listByUuid.put(uuid, set);
            } catch (IllegalArgumentException ignored) {
            }
        }
        dirty = false;
    }

    public void save() {
        if (!dirty || filterConfig == null) return;
        Set<UUID> allUuids = new HashSet<>(modeByUuid.keySet());
        allUuids.addAll(listByUuid.keySet());

        filterConfig.set("filters", null);
        for (UUID uuid : allUuids) {
            String path = "filters." + uuid;
            filterConfig.set(path + ".mode", modeByUuid.getOrDefault(uuid, FilterMode.NONE).name());
            Set<Material> items = listByUuid.getOrDefault(uuid, Collections.emptySet());
            List<String> itemNames = new ArrayList<>();
            for (Material m : items) itemNames.add(m.name());
            filterConfig.set(path + ".items", itemNames);
        }
        try {
            filterConfig.save(filterFile);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not save " + FILE_NAME, e);
        }
        dirty = false;
    }
}
