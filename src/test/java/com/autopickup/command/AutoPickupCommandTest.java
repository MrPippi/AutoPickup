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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoPickupCommandTest {

    @Mock AutoPickupPlugin plugin;
    @Mock PlayerStateManager stateManager;
    @Mock FilterGuiListener filterGuiListener;
    @Mock Player player;
    @Mock CommandSender consoleSender;
    @Mock Command command;

    AutoPickupCommand cmd;
    UUID playerId;

    @BeforeEach
    void setUp() {
        cmd = new AutoPickupCommand(plugin, stateManager, filterGuiListener);
        playerId = UUID.randomUUID();

        // getMessage returns Component.text(path) so we can verify specific keys
        lenient().when(plugin.getMessage(anyString()))
                .thenAnswer(inv -> Component.text(inv.getArgument(0, String.class)));
        lenient().when(player.getUniqueId()).thenReturn(playerId);
    }

    // --- Permission checks ---

    @Test
    void use_noPermission_sendsNoPermissionMessage() {
        when(player.hasPermission("autopickup.use")).thenReturn(false);

        cmd.onCommand(player, command, "ap", new String[]{});

        verify(plugin).getMessage("messages.no-permission");
        verify(player).sendMessage(Component.text("messages.no-permission"));
        verifyNoInteractions(stateManager);
    }

    @Test
    void mode_noPermission_sendsNoPermissionMessage() {
        when(player.hasPermission("autopickup.mode")).thenReturn(false);

        cmd.onCommand(player, command, "ap", new String[]{"mode"});

        verify(plugin).getMessage("messages.no-permission");
        verifyNoInteractions(filterGuiListener);
    }

    @Test
    void reload_noPermission_sendsNoPermissionMessage() {
        when(consoleSender.hasPermission("autopickup.reload")).thenReturn(false);

        cmd.onCommand(consoleSender, command, "ap", new String[]{"reload"});

        verify(plugin).getMessage("messages.no-permission");
        verify(consoleSender).sendMessage(Component.text("messages.no-permission"));
        verify(plugin, never()).reload();
    }

    // --- Console sender ---

    @Test
    void toggle_consoleSender_sendsPlayersOnlyMessage() {
        when(consoleSender.hasPermission("autopickup.use")).thenReturn(true);

        cmd.onCommand(consoleSender, command, "ap", new String[]{});

        verify(plugin).getMessage("messages.players-only");
        verify(consoleSender).sendMessage(Component.text("messages.players-only"));
        verifyNoInteractions(stateManager);
    }

    @Test
    void mode_consoleSender_sendsPlayersOnlyMessage() {
        when(consoleSender.hasPermission("autopickup.mode")).thenReturn(true);

        cmd.onCommand(consoleSender, command, "ap", new String[]{"mode"});

        verify(plugin).getMessage("messages.players-only");
        verifyNoInteractions(filterGuiListener);
    }

    // --- Toggle (no args) ---

    @Test
    void toggle_noArgs_currentlyDisabled_enablesAndSendsOnMessage() {
        when(player.hasPermission("autopickup.use")).thenReturn(true);
        when(stateManager.isEnabled(playerId)).thenReturn(false);

        cmd.onCommand(player, command, "ap", new String[]{});

        verify(stateManager).setEnabled(playerId, true);
        verify(player).sendMessage(Component.text("messages.toggled-on"));
        verify(stateManager).save();
    }

    @Test
    void toggle_noArgs_currentlyEnabled_disablesAndSendsOffMessage() {
        when(player.hasPermission("autopickup.use")).thenReturn(true);
        when(stateManager.isEnabled(playerId)).thenReturn(true);

        cmd.onCommand(player, command, "ap", new String[]{});

        verify(stateManager).setEnabled(playerId, false);
        verify(player).sendMessage(Component.text("messages.toggled-off"));
        verify(stateManager).save();
    }

    // --- Explicit on / off ---

    @Test
    void on_arg_setsEnabledTrueAndSendsOnMessage() {
        when(player.hasPermission("autopickup.use")).thenReturn(true);

        cmd.onCommand(player, command, "ap", new String[]{"on"});

        verify(stateManager).setEnabled(playerId, true);
        verify(player).sendMessage(Component.text("messages.toggled-on"));
        verify(stateManager).save();
    }

    @Test
    void off_arg_setsEnabledFalseAndSendsOffMessage() {
        when(player.hasPermission("autopickup.use")).thenReturn(true);

        cmd.onCommand(player, command, "ap", new String[]{"off"});

        verify(stateManager).setEnabled(playerId, false);
        verify(player).sendMessage(Component.text("messages.toggled-off"));
        verify(stateManager).save();
    }

    @Test
    void on_arg_caseInsensitive() {
        when(player.hasPermission("autopickup.use")).thenReturn(true);

        cmd.onCommand(player, command, "ap", new String[]{"ON"});

        verify(stateManager).setEnabled(playerId, true);
    }

    @Test
    void off_arg_caseInsensitive() {
        when(player.hasPermission("autopickup.use")).thenReturn(true);

        cmd.onCommand(player, command, "ap", new String[]{"OFF"});

        verify(stateManager).setEnabled(playerId, false);
    }

    // --- Invalid arg ---

    @Test
    void unknownArg_sendsInvalidUsageMessage_noStateChange() {
        when(player.hasPermission("autopickup.use")).thenReturn(true);
        when(stateManager.isEnabled(playerId)).thenReturn(false);

        cmd.onCommand(player, command, "ap", new String[]{"banana"});

        verify(plugin).getMessage("messages.invalid-usage");
        verify(player).sendMessage(Component.text("messages.invalid-usage"));
        verify(stateManager).isEnabled(playerId);
        verify(stateManager, never()).setEnabled(any(UUID.class), anyBoolean());
        verify(stateManager, never()).save();
    }

    // --- mode subcommand ---

    @Test
    void mode_arg_opensFilterGui() {
        when(player.hasPermission("autopickup.mode")).thenReturn(true);

        cmd.onCommand(player, command, "ap", new String[]{"mode"});

        verify(filterGuiListener).openGui(player, 0, "");
    }

    @Test
    void mode_arg_caseInsensitive() {
        when(player.hasPermission("autopickup.mode")).thenReturn(true);

        cmd.onCommand(player, command, "ap", new String[]{"MODE"});

        verify(filterGuiListener).openGui(player, 0, "");
    }

    // --- reload subcommand ---

    @Test
    void reload_arg_withPermission_reloadsPluginAndSendsMessage() {
        when(consoleSender.hasPermission("autopickup.reload")).thenReturn(true);

        cmd.onCommand(consoleSender, command, "ap", new String[]{"reload"});

        verify(plugin).reload();
        verify(plugin).getMessage("messages.reloaded");
        verify(consoleSender).sendMessage(Component.text("messages.reloaded"));
    }

    @Test
    void reload_arg_caseInsensitive() {
        when(player.hasPermission("autopickup.reload")).thenReturn(true);

        cmd.onCommand(player, command, "ap", new String[]{"RELOAD"});

        verify(plugin).reload();
    }
}
