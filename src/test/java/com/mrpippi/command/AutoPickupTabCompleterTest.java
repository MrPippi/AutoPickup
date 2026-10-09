package com.autopickup.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Characterization tests: records the current behaviour of AutoPickupTabCompleter. */
class AutoPickupTabCompleterTest {

    private final AutoPickupTabCompleter completer = new AutoPickupTabCompleter();
    private final Command command = mock(Command.class);

    private static <T extends CommandSender> T sender(Class<T> type, boolean canReload) {
        T s = mock(type);
        when(s.hasPermission("autopickup.reload")).thenReturn(canReload);
        return s;
    }

    @Test
    void emptyFirstArgSuggestsAllWithReloadPermission() {
        assertEquals(List.of("on", "off", "mode", "reload"),
                completer.onTabComplete(sender(Player.class, true), command, "ap", new String[]{""}));
    }

    @Test
    void reloadIsHiddenWithoutPermission() {
        assertEquals(List.of("on", "off", "mode"),
                completer.onTabComplete(sender(Player.class, false), command, "ap", new String[]{""}));
    }

    @Test
    void filtersByPrefixCaseInsensitively() {
        assertEquals(List.of("on", "off"),
                completer.onTabComplete(sender(Player.class, true), command, "ap", new String[]{"O"}));
        assertEquals(List.of("reload"),
                completer.onTabComplete(sender(Player.class, true), command, "ap", new String[]{"re"}));
    }

    @Test
    void nonPlayerSendersAlsoGetSuggestions() {
        assertEquals(List.of("on", "off", "mode", "reload"),
                completer.onTabComplete(sender(CommandSender.class, true), command, "ap", new String[]{""}));
    }

    @Test
    void useOrModePermissionIsNotChecked() {
        Player p = sender(Player.class, false);
        completer.onTabComplete(p, command, "ap", new String[]{""});
        verify(p, never()).hasPermission("autopickup.use");
        verify(p, never()).hasPermission("autopickup.mode");
    }

    @Test
    void secondArgumentGetsNoSuggestions() {
        assertEquals(List.of(),
                completer.onTabComplete(sender(Player.class, true), command, "ap", new String[]{"on", ""}));
    }
}
