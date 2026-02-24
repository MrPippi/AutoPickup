package com.autopickup.listener;

import com.autopickup.manager.PlayerStateManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Listens for block drop item events (Paper). When a player with auto-pickup enabled
 * breaks a block, drops are added to inventory instead of the ground; overflow drops naturally.
 * Does not affect creative mode or non-player breaks.
 */
public class BlockBreakListener implements Listener {

    private final PlayerStateManager stateManager;

    public BlockBreakListener(PlayerStateManager stateManager) {
        this.stateManager = stateManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDropItem(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        if (!stateManager.isEnabled(player.getUniqueId())) {
            return;
        }

        List<Item> items = event.getItems();
        if (items == null || items.isEmpty()) {
            return;
        }

        Location dropLocation = event.getBlockState().getLocation();
        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : items) {
            ItemStack stack = item.getItemStack();
            if (stack != null && !stack.getType().isAir()) {
                stacks.add(stack);
            }
        }
        items.clear();

        for (ItemStack stack : stacks) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            for (ItemStack overflow : leftover.values()) {
                if (overflow != null && !overflow.getType().isAir()) {
                    dropLocation.getWorld().dropItemNaturally(dropLocation, overflow);
                }
            }
        }
    }
}
