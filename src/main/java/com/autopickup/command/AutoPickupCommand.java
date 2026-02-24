package com.autopickup.command;

import com.autopickup.AutoPickupPlugin;
import com.autopickup.manager.PlayerStateManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles /autopickup command: toggles or sets player auto-pickup state.
 * Requires autopickup.use permission; players-only.
 */
public class AutoPickupCommand implements CommandExecutor {

    private static final String PERMISSION_USE = "autopickup.use";
    private static final String MSG_NO_PERMISSION = "messages.no-permission";
    private static final String MSG_PLAYERS_ONLY = "messages.players-only";
    private static final String MSG_TOGGLED_ON = "messages.toggled-on";
    private static final String MSG_TOGGLED_OFF = "messages.toggled-off";
    private static final String MSG_INVALID_USAGE = "messages.invalid-usage";

    private final AutoPickupPlugin plugin;
    private final PlayerStateManager stateManager;

    public AutoPickupCommand(AutoPickupPlugin plugin, PlayerStateManager stateManager) {
        this.plugin = plugin;
        this.stateManager = stateManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION_USE)) {
            sender.sendMessage(plugin.getMessage(MSG_NO_PERMISSION));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage(MSG_PLAYERS_ONLY));
            return true;
        }
        Player player = (Player) sender;
        boolean current = stateManager.isEnabled(player.getUniqueId());

        if (args.length > 0) {
            boolean isOn = args[0].trim().equalsIgnoreCase("on");
            boolean isOff = args[0].trim().equalsIgnoreCase("off");
            if (isOn) {
                stateManager.setEnabled(player.getUniqueId(), true);
                player.sendMessage(plugin.getMessage(MSG_TOGGLED_ON));
            } else if (isOff) {
                stateManager.setEnabled(player.getUniqueId(), false);
                player.sendMessage(plugin.getMessage(MSG_TOGGLED_OFF));
            } else {
                player.sendMessage(plugin.getMessage(MSG_INVALID_USAGE));
                return true;
            }
        } else {
            stateManager.setEnabled(player.getUniqueId(), !current);
            player.sendMessage(current ? plugin.getMessage(MSG_TOGGLED_OFF) : plugin.getMessage(MSG_TOGGLED_ON));
        }
        stateManager.save();
        return true;
    }
}
