package com.autopickup;

import com.autopickup.command.AutoPickupCommand;
import com.autopickup.command.AutoPickupTabCompleter;
import com.autopickup.listener.BlockBreakListener;
import com.autopickup.manager.PlayerStateManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for AutoPickup.
 * When players with auto-pickup enabled break blocks, drops go to inventory instead of ground;
 * if inventory is full, drops fall normally.
 */
public class AutoPickupPlugin extends JavaPlugin {

    private static final String PLUGIN_AUTHOR = "AutoPickup";
    private static final String PLUGIN_VERSION = "1.0.0";

    private PlayerStateManager stateManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        boolean defaultEnabled = getConfig().getBoolean("settings.default-enabled", false);
        stateManager = new PlayerStateManager(getDataFolder(), defaultEnabled, getLogger());
        stateManager.load();

        var cmd = getCommand("autopickup");
        cmd.setExecutor(new AutoPickupCommand(this, stateManager));
        cmd.setTabCompleter(new AutoPickupTabCompleter());
        getServer().getPluginManager().registerEvents(new BlockBreakListener(stateManager), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                Class<?> clazz = Class.forName("com.autopickup.placeholder.AutoPickupPlaceholder");
                Object expansion = clazz.getConstructor(AutoPickupPlugin.class).newInstance(this);
                expansion.getClass().getMethod("register").invoke(expansion);
            } catch (ReflectiveOperationException e) {
                getLogger().warning("Could not register PlaceholderAPI expansion: " + e.getMessage());
            }
        }
    }

    @Override
    public void onDisable() {
        if (stateManager != null) {
            stateManager.save();
        }
    }

    /**
     * Returns the player state manager. Never null after onEnable.
     */
    public PlayerStateManager getStateManager() {
        return stateManager;
    }

    /** Returns plugin author name for PlaceholderAPI expansion. */
    public String getPluginAuthor() {
        return PLUGIN_AUTHOR;
    }

    /** Returns plugin version for PlaceholderAPI expansion. */
    public String getPluginVersion() {
        return PLUGIN_VERSION;
    }

    /**
     * Gets a message from config and parses & color codes into an Adventure Component.
     */
    public Component getMessage(String path) {
        String msg = getConfig().getString(path, "");
        return LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
    }
}
