package com.autopickup.command;

import com.autopickup.AutoPickupPlugin;
import com.autopickup.gui.FilterGuiListener;
import com.autopickup.manager.PlayerStateManager;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Characterization tests: records the current behaviour of AutoPickupCommand. */
class AutoPickupCommandTest {

    @TempDir
    File dir;

    private AutoPickupPlugin plugin;
    private FilterGuiListener gui;
    private PlayerStateManager stateManager;
    private AutoPickupCommand cmd;
    private final Command command = mock(Command.class);
    private final UUID uuid = UUID.randomUUID();
    private Player player;

    @BeforeEach
    void setUp() {
        plugin = mock(AutoPickupPlugin.class);
        // Messages are returned as their config path so tests can tell them apart
        when(plugin.getMessage(anyString())).thenAnswer(inv -> Component.text(inv.getArgument(0, String.class)));
        gui = mock(FilterGuiListener.class);
        stateManager = new PlayerStateManager(dir, false, Logger.getLogger("test"));
        stateManager.load();
        cmd = new AutoPickupCommand(plugin, stateManager, gui);

        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.hasPermission(anyString())).thenReturn(true);
    }

    private boolean run(CommandSender sender, String... args) {
        return cmd.onCommand(sender, command, "autopickup", args);
    }

    private static Component msg(String path) {
        return Component.text(path);
    }

    private File playersFile() {
        return new File(dir, "players.yml");
    }

    @Test
    void noArgsTogglesStateAndSaves() {
        assertTrue(run(player));
        assertTrue(stateManager.isEnabled(uuid));
        verify(player).sendMessage(msg("messages.toggled-on"));
        assertTrue(playersFile().length() > 0, "state is saved after toggle");

        assertTrue(run(player));
        assertFalse(stateManager.isEnabled(uuid));
        verify(player).sendMessage(msg("messages.toggled-off"));
    }

    @Test
    void onAndOffAreCaseInsensitive() {
        run(player, "ON");
        assertTrue(stateManager.isEnabled(uuid));
        run(player, "Off");
        assertFalse(stateManager.isEnabled(uuid));
    }

    @Test
    void onWhenAlreadyOnStillSendsToggledOn() {
        stateManager.setEnabled(uuid, true);
        run(player, "on");
        assertTrue(stateManager.isEnabled(uuid));
        verify(player).sendMessage(msg("messages.toggled-on"));
    }

    @Test
    void unknownArgSendsInvalidUsageAndDoesNotChangeOrSave() {
        assertTrue(run(player, "foo"));
        verify(player).sendMessage(msg("messages.invalid-usage"));
        assertFalse(stateManager.isEnabled(uuid));
        assertEquals(0, playersFile().length());
    }

    @Test
    void extraArgsAreIgnored() {
        run(player, "on", "whatever");
        assertTrue(stateManager.isEnabled(uuid));
    }

    @Test
    void toggleWithoutUsePermissionIsRejected() {
        when(player.hasPermission("autopickup.use")).thenReturn(false);
        run(player, "on");
        verify(player).sendMessage(msg("messages.no-permission"));
        assertFalse(stateManager.isEnabled(uuid));
    }

    @Test
    void consoleToggleIsPlayersOnly() {
        CommandSender console = mock(CommandSender.class);
        when(console.hasPermission(anyString())).thenReturn(true);
        run(console);
        verify(console).sendMessage(msg("messages.players-only"));
    }

    @Test
    void consoleWithUnknownArgGetsPlayersOnlyNotInvalidUsage() {
        CommandSender console = mock(CommandSender.class);
        when(console.hasPermission(anyString())).thenReturn(true);
        run(console, "foo");
        verify(console).sendMessage(msg("messages.players-only"));
        verify(console, never()).sendMessage(msg("messages.invalid-usage"));
    }

    @Test
    void consoleWithoutUsePermissionGetsNoPermission() {
        CommandSender console = mock(CommandSender.class);
        run(console);
        verify(console).sendMessage(msg("messages.no-permission"));
    }

    @Test
    void modeOpensGuiAtFirstPageWithEmptySearch() {
        run(player, "MODE");
        verify(gui).openGui(player, 0, "");
    }

    @Test
    void modeWithoutPermissionIsRejected() {
        when(player.hasPermission("autopickup.mode")).thenReturn(false);
        run(player, "mode");
        verify(player).sendMessage(msg("messages.no-permission"));
        verifyNoInteractions(gui);
    }

    @Test
    void modeFromConsoleIsPlayersOnly() {
        CommandSender console = mock(CommandSender.class);
        when(console.hasPermission(anyString())).thenReturn(true);
        run(console, "mode");
        verify(console).sendMessage(msg("messages.players-only"));
        verifyNoInteractions(gui);
    }

    @Test
    void reloadWorksFromConsoleWithPermission() {
        CommandSender console = mock(CommandSender.class);
        when(console.hasPermission("autopickup.reload")).thenReturn(true);
        run(console, "Reload");
        verify(plugin).reload();
        verify(console).sendMessage(msg("messages.reloaded"));
    }

    @Test
    void reloadWithoutPermissionIsRejected() {
        when(player.hasPermission("autopickup.reload")).thenReturn(false);
        run(player, "reload");
        verify(plugin, never()).reload();
        verify(player).sendMessage(msg("messages.no-permission"));
    }
}
