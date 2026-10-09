package com.autopickup.placeholder;

import com.autopickup.AutoPickupPlugin;
import com.autopickup.manager.PlayerStateManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Characterization tests: records the current behaviour of AutoPickupPlaceholder. */
class AutoPickupPlaceholderTest {

    private AutoPickupPlugin plugin;
    private PlayerStateManager stateManager;
    private AutoPickupPlaceholder placeholder;
    private Player player;
    private final UUID uuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        plugin = mock(AutoPickupPlugin.class);
        stateManager = mock(PlayerStateManager.class);
        when(plugin.getStateManager()).thenReturn(stateManager);
        when(plugin.getPluginAuthor()).thenReturn("AutoPickup");
        when(plugin.getPluginVersion()).thenReturn("1.0.0");
        placeholder = new AutoPickupPlaceholder(plugin);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
    }

    @Test
    void metadata() {
        assertEquals("autopickup", placeholder.getIdentifier());
        assertEquals("AutoPickup", placeholder.getAuthor());
        assertEquals("1.0.0", placeholder.getVersion());
        assertEquals("AutoPickup", placeholder.getRequiredPlugin());
        assertTrue(placeholder.persist());
    }

    @Test
    void statusParamsReturnOnOrOff() {
        when(stateManager.isEnabled(uuid)).thenReturn(true);
        assertEquals("ON", placeholder.onPlaceholderRequest(player, null));
        assertEquals("ON", placeholder.onPlaceholderRequest(player, ""));
        assertEquals("ON", placeholder.onPlaceholderRequest(player, "  "));
        assertEquals("ON", placeholder.onPlaceholderRequest(player, "status"));
        assertEquals("ON", placeholder.onPlaceholderRequest(player, " STATUS "));

        when(stateManager.isEnabled(uuid)).thenReturn(false);
        assertEquals("OFF", placeholder.onPlaceholderRequest(player, "status"));
    }

    @Test
    void unknownParamReturnsNull() {
        assertNull(placeholder.onPlaceholderRequest(player, "mode"));
    }

    @Test
    void nullPlayerReturnsEmptyString() {
        assertEquals("", placeholder.onPlaceholderRequest(null, "status"));
    }

    @Test
    void missingStateManagerReturnsEmptyString() {
        when(plugin.getStateManager()).thenReturn(null);
        assertEquals("", placeholder.onPlaceholderRequest(player, "status"));
    }
}
