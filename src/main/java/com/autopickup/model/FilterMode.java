package com.autopickup.model;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Filter mode for per-player auto-pickup item filtering.
 * NONE: pick up everything (default behaviour).
 * WHITELIST: only pick up items explicitly in the player's list.
 * BLACKLIST: pick up everything except items in the player's list.
 *
 * <p>Display name and description are read from gui.yml when provided;
 * otherwise fallback values are used.
 */
public enum FilterMode {

    NONE("§7None", "§7Pick up all items (no filter)"),
    WHITELIST("§aWhitelist", "§aOnly pick up items in the list"),
    BLACKLIST("§cBlacklist", "§cPick up all except items in the list");

    private final String displayNameFallback;
    private final String descriptionFallback;

    FilterMode(String displayNameFallback, String descriptionFallback) {
        this.displayNameFallback = displayNameFallback;
        this.descriptionFallback = descriptionFallback;
    }

    /** Display name from gui.yml or fallback. */
    public String getDisplayName(FileConfiguration guiConfig) {
        if (guiConfig != null) {
            return guiConfig.getString("modes." + name() + ".display", displayNameFallback);
        }
        return displayNameFallback;
    }

    /** Description from gui.yml or fallback. */
    public String getDescription(FileConfiguration guiConfig) {
        if (guiConfig != null) {
            return guiConfig.getString("modes." + name() + ".description", descriptionFallback);
        }
        return descriptionFallback;
    }

    /** Returns the next mode in the cycle: NONE → WHITELIST → BLACKLIST → NONE. */
    public FilterMode next() {
        FilterMode[] vals = values();
        return vals[(ordinal() + 1) % vals.length];
    }
}
