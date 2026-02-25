package com.autopickup;

import com.autopickup.command.AutoPickupCommand;
import com.autopickup.command.AutoPickupTabCompleter;
import com.autopickup.gui.FilterGuiListener;
import com.autopickup.listener.BlockBreakListener;
import com.autopickup.manager.FilterManager;
import com.autopickup.manager.PlayerStateManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/**
 * Main plugin class for AutoPickup.
 * When players with auto-pickup enabled break blocks, drops go to inventory instead of ground;
 * if inventory is full, drops fall normally.
 */
public class AutoPickupPlugin extends JavaPlugin {

    private static final String PLUGIN_AUTHOR  = "AutoPickup";
    private static final String PLUGIN_VERSION = "1.0.0";

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * Mapping from legacy color/format code characters to their MiniMessage tag names.
     * Used by {@link #legacyToMiniMessage(String)} to convert & codes before parsing.
     */
    private static final Map<Character, String> LEGACY_MAP = Map.ofEntries(
            Map.entry('0', "black"),
            Map.entry('1', "dark_blue"),
            Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"),
            Map.entry('4', "dark_red"),
            Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"),
            Map.entry('7', "gray"),
            Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"),
            Map.entry('a', "green"),
            Map.entry('b', "aqua"),
            Map.entry('c', "red"),
            Map.entry('d', "light_purple"),
            Map.entry('e', "yellow"),
            Map.entry('f', "white"),
            Map.entry('k', "obfuscated"),
            Map.entry('l', "bold"),
            Map.entry('m', "strikethrough"),
            Map.entry('n', "underlined"),
            Map.entry('o', "italic"),
            Map.entry('r', "reset")
    );

    private PlayerStateManager stateManager;
    private FilterManager filterManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        boolean defaultEnabled = getConfig().getBoolean("settings.default-enabled", false);

        stateManager  = new PlayerStateManager(getDataFolder(), defaultEnabled, getLogger());
        stateManager.load();

        filterManager = new FilterManager(getDataFolder(), getLogger());
        filterManager.load();

        FilterGuiListener filterGuiListener = new FilterGuiListener(this, filterManager);

        var cmd = getCommand("autopickup");
        cmd.setExecutor(new AutoPickupCommand(this, stateManager, filterGuiListener));
        cmd.setTabCompleter(new AutoPickupTabCompleter());

        getServer().getPluginManager().registerEvents(
                new BlockBreakListener(stateManager, filterManager), this);
        getServer().getPluginManager().registerEvents(filterGuiListener, this);

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
        if (stateManager  != null) stateManager.save();
        if (filterManager != null) filterManager.save();
    }

    public PlayerStateManager getStateManager()  { return stateManager;  }
    public FilterManager      getFilterManager() { return filterManager; }

    /** Returns plugin author name for PlaceholderAPI expansion. */
    public String getPluginAuthor()  { return PLUGIN_AUTHOR;  }
    /** Returns plugin version for PlaceholderAPI expansion. */
    public String getPluginVersion() { return PLUGIN_VERSION; }

    /**
     * Reads a message from config.yml and returns it as an Adventure Component.
     *
     * <p>Supports two colour/format syntaxes simultaneously:
     * <ul>
     *   <li><b>MiniMessage</b> – {@code <yellow>Hello <#00ff00>World</yellow>}
     *   <li><b>Legacy &amp; codes</b> – {@code &aGreen &cRed &#00ff00Hex}
     *       (converted to MiniMessage tags before parsing, so both can be mixed)
     * </ul>
     */
    public Component getMessage(String path) {
        String msg = getConfig().getString(path, "");
        return MINI_MESSAGE.deserialize(legacyToMiniMessage(msg));
    }

    /**
     * Pre-processes a string by converting legacy {@code &} / {@code §} color codes
     * into equivalent MiniMessage tags so that both syntaxes can coexist.
     *
     * <p>Supported conversions:
     * <ul>
     *   <li>{@code &a} → {@code <green>}, {@code &c} → {@code <red>}, … (all 0-9, a-f, k-r)
     *   <li>{@code &#RRGGBB} → {@code <#RRGGBB>}  (Spigot/Essentials hex format)
     *   <li>{@code §} variants of the above
     * </ul>
     */
    static String legacyToMiniMessage(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < text.length()) {
                // Spigot/Essentials hex: &#RRGGBB  (§# not standard, but handle it too)
                if (text.charAt(i + 1) == '#' && i + 7 < text.length()) {
                    String hex = text.substring(i + 2, i + 8);
                    if (hex.matches("[0-9a-fA-F]{6}")) {
                        out.append('<').append('#').append(hex).append('>');
                        i += 7; // &, #, R, R, G, G, B, B  → skip 7 more after i
                        continue;
                    }
                }
                // Standard legacy code
                char code = Character.toLowerCase(text.charAt(i + 1));
                String tag = LEGACY_MAP.get(code);
                if (tag != null) {
                    out.append('<').append(tag).append('>');
                    i++; // skip the code char; loop will do its own i++
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }
}
