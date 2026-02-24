package com.autopickup.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PlayerStateManager (load/save, isEnabled/setEnabled).
 * Runs without a game or server.
 */
class PlayerStateManagerTest {

    @TempDir
    File tempDir;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = Logger.getAnonymousLogger();
        logger.setLevel(java.util.logging.Level.OFF);
    }

    @Test
    void defaultEnabled_false_unknownUuidReturnsFalse() {
        PlayerStateManager manager = new PlayerStateManager(tempDir, false, logger);
        manager.load();
        UUID uuid = UUID.randomUUID();
        assertFalse(manager.isEnabled(uuid));
    }

    @Test
    void defaultEnabled_true_unknownUuidReturnsTrue() {
        PlayerStateManager manager = new PlayerStateManager(tempDir, true, logger);
        manager.load();
        UUID uuid = UUID.randomUUID();
        assertTrue(manager.isEnabled(uuid));
    }

    @Test
    void setEnabled_persistsAfterSaveAndLoad() throws Exception {
        PlayerStateManager manager = new PlayerStateManager(tempDir, false, logger);
        manager.load();
        UUID uuid = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        manager.setEnabled(uuid, true);
        manager.save();

        PlayerStateManager manager2 = new PlayerStateManager(tempDir, false, logger);
        manager2.load();
        assertTrue(manager2.isEnabled(uuid));
    }

    @Test
    void setEnabled_false_persistsAfterSaveAndLoad() throws Exception {
        PlayerStateManager manager = new PlayerStateManager(tempDir, true, logger);
        manager.load();
        UUID uuid = UUID.fromString("11111111-2222-3333-4444-555555555555");
        manager.setEnabled(uuid, false);
        manager.save();

        PlayerStateManager manager2 = new PlayerStateManager(tempDir, true, logger);
        manager2.load();
        assertFalse(manager2.isEnabled(uuid));
    }

    @Test
    void multiplePlayers_persistCorrectly() throws Exception {
        PlayerStateManager manager = new PlayerStateManager(tempDir, false, logger);
        manager.load();
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        manager.setEnabled(u1, true);
        manager.setEnabled(u2, false);
        manager.save();

        PlayerStateManager manager2 = new PlayerStateManager(tempDir, false, logger);
        manager2.load();
        assertTrue(manager2.isEnabled(u1));
        assertFalse(manager2.isEnabled(u2));
    }
}
