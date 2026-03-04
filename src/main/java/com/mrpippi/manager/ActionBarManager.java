package com.autopickup.manager;

import com.autopickup.AutoPickupPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Batches per-player pickup events within a single tick and displays a single
 * ActionBar message listing all items collected.  The message stays visible for
 * a configurable number of ticks after the last pickup.
 *
 * <p>Thread-safety: {@link #record(Player, Material, int)} may be called from
 * any thread, but the display task is always scheduled on the main thread.
 */
public class ActionBarManager {

    private final AutoPickupPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * Per-player accumulator: maps material → total count for the current display window.
     * Replaced by a fresh map each time the display task fires (or when first entry arrives
     * after the previous window closed).
     */
    private final ConcurrentHashMap<UUID, Map<Material, Integer>> pendingPickups = new ConcurrentHashMap<>();

    /** Tracks whether a display task is already scheduled for each player. */
    private final ConcurrentHashMap<UUID, BukkitRunnable> displayTasks = new ConcurrentHashMap<>();

    public ActionBarManager(AutoPickupPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Records that {@code player} picked up {@code amount} of {@code material}.
     * Accumulates within the current display window and refreshes the ActionBar.
     *
     * @param player   the player who picked up the item
     * @param material the material type
     * @param amount   quantity picked up (may be > 1 for stacks)
     */
    public void record(Player player, Material material, int amount) {
        if (!isEnabled()) return;

        UUID id = player.getUniqueId();
        pendingPickups.compute(id, (uuid, map) -> {
            if (map == null) map = new LinkedHashMap<>();
            map.merge(material, amount, Integer::sum);
            return map;
        });

        scheduleDisplay(player);
    }

    /** Returns true if the actionbar feature is enabled in config.yml. */
    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("settings.actionbar.enabled", true);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private void scheduleDisplay(Player player) {
        UUID id = player.getUniqueId();
        int displayTicks = plugin.getConfig().getInt("settings.actionbar.display-ticks", 40);

        BukkitRunnable existing = displayTasks.get(id);
        if (existing != null) {
            existing.cancel();
        }

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                displayTasks.remove(id);
                Map<Material, Integer> pickups = pendingPickups.remove(id);
                if (pickups == null || pickups.isEmpty()) return;
                if (!player.isOnline()) return;

                Component bar = buildActionBar(pickups);
                player.sendActionBar(bar);
            }
        };

        displayTasks.put(id, task);
        task.runTaskLater(plugin, displayTicks);

        // Send immediately to keep the bar visible during accumulation
        sendNow(player);
    }

    private void sendNow(Player player) {
        UUID id = player.getUniqueId();
        Map<Material, Integer> pickups = pendingPickups.get(id);
        if (pickups == null || pickups.isEmpty()) return;

        Component bar = buildActionBar(pickups);

        // Must run on the main thread
        if (plugin.getServer().isPrimaryThread()) {
            player.sendActionBar(bar);
        } else {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) player.sendActionBar(bar);
            });
        }
    }

    /**
     * Builds the full ActionBar component from the accumulated pickup map.
     *
     * <p>Template: {@code settings.actionbar-format} in lang.yml with placeholder
     * {@code {entries}} being a comma-joined list of per-entry strings and
     * {@code {total}} being the sum of all counts.
     */
    private Component buildActionBar(Map<Material, Integer> pickups) {
        String entryTemplate = getLangString("messages.actionbar-entry", "<translate:{item}> &7x{count}");
        String barTemplate   = getLangString("messages.actionbar", "&a+ &f{entries}");

        List<String> entryStrings = new ArrayList<>();
        int total = 0;
        for (Map.Entry<Material, Integer> e : pickups.entrySet()) {
            Material mat = e.getKey();
            int count = e.getValue();
            total += count;

            String key = (mat.isBlock() ? "block" : "item") + ".minecraft." + mat.getKey().getKey();
            String entry = entryTemplate
                    .replace("{item}", key)
                    .replace("{count}", String.valueOf(count));
            entryStrings.add(entry);
        }

        String joined = String.join("<gray>, </gray>", entryStrings);
        String barRaw = barTemplate
                .replace("{entries}", joined)
                .replace("{total}", String.valueOf(total));

        return miniMessage.deserialize(AutoPickupPlugin.legacyToMiniMessage(barRaw));
    }

    private String getLangString(String path, String fallback) {
        try {
            var langConfig = plugin.getLangConfig();
            if (langConfig != null) {
                String val = langConfig.getString(path);
                if (val != null) return val;
            }
        } catch (Exception ignored) {}
        return fallback;
    }
}
