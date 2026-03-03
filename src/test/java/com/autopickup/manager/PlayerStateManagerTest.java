package com.autopickup.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class PlayerStateManagerTest {

    @TempDir
    File dataFolder;

    private static final Logger LOGGER = Logger.getLogger("test");

    PlayerStateManager manager;

    @BeforeEach
    void setUp() {
        manager = new PlayerStateManager(dataFolder, false, LOGGER);
        manager.load();
    }

    @Test
    void unknownPlayer_returnsDefault_false() {
        assertFalse(manager.isEnabled(UUID.randomUUID()));
    }

    @Test
    void unknownPlayer_returnsDefault_true() {
        PlayerStateManager mgr = new PlayerStateManager(dataFolder, true, LOGGER);
        mgr.load();
        assertTrue(mgr.isEnabled(UUID.randomUUID()));
    }

    @Test
    void setEnabled_true_isRetrievable() {
        UUID uuid = UUID.randomUUID();
        manager.setEnabled(uuid, true);
        assertTrue(manager.isEnabled(uuid));
    }

    @Test
    void setEnabled_false_isRetrievable() {
        UUID uuid = UUID.randomUUID();
        manager.setEnabled(uuid, false);
        assertFalse(manager.isEnabled(uuid));
    }

    @Test
    void saveAndLoad_roundtrip() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        manager.setEnabled(uuid1, true);
        manager.setEnabled(uuid2, false);
        manager.save();

        PlayerStateManager fresh = new PlayerStateManager(dataFolder, false, LOGGER);
        fresh.load();

        assertTrue(fresh.isEnabled(uuid1));
        assertFalse(fresh.isEnabled(uuid2));
    }

    @Test
    void save_whenNotDirty_doesNotThrow() {
        // Should be a no-op without error
        assertDoesNotThrow(() -> manager.save());
    }

    @Test
    void load_createsPlayersYmlIfMissing() {
        File playersFile = new File(dataFolder, "players.yml");
        assertFalse(playersFile.exists(), "precondition: file should not exist yet");
        // load() is called in setUp(); the file should now exist
        manager.load();
        assertTrue(playersFile.exists(), "players.yml should be created by load()");
    }

    @Test
    void load_afterSave_invalidUuidKeyIgnored() {
        // Manually write a players.yml with an invalid UUID key
        File playersFile = new File(dataFolder, "players.yml");
        try {
            java.nio.file.Files.writeString(playersFile.toPath(),
                    "players:\n  not-a-uuid: true\n  " + UUID.randomUUID() + ": false\n");
        } catch (Exception e) {
            fail("Could not write test file: " + e.getMessage());
        }

        PlayerStateManager fresh = new PlayerStateManager(dataFolder, true, LOGGER);
        fresh.load();
        // Invalid key is skipped — unknown player still returns the default
        assertTrue(fresh.isEnabled(UUID.randomUUID()),
                "Unknown player should return defaultEnabled=true after loading invalid keys");
    }

    @Test
    void overwrite_existingValue() {
        UUID uuid = UUID.randomUUID();
        manager.setEnabled(uuid, true);
        manager.setEnabled(uuid, false);
        assertFalse(manager.isEnabled(uuid));
    }
}
