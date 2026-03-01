package com.autopickup.placeholder;

import com.autopickup.AutoPickupPlugin;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI expansion: %autopickup_status% returns ON or OFF.
 * Registered only when PlaceholderAPI is present.
 */
public class AutoPickupPlaceholder extends me.clip.placeholderapi.expansion.PlaceholderExpansion {

    private final AutoPickupPlugin plugin;

    public AutoPickupPlaceholder(AutoPickupPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "autopickup";
    }

    @Override
    public String getAuthor() {
        return plugin.getPluginAuthor();
    }

    @Override
    public String getVersion() {
        return plugin.getPluginVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String getRequiredPlugin() {
        return "AutoPickup";
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        }
        if (plugin.getStateManager() == null) {
            return "";
        }
        if (params == null || params.trim().isEmpty() || params.trim().equalsIgnoreCase("status")) {
            return plugin.getStateManager().isEnabled(player.getUniqueId()) ? "ON" : "OFF";
        }
        return null;
    }
}
