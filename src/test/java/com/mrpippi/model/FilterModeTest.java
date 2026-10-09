package com.autopickup.model;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Characterization tests: records the current behaviour of FilterMode. */
class FilterModeTest {

    @Test
    void nextCyclesNoneWhitelistBlacklist() {
        assertEquals(FilterMode.WHITELIST, FilterMode.NONE.next());
        assertEquals(FilterMode.BLACKLIST, FilterMode.WHITELIST.next());
        assertEquals(FilterMode.NONE, FilterMode.BLACKLIST.next());
    }

    @Test
    void fallbackTextIsUsedWhenConfigIsNull() {
        assertEquals("§7None", FilterMode.NONE.getDisplayName(null));
        assertEquals("§aWhitelist", FilterMode.WHITELIST.getDisplayName(null));
        assertEquals("§cBlacklist", FilterMode.BLACKLIST.getDisplayName(null));
        assertEquals("§7Pick up all items (no filter)", FilterMode.NONE.getDescription(null));
        assertEquals("§aOnly pick up items in the list", FilterMode.WHITELIST.getDescription(null));
        assertEquals("§cPick up all except items in the list", FilterMode.BLACKLIST.getDescription(null));
    }

    @Test
    void fallbackTextIsUsedWhenKeyIsMissing() {
        YamlConfiguration cfg = new YamlConfiguration();
        assertEquals("§aWhitelist", FilterMode.WHITELIST.getDisplayName(cfg));
        assertEquals("§aOnly pick up items in the list", FilterMode.WHITELIST.getDescription(cfg));
    }

    @Test
    void configValuesOverrideFallbacks() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("modes.WHITELIST.display", "&aWL");
        cfg.set("modes.WHITELIST.description", "desc");
        assertEquals("&aWL", FilterMode.WHITELIST.getDisplayName(cfg));
        assertEquals("desc", FilterMode.WHITELIST.getDescription(cfg));
    }
}
