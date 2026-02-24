package com.autopickup.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Provides tab-completion for /autopickup: suggests "on" and "off" for the first argument.
 */
public class AutoPickupTabCompleter implements TabCompleter {

    private static final List<String> COMPLETIONS = List.of("on", "off");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return List.of();
        }
        if (!sender.hasPermission("autopickup.use")) {
            return List.of();
        }
        if (args.length == 1) {
            String typed = args[0].toLowerCase();
            return COMPLETIONS.stream()
                    .filter(s -> s.startsWith(typed))
                    .toList();
        }
        return List.of();
    }
}
