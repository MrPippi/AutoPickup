package com.autopickup.listener;

import com.autopickup.AutoPickupPlugin;
import com.autopickup.manager.FilterManager;
import com.autopickup.manager.PlayerStateManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Listens for block drop item events (Paper). When a player with auto-pickup enabled
 * breaks a block, drops are added to inventory instead of the ground; overflow drops naturally.
 * Drops that are excluded by the player's filter (whitelist / blacklist) fall naturally.
 * Does not affect creative mode or non-player breaks.
 */
public class BlockBreakListener implements Listener {

    private final AutoPickupPlugin plugin;
    private final PlayerStateManager stateManager;
    private final FilterManager filterManager;

    public BlockBreakListener(AutoPickupPlugin plugin, PlayerStateManager stateManager, FilterManager filterManager) {
        this.plugin        = plugin;
        this.stateManager  = stateManager;
        this.filterManager = filterManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDropItem(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (!stateManager.isEnabled(player.getUniqueId())) return;

        List<Item> items = event.getItems();
        if (items == null || items.isEmpty()) return;

        Location dropLocation = event.getBlockState().getLocation();

        // Iterate over items that should be picked up; remove them from the event so the
        // game never spawns them as entities. Items that should NOT be picked up are left in
        // the event list and fall naturally with their original physics.
        java.util.Iterator<Item> iterator = items.iterator();
        while (iterator.hasNext()) {
            Item entity = iterator.next();
            ItemStack stack = entity.getItemStack();
            if (stack == null || stack.getType().isAir()) continue;
            if (!filterManager.shouldPickup(player.getUniqueId(), stack.getType())) continue;

            // Remove from event so this item will NOT be spawned on the ground
            iterator.remove();

            Material material = stack.getType();
            int originalAmount = stack.getAmount();

            // Add to inventory; overflow spawns naturally at the drop location
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            int leftoverAmount = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
            int pickedUp = originalAmount - leftoverAmount;

            if (pickedUp > 0) {
                plugin.getActionBarManager().record(player, material, pickedUp);
            }

            for (ItemStack overflow : leftover.values()) {
                if (overflow != null && !overflow.getType().isAir()) {
                    dropLocation.getWorld().dropItemNaturally(dropLocation, overflow);
                }
            }
        }
    }
}
