package com.autopickup.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

/**
 * Tab-completion for /autopickup: suggests on, off, mode, and reload for the first argument.
 */
public class AutoPickupTabCompleter implements TabCompleter {

    private static final String PERMISSION_RELOAD = "autopickup.reload";
    private static final List<String> COMPLETIONS_BASE = List.of("on", "off", "mode");
    private static final List<String> COMPLETIONS_WITH_RELOAD = List.of("on", "off", "mode", "reload");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            String typed = args[0].toLowerCase();
            List<String> options = sender.hasPermission(PERMISSION_RELOAD)
                    ? COMPLETIONS_WITH_RELOAD
                    : COMPLETIONS_BASE;
            return options.stream()
                    .filter(s -> s.startsWith(typed))
                    .toList();
        }
        return List.of();
    }
}
