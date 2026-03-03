package com.autopickup.manager;

import com.autopickup.model.FilterMode;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class FilterManagerTest {

    @TempDir
    File dataFolder;

    private static final Logger LOGGER = Logger.getLogger("test");

    FilterManager manager;
    UUID uuid;

    @BeforeEach
    void setUp() {
        manager = new FilterManager(dataFolder, LOGGER);
        manager.load();
        uuid = UUID.randomUUID();
    }

    // --- getMode ---

    @Test
    void unknownPlayer_defaultModeIsNone() {
        assertEquals(FilterMode.NONE, manager.getMode(uuid));
    }

    @Test
    void setMode_isRetrievable() {
        manager.setMode(uuid, FilterMode.WHITELIST);
        assertEquals(FilterMode.WHITELIST, manager.getMode(uuid));

        manager.setMode(uuid, FilterMode.BLACKLIST);
        assertEquals(FilterMode.BLACKLIST, manager.getMode(uuid));
    }

    // --- getFilterList ---

    @Test
    void getFilterList_unknownPlayer_returnsEmptySet() {
        Set<Material> list = manager.getFilterList(uuid);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // --- toggleItem ---

    @Test
    void toggleItem_addsWhenAbsent() {
        manager.toggleItem(uuid, Material.STONE);
        assertTrue(manager.getFilterList(uuid).contains(Material.STONE));
    }

    @Test
    void toggleItem_removesWhenPresent() {
        manager.toggleItem(uuid, Material.STONE);
        manager.toggleItem(uuid, Material.STONE);
        assertFalse(manager.getFilterList(uuid).contains(Material.STONE));
    }

    @Test
    void toggleItem_multipleMaterials() {
        manager.toggleItem(uuid, Material.STONE);
        manager.toggleItem(uuid, Material.DIRT);
        assertTrue(manager.getFilterList(uuid).contains(Material.STONE));
        assertTrue(manager.getFilterList(uuid).contains(Material.DIRT));
    }

    // --- clearList ---

    @Test
    void clearList_removesAllItems() {
        manager.toggleItem(uuid, Material.STONE);
        manager.toggleItem(uuid, Material.DIRT);
        manager.clearList(uuid);
        assertTrue(manager.getFilterList(uuid).isEmpty());
    }

    @Test
    void clearList_emptyList_doesNotThrow() {
        assertDoesNotThrow(() -> manager.clearList(uuid));
    }

    // --- shouldPickup ---

    @Test
    void shouldPickup_modeNone_alwaysTrue() {
        manager.setMode(uuid, FilterMode.NONE);
        assertTrue(manager.shouldPickup(uuid, Material.STONE));
        assertTrue(manager.shouldPickup(uuid, Material.DIRT));
    }

    @Test
    void shouldPickup_whitelist_trueOnlyForListedMaterial() {
        manager.setMode(uuid, FilterMode.WHITELIST);
        manager.toggleItem(uuid, Material.STONE);

        assertTrue(manager.shouldPickup(uuid, Material.STONE),
                "Whitelisted material should be picked up");
        assertFalse(manager.shouldPickup(uuid, Material.DIRT),
                "Non-whitelisted material should not be picked up");
    }

    @Test
    void shouldPickup_blacklist_falseOnlyForListedMaterial() {
        manager.setMode(uuid, FilterMode.BLACKLIST);
        manager.toggleItem(uuid, Material.STONE);

        assertFalse(manager.shouldPickup(uuid, Material.STONE),
                "Blacklisted material should not be picked up");
        assertTrue(manager.shouldPickup(uuid, Material.DIRT),
                "Non-blacklisted material should be picked up");
    }

    @Test
    void shouldPickup_whitelist_emptyList_nothingPickedUp() {
        manager.setMode(uuid, FilterMode.WHITELIST);
        assertFalse(manager.shouldPickup(uuid, Material.STONE));
    }

    @Test
    void shouldPickup_blacklist_emptyList_everythingPickedUp() {
        manager.setMode(uuid, FilterMode.BLACKLIST);
        assertTrue(manager.shouldPickup(uuid, Material.STONE));
    }

    @Test
    void shouldPickup_unknownPlayer_defaultNone_alwaysTrue() {
        // No mode set → defaults to NONE → always true
        assertTrue(manager.shouldPickup(UUID.randomUUID(), Material.STONE));
    }

    // --- save / load roundtrip ---

    @Test
    void saveAndLoad_modePersists() {
        manager.setMode(uuid, FilterMode.BLACKLIST);
        manager.save();

        FilterManager fresh = new FilterManager(dataFolder, LOGGER);
        fresh.load();
        assertEquals(FilterMode.BLACKLIST, fresh.getMode(uuid));
    }

    @Test
    void saveAndLoad_itemListPersists() {
        manager.toggleItem(uuid, Material.STONE);
        manager.toggleItem(uuid, Material.DIRT);
        manager.save();

        FilterManager fresh = new FilterManager(dataFolder, LOGGER);
        fresh.load();
        assertTrue(fresh.getFilterList(uuid).contains(Material.STONE));
        assertTrue(fresh.getFilterList(uuid).contains(Material.DIRT));
    }

    @Test
    void save_whenNotDirty_doesNotThrow() {
        assertDoesNotThrow(() -> manager.save());
    }

    @Test
    void load_createsFiltersYmlIfMissing() {
        File filtersFile = new File(dataFolder, "filters.yml");
        assertFalse(filtersFile.exists(), "precondition: file should not exist yet");
        manager.load();
        assertTrue(filtersFile.exists(), "filters.yml should be created by load()");
    }

    @Test
    void load_invalidUuidKey_isSkipped() {
        File filtersFile = new File(dataFolder, "filters.yml");
        try {
            java.nio.file.Files.writeString(filtersFile.toPath(),
                    "filters:\n  not-a-uuid:\n    mode: WHITELIST\n    items: []\n");
        } catch (Exception e) {
            fail("Could not write test file: " + e.getMessage());
        }

        FilterManager fresh = new FilterManager(dataFolder, LOGGER);
        fresh.load(); // should not throw
        assertEquals(FilterMode.NONE, fresh.getMode(UUID.randomUUID()));
    }

    @Test
    void load_unknownMaterialName_isSkipped() {
        UUID id = UUID.randomUUID();
        File filtersFile = new File(dataFolder, "filters.yml");
        try {
            java.nio.file.Files.writeString(filtersFile.toPath(),
                    "filters:\n  " + id + ":\n    mode: WHITELIST\n    items:\n      - NOT_A_MATERIAL\n      - STONE\n");
        } catch (Exception e) {
            fail("Could not write test file: " + e.getMessage());
        }

        FilterManager fresh = new FilterManager(dataFolder, LOGGER);
        fresh.load();
        // STONE is valid; NOT_A_MATERIAL is skipped
        assertTrue(fresh.getFilterList(id).contains(Material.STONE));
        assertEquals(1, fresh.getFilterList(id).size());
    }

    @Test
    void load_unknownModeName_defaultsToNone() {
        UUID id = UUID.randomUUID();
        File filtersFile = new File(dataFolder, "filters.yml");
        try {
            java.nio.file.Files.writeString(filtersFile.toPath(),
                    "filters:\n  " + id + ":\n    mode: INVALID_MODE\n    items: []\n");
        } catch (Exception e) {
            fail("Could not write test file: " + e.getMessage());
        }

        FilterManager fresh = new FilterManager(dataFolder, LOGGER);
        fresh.load();
        assertEquals(FilterMode.NONE, fresh.getMode(id));
    }
}
