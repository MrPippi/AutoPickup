package com.autopickup.model;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FilterModeTest {

    @Test
    void next_cyclesNoneToWhitelistToBlacklistAndBack() {
        assertEquals(FilterMode.WHITELIST, FilterMode.NONE.next());
        assertEquals(FilterMode.BLACKLIST, FilterMode.WHITELIST.next());
        assertEquals(FilterMode.NONE,      FilterMode.BLACKLIST.next());
    }

    @Test
    void getDisplayName_nullConfig_returnsFallback() {
        assertNotNull(FilterMode.NONE.getDisplayName(null));
        assertNotNull(FilterMode.WHITELIST.getDisplayName(null));
        assertNotNull(FilterMode.BLACKLIST.getDisplayName(null));
    }

    @Test
    void getDescription_nullConfig_returnsFallback() {
        assertNotNull(FilterMode.NONE.getDescription(null));
        assertNotNull(FilterMode.WHITELIST.getDescription(null));
        assertNotNull(FilterMode.BLACKLIST.getDescription(null));
    }

    @Test
    void getDisplayName_configOverridesTaken() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("modes.NONE.display", "CustomNone");
        cfg.set("modes.WHITELIST.display", "CustomWhitelist");

        assertEquals("CustomNone",      FilterMode.NONE.getDisplayName(cfg));
        assertEquals("CustomWhitelist", FilterMode.WHITELIST.getDisplayName(cfg));
        // BLACKLIST has no override — falls back to hardcoded value
        assertNotNull(FilterMode.BLACKLIST.getDisplayName(cfg));
    }

    @Test
    void getDescription_configOverridesTaken() {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("modes.BLACKLIST.description", "CustomBlacklist");

        assertEquals("CustomBlacklist", FilterMode.BLACKLIST.getDescription(cfg));
        assertNotNull(FilterMode.NONE.getDescription(cfg));
    }
}
