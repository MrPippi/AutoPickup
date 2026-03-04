package com.autopickup.listener;

import com.autopickup.manager.FilterManager;
import com.autopickup.manager.PlayerStateManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExpEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Integrates with MiraculixxT's VeinMiner plugin (https://github.com/MiraculixxT/Veinminer).
 *
 * VeinMiner fires a custom VeinminerDropEvent (extends BlockExpEvent) after each vein block is
 * broken, carrying a mutable list of item drops.  Because we cannot depend on VeinMiner at
 * compile time this listener is registered only when the VeinMiner plugin is detected on the
 * server, and it identifies the event class by name at runtime via instanceof check with a
 * cached Class reference.
 *
 * For every item in the VeinminerDropEvent.items list that passes AutoPickup's filter,
 * the item is removed from the list and placed directly into the player's inventory.
 * Items that do not pass the filter remain in the list so VeinMiner drops them normally.
 */
public class VeinMinerListener implements Listener {

    /**
     * Fully-qualified class name of the drop event fired by MiraculixxT's VeinMiner.
     * VeinminerDropEvent extends BlockExpEvent, so we cast via that base type.
     */
    private static final String DROP_EVENT_CLASS =
            "de.miraculixx.veinminer.VeinMinerEvent$VeinminerDropEvent";

    /**
     * Cached Class for VeinminerDropEvent.  Null if the class is not available.
     * Resolved once on construction so all subsequent event calls avoid Class.forName overhead.
     */
    private final Class<?> dropEventClass;

    /** Accessor to the mutable items list via reflection: VeinminerDropEvent.items (Kotlin var). */
    private final java.lang.reflect.Method itemsGetter;
    private final java.lang.reflect.Method playerGetter;

    private final PlayerStateManager stateManager;
    private final FilterManager      filterManager;

    public VeinMinerListener(PlayerStateManager stateManager, FilterManager filterManager) {
        this.stateManager  = stateManager;
        this.filterManager = filterManager;

        Class<?> cls     = null;
        java.lang.reflect.Method items  = null;
        java.lang.reflect.Method player = null;

        try {
            cls    = Class.forName(DROP_EVENT_CLASS);
            items  = cls.getMethod("getItems");
            player = cls.getMethod("getPlayer");
        } catch (ReflectiveOperationException ignored) {
            // VeinMiner not present or API changed – this listener will be a no-op
        }

        this.dropEventClass = cls;
        this.itemsGetter    = items;
        this.playerGetter   = player;
    }

    /**
     * Listens on BlockExpEvent at NORMAL priority so we run after VeinMiner (HIGH) but before
     * most other plugins.  We identify VeinminerDropEvent by its runtime class.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onVeinminerDrop(BlockExpEvent event) {
        if (dropEventClass == null || !dropEventClass.isInstance(event)) return;

        Player player;
        List<ItemStack> items;
        try {
            player = (Player) playerGetter.invoke(event);
            //noinspection unchecked
            items = (List<ItemStack>) itemsGetter.invoke(event);
        } catch (ReflectiveOperationException e) {
            return;
        }

        if (player == null || player.getGameMode() == GameMode.CREATIVE) return;
        if (!stateManager.isEnabled(player.getUniqueId())) return;
        if (items == null || items.isEmpty()) return;

        Location dropLocation = event.getBlock().getLocation().toCenterLocation();

        Iterator<ItemStack> it = items.iterator();
        while (it.hasNext()) {
            ItemStack stack = it.next();
            if (stack == null || stack.getType().isAir()) continue;
            if (!filterManager.shouldPickup(player.getUniqueId(), stack.getType())) continue;

            it.remove();

            Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            for (ItemStack overflow : leftover.values()) {
                if (overflow != null && !overflow.getType().isAir()) {
                    dropLocation.getWorld().dropItemNaturally(dropLocation, overflow);
                }
            }
        }
    }
}
