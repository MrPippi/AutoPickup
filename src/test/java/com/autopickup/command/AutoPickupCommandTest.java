package com.autopickup.command;

import com.autopickup.AutoPickupPlugin;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

/**
 * Unit tests for /autopickup command: permission, players-only, toggle behaviour.
 * Runs without a game or server.
 */
@ExtendWith(MockitoExtension.class)
class AutoPickupCommandTest {

    @Mock
    private AutoPickupPlugin plugin;
    @Mock
    private PlayerStateManager stateManager;
    @Mock
    private CommandSender sender;
    @Mock
    private Player player;
    @Mock
    private Command command;

    private AutoPickupCommand executor;
    private static final UUID PLAYER_UUID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @BeforeEach
    void setUp() {
        executor = new AutoPickupCommand(plugin, stateManager);
        lenient().when(plugin.getMessage(anyString())).thenAnswer(inv -> Component.text((String) inv.getArgument(0)));
        lenient().when(player.getUniqueId()).thenReturn(PLAYER_UUID);
    }

    @Test
    void noPermission_sendsNoPermissionMessage() {
        when(sender.hasPermission("autopickup.use")).thenReturn(false);

        boolean result = executor.onCommand(sender, command, "autopickup", new String[0]);

        assertTrue(result);
        verify(sender).sendMessage(any(Component.class));
        verify(plugin).getMessage("messages.no-permission");
        verify(stateManager, never()).setEnabled(any(), anyBoolean());
        verify(stateManager, never()).save();
    }

    @Test
    void notPlayer_sendsPlayersOnlyMessage() {
        when(sender.hasPermission("autopickup.use")).thenReturn(true);
        // sender is CommandSender mock, not Player, so (sender instanceof Player) is false

        boolean result = executor.onCommand(sender, command, "autopickup", new String[0]);

        assertTrue(result);
        verify(sender).sendMessage(any(Component.class));
        verify(plugin).getMessage("messages.players-only");
        verify(stateManager, never()).setEnabled(any(), anyBoolean());
    }

    @Test
    void player_toggleOffToOn_setsEnabledAndSendsToggledOn() {
        when(player.hasPermission("autopickup.use")).thenReturn(true);
        when(stateManager.isEnabled(PLAYER_UUID)).thenReturn(false);

        boolean result = executor.onCommand(player, command, "autopickup", new String[0]);

        assertTrue(result);
        verify(stateManager).setEnabled(PLAYER_UUID, true);
        verify(plugin).getMessage("messages.toggled-on");
        verify(player).sendMessage(any(Component.class));
        verify(stateManager).save();
    }

    @Test
    void player_toggleOnToOff_setsEnabledAndSendsToggledOff() {
        when(player.hasPermission("autopickup.use")).thenReturn(true);
        when(stateManager.isEnabled(PLAYER_UUID)).thenReturn(true);

        boolean result = executor.onCommand(player, command, "autopickup", new String[0]);

        assertTrue(result);
        verify(stateManager).setEnabled(PLAYER_UUID, false);
        verify(plugin).getMessage("messages.toggled-off");
        verify(player).sendMessage(any(Component.class));
        verify(stateManager).save();
    }

    @Test
    void player_argOn_setsEnabledTrue() {
        when(player.hasPermission("autopickup.use")).thenReturn(true);
        when(stateManager.isEnabled(PLAYER_UUID)).thenReturn(false);

        executor.onCommand(player, command, "autopickup", new String[]{"on"});

        verify(stateManager).setEnabled(PLAYER_UUID, true);
        verify(plugin).getMessage("messages.toggled-on");
        verify(stateManager).save();
    }

    @Test
    void player_argOff_setsEnabledFalse() {
        when(player.hasPermission("autopickup.use")).thenReturn(true);
        when(stateManager.isEnabled(PLAYER_UUID)).thenReturn(true);

        executor.onCommand(player, command, "autopickup", new String[]{"off"});

        verify(stateManager).setEnabled(PLAYER_UUID, false);
        verify(plugin).getMessage("messages.toggled-off");
        verify(stateManager).save();
    }
}
