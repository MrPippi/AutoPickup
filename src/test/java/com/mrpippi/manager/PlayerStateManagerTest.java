package com.autopickup.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/** Characterization tests: records the current behaviour of PlayerStateManager. */
class PlayerStateManagerTest {

    private static final Logger LOGGER = Logger.getLogger("test");

    @TempDir
    File dir;

    private final UUID uuid = UUID.randomUUID();

    private PlayerStateManager loaded(boolean defaultEnabled) {
        PlayerStateManager m = new PlayerStateManager(dir, defaultEnabled, LOGGER);
        m.load();
        return m;
    }

    @Test
    void unknownPlayerUsesDefault() {
        assertFalse(loaded(false).isEnabled(uuid));
        assertTrue(loaded(true).isEnabled(uuid));
    }

    @Test
    void explicitStateOverridesDefault() {
        PlayerStateManager m = loaded(true);
        m.setEnabled(uuid, false);
        assertFalse(m.isEnabled(uuid));
    }

    @Test
    void loadCreatesDataFolderAndEmptyFile() {
        File nested = new File(dir, "a/b");
        new PlayerStateManager(nested, false, LOGGER).load();
        assertTrue(new File(nested, "players.yml").exists());
    }

    @Test
    void saveAndLoadRoundTrip() {
        UUID other = UUID.randomUUID();
        PlayerStateManager m = loaded(false);
        m.setEnabled(uuid, true);
        m.setEnabled(other, false);
        m.save();

        PlayerStateManager reloaded = loaded(true);
        assertTrue(reloaded.isEnabled(uuid));
        assertFalse(reloaded.isEnabled(other));

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(dir, "players.yml"));
        assertTrue(yaml.getBoolean("players." + uuid));
        assertTrue(yaml.contains("players." + other));
        assertFalse(yaml.getBoolean("players." + other));
    }

    @Test
    void saveIsNoOpWhenNotDirty() throws Exception {
        PlayerStateManager m = loaded(false);
        File file = new File(dir, "players.yml");
        Files.writeString(file.toPath(), "marker: true\n");
        m.save();
        assertEquals("marker: true\n", Files.readString(file.toPath()));
    }

    @Test
    void saveBeforeLoadIsNoOp() {
        File fresh = new File(dir, "fresh");
        PlayerStateManager m = new PlayerStateManager(fresh, false, LOGGER);
        m.setEnabled(uuid, true);
        m.save();
        assertFalse(new File(fresh, "players.yml").exists());
    }

    @Test
    void loadSkipsInvalidUuidKeys() throws Exception {
        Files.writeString(new File(dir, "players.yml").toPath(), """
                players:
                  not-a-uuid: true
                  %s: true
                """.formatted(uuid));
        PlayerStateManager m = loaded(false);
        assertTrue(m.isEnabled(uuid));
    }

    @Test
    void nonBooleanValueLoadsAsFalse() throws Exception {
        Files.writeString(new File(dir, "players.yml").toPath(), """
                players:
                  %s: "yes please"
                """.formatted(uuid));
        PlayerStateManager m = loaded(true);
        assertFalse(m.isEnabled(uuid));
    }

    @Test
    void loadDiscardsUnsavedInMemoryChanges() {
        PlayerStateManager m = loaded(false);
        m.setEnabled(uuid, true);
        m.load();
        assertFalse(m.isEnabled(uuid));
    }
}
