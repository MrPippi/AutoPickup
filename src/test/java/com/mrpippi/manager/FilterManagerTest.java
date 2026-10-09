package com.autopickup.manager;

import com.autopickup.model.FilterMode;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/** Characterization tests: records the current behaviour of FilterManager. */
class FilterManagerTest {

    private static final Logger LOGGER = Logger.getLogger("test");

    @TempDir
    File dir;

    private FilterManager manager;
    private final UUID uuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        manager = new FilterManager(dir, LOGGER);
        manager.load();
    }

    @Test
    void unknownPlayerDefaultsToNoneWithEmptyList() {
        assertEquals(FilterMode.NONE, manager.getMode(uuid));
        assertTrue(manager.getFilterList(uuid).isEmpty());
    }

    @Test
    void noneModePicksUpEverythingRegardlessOfList() {
        manager.toggleItem(uuid, Material.DIRT);
        assertTrue(manager.shouldPickup(uuid, Material.DIRT));
        assertTrue(manager.shouldPickup(uuid, Material.STONE));
    }

    @Test
    void whitelistPicksUpOnlyListedItems() {
        manager.setMode(uuid, FilterMode.WHITELIST);
        manager.toggleItem(uuid, Material.DIAMOND);
        assertTrue(manager.shouldPickup(uuid, Material.DIAMOND));
        assertFalse(manager.shouldPickup(uuid, Material.STONE));
    }

    @Test
    void emptyWhitelistPicksUpNothing() {
        manager.setMode(uuid, FilterMode.WHITELIST);
        assertFalse(manager.shouldPickup(uuid, Material.DIAMOND));
    }

    @Test
    void blacklistPicksUpEverythingExceptListedItems() {
        manager.setMode(uuid, FilterMode.BLACKLIST);
        manager.toggleItem(uuid, Material.COBBLESTONE);
        assertFalse(manager.shouldPickup(uuid, Material.COBBLESTONE));
        assertTrue(manager.shouldPickup(uuid, Material.DIAMOND));
    }

    @Test
    void toggleItemAddsThenRemoves() {
        manager.toggleItem(uuid, Material.DIAMOND);
        assertEquals(Set.of(Material.DIAMOND), manager.getFilterList(uuid));
        manager.toggleItem(uuid, Material.DIAMOND);
        assertTrue(manager.getFilterList(uuid).isEmpty());
    }

    @Test
    void clearListEmptiesListButKeepsMode() {
        manager.setMode(uuid, FilterMode.BLACKLIST);
        manager.toggleItem(uuid, Material.DIAMOND);
        manager.clearList(uuid);
        assertTrue(manager.getFilterList(uuid).isEmpty());
        assertEquals(FilterMode.BLACKLIST, manager.getMode(uuid));
    }

    @Test
    void getFilterListReturnsLiveMutableSet() {
        manager.getFilterList(uuid).add(Material.GOLD_INGOT);
        manager.setMode(uuid, FilterMode.WHITELIST);
        assertTrue(manager.shouldPickup(uuid, Material.GOLD_INGOT));
    }

    @Test
    void loadCreatesEmptyFileWhenMissing() {
        assertTrue(new File(dir, "filters.yml").exists());
    }

    @Test
    void saveAndLoadRoundTrip() {
        manager.setMode(uuid, FilterMode.WHITELIST);
        manager.toggleItem(uuid, Material.DIAMOND);
        manager.toggleItem(uuid, Material.GOLD_INGOT);
        manager.save();

        FilterManager reloaded = new FilterManager(dir, LOGGER);
        reloaded.load();
        assertEquals(FilterMode.WHITELIST, reloaded.getMode(uuid));
        assertEquals(Set.of(Material.DIAMOND, Material.GOLD_INGOT), reloaded.getFilterList(uuid));
    }

    @Test
    void saveWritesExpectedYamlLayout() {
        manager.setMode(uuid, FilterMode.BLACKLIST);
        manager.toggleItem(uuid, Material.DIRT);
        manager.save();

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(dir, "filters.yml"));
        assertEquals("BLACKLIST", yaml.getString("filters." + uuid + ".mode"));
        assertEquals(List.of("DIRT"), yaml.getStringList("filters." + uuid + ".items"));
    }

    @Test
    void playerWithOnlyListAndNoModeIsSavedAsNone() {
        manager.toggleItem(uuid, Material.DIRT);
        manager.save();

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(dir, "filters.yml"));
        assertEquals("NONE", yaml.getString("filters." + uuid + ".mode"));
    }

    @Test
    void readingFilterListAloneCreatesEntryThatGetsSavedOnNextDirtySave() {
        UUID other = UUID.randomUUID();
        manager.getFilterList(other); // side effect: computeIfAbsent
        manager.setMode(uuid, FilterMode.WHITELIST);
        manager.save();

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(dir, "filters.yml"));
        assertEquals("NONE", yaml.getString("filters." + other + ".mode"));
        assertTrue(yaml.getStringList("filters." + other + ".items").isEmpty());
    }

    @Test
    void saveIsNoOpWhenNotDirty() throws Exception {
        File file = new File(dir, "filters.yml");
        Files.writeString(file.toPath(), "marker: true\n");
        manager.save();
        assertEquals("marker: true\n", Files.readString(file.toPath()));
    }

    @Test
    void saveBeforeLoadIsNoOp() {
        File fresh = new File(dir, "fresh");
        FilterManager notLoaded = new FilterManager(fresh, LOGGER);
        notLoaded.setMode(uuid, FilterMode.WHITELIST);
        notLoaded.save();
        assertFalse(new File(fresh, "filters.yml").exists());
    }

    @Test
    void loadSkipsInvalidUuidsAndUnknownItemsAndFallsBackToNoneForUnknownMode() throws Exception {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        Files.writeString(new File(dir, "filters.yml").toPath(), """
                filters:
                  not-a-uuid:
                    mode: WHITELIST
                    items: [DIAMOND]
                  %s:
                    mode: SOMETHING_ELSE
                    items: [DIAMOND, NOT_A_MATERIAL]
                  %s:
                    items: [STONE]
                """.formatted(a, b));
        manager.load();

        assertEquals(FilterMode.NONE, manager.getMode(a));
        assertEquals(Set.of(Material.DIAMOND), manager.getFilterList(a));
        assertEquals(FilterMode.NONE, manager.getMode(b));
        assertEquals(Set.of(Material.STONE), manager.getFilterList(b));
    }

    @Test
    void modeNamesAreCaseSensitiveOnLoad() throws Exception {
        Files.writeString(new File(dir, "filters.yml").toPath(), """
                filters:
                  %s:
                    mode: whitelist
                """.formatted(uuid));
        manager.load();
        assertEquals(FilterMode.NONE, manager.getMode(uuid));
    }

    @Test
    void loadDiscardsUnsavedInMemoryChanges() {
        manager.setMode(uuid, FilterMode.WHITELIST);
        manager.load();
        assertEquals(FilterMode.NONE, manager.getMode(uuid));
    }
}
