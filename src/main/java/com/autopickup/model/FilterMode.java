package com.autopickup.model;

/**
 * Filter mode for per-player auto-pickup item filtering.
 * NONE: pick up everything (default behaviour).
 * WHITELIST: only pick up items explicitly in the player's list.
 * BLACKLIST: pick up everything except items in the player's list.
 */
public enum FilterMode {

    NONE("§7None", "§7Pick up all items (no filter)"),
    WHITELIST("§aWhitelist", "§aOnly pick up items in the list"),
    BLACKLIST("§cBlacklist", "§cPick up all except items in the list");

    private final String displayName;
    private final String description;

    FilterMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription()  { return description; }

    /** Returns the next mode in the cycle: NONE → WHITELIST → BLACKLIST → NONE. */
    public FilterMode next() {
        FilterMode[] vals = values();
        return vals[(ordinal() + 1) % vals.length];
    }
}
