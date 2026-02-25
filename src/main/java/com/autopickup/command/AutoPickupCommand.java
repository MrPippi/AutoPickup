package com.autopickup.command;

import com.autopickup.AutoPickupPlugin;
import com.autopickup.gui.FilterGuiListener;
import com.autopickup.manager.PlayerStateManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles /autopickup [on|off|mode]:
 *   on/off  – toggles auto-pickup state
 *   mode    – opens the filter GUI (whitelist / blacklist / none)
 *   (none)  – toggles current state
 */
public class AutoPickupCommand implements CommandExecutor {

    private static final String PERMISSION_USE  = "autopickup.use";
    private static final String PERMISSION_MODE = "autopickup.mode";

    private final AutoPickupPlugin   plugin;
    private final PlayerStateManager stateManager;
    private final FilterGuiListener  filterGuiListener;

    public AutoPickupCommand(AutoPickupPlugin plugin,
                             PlayerStateManager stateManager,
                             FilterGuiListener filterGuiListener) {
        this.plugin            = plugin;
        this.stateManager      = stateManager;
        this.filterGuiListener = filterGuiListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /autopickup mode
        if (args.length > 0 && args[0].equalsIgnoreCase("mode")) {
            if (!sender.hasPermission(PERMISSION_MODE)) {
                sender.sendMessage(plugin.getMessage("messages.no-permission"));
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getMessage("messages.players-only"));
                return true;
            }
            filterGuiListener.openGui(player, 0, "");
            return true;
        }

        // /autopickup [on|off|<toggle>]
        if (!sender.hasPermission(PERMISSION_USE)) {
            sender.sendMessage(plugin.getMessage("messages.no-permission"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessage("messages.players-only"));
            return true;
        }

        boolean current = stateManager.isEnabled(player.getUniqueId());

        if (args.length > 0) {
            boolean isOn  = args[0].equalsIgnoreCase("on");
            boolean isOff = args[0].equalsIgnoreCase("off");
            if (isOn) {
                stateManager.setEnabled(player.getUniqueId(), true);
                player.sendMessage(plugin.getMessage("messages.toggled-on"));
            } else if (isOff) {
                stateManager.setEnabled(player.getUniqueId(), false);
                player.sendMessage(plugin.getMessage("messages.toggled-off"));
            } else {
                player.sendMessage(plugin.getMessage("messages.invalid-usage"));
                return true;
            }
        } else {
            stateManager.setEnabled(player.getUniqueId(), !current);
            player.sendMessage(current
                    ? plugin.getMessage("messages.toggled-off")
                    : plugin.getMessage("messages.toggled-on"));
        }
        stateManager.save();
        return true;
    }
}
