package com.autopickup.listener;

import com.autopickup.AutoPickupPlugin;
import com.autopickup.manager.ActionBarManager;
import com.autopickup.manager.FilterManager;
import com.autopickup.manager.PlayerStateManager;
import com.autopickup.model.FilterMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Characterization tests: records the current behaviour of BlockBreakListener. */
class BlockBreakListenerTest {

    @TempDir
    File dir;

    private final UUID uuid = UUID.randomUUID();
    private PlayerStateManager stateManager;
    private FilterManager filterManager;
    private ActionBarManager actionBar;
    private BlockBreakListener listener;

    private Player player;
    private PlayerInventory inventory;
    private World blockWorld;
    private Location dropLocation;
    private BlockDropItemEvent event;
    private List<Item> items;

    @BeforeEach
    void setUp() {
        stateManager = new PlayerStateManager(dir, false, Logger.getLogger("test"));
        stateManager.load();
        filterManager = new FilterManager(dir, Logger.getLogger("test"));
        filterManager.load();

        AutoPickupPlugin plugin = mock(AutoPickupPlugin.class);
        actionBar = mock(ActionBarManager.class);
        when(plugin.getActionBarManager()).thenReturn(actionBar);
        listener = new BlockBreakListener(plugin, stateManager, filterManager);

        player = mock(Player.class);
        inventory = mock(PlayerInventory.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>());
        // A different world on the player, to show overflow uses the block's world
        when(player.getWorld()).thenReturn(mock(World.class));

        blockWorld = mock(World.class);
        dropLocation = mock(Location.class);
        when(dropLocation.getWorld()).thenReturn(blockWorld);
        BlockState blockState = mock(BlockState.class);
        when(blockState.getLocation()).thenReturn(dropLocation);

        items = new ArrayList<>();
        event = mock(BlockDropItemEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.getItems()).thenReturn(items);
        when(event.getBlockState()).thenReturn(blockState);
    }

    private static ItemStack stack(Material type, int amount) {
        ItemStack s = mock(ItemStack.class);
        when(s.getType()).thenReturn(type);
        when(s.getAmount()).thenReturn(amount);
        return s;
    }

    private Item drop(ItemStack s) {
        Item item = mock(Item.class);
        when(item.getItemStack()).thenReturn(s);
        items.add(item);
        return item;
    }

    @Test
    void handlerIsHighestPriorityAndIgnoresCancelled() throws Exception {
        EventHandler h = BlockBreakListener.class
                .getMethod("onBlockDropItem", BlockDropItemEvent.class)
                .getAnnotation(EventHandler.class);
        assertEquals(EventPriority.HIGHEST, h.priority());
        assertTrue(h.ignoreCancelled());
    }

    @Test
    void disabledPlayerDropsAreUntouched() {
        drop(stack(Material.DIRT, 1));
        listener.onBlockDropItem(event);
        assertEquals(1, items.size());
        verifyNoInteractions(inventory, actionBar);
    }

    @Test
    void creativePlayerDropsAreUntouched() {
        stateManager.setEnabled(uuid, true);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
        drop(stack(Material.DIRT, 1));
        listener.onBlockDropItem(event);
        assertEquals(1, items.size());
        verifyNoInteractions(inventory, actionBar);
    }

    @Test
    void adventureModeIsNotExcluded() {
        stateManager.setEnabled(uuid, true);
        when(player.getGameMode()).thenReturn(GameMode.ADVENTURE);
        drop(stack(Material.DIRT, 1));
        listener.onBlockDropItem(event);
        assertTrue(items.isEmpty());
    }

    @Test
    void nullPlayerIsIgnored() {
        when(event.getPlayer()).thenReturn(null);
        drop(stack(Material.DIRT, 1));
        listener.onBlockDropItem(event);
        assertEquals(1, items.size());
    }

    @Test
    void enabledPlayerGetsDropsInInventoryAndActionBarRecord() {
        stateManager.setEnabled(uuid, true);
        ItemStack dirt = stack(Material.DIRT, 3);
        drop(dirt);

        listener.onBlockDropItem(event);

        assertTrue(items.isEmpty(), "picked-up items are removed from the event");
        verify(inventory).addItem(dirt);
        verify(actionBar).record(player, Material.DIRT, 3);
        verifyNoInteractions(blockWorld);
    }

    @Test
    void airStacksAreLeftInEvent() {
        stateManager.setEnabled(uuid, true);
        drop(stack(Material.AIR, 1));
        listener.onBlockDropItem(event);
        assertEquals(1, items.size());
        verifyNoInteractions(inventory);
    }

    @Test
    void itemsRejectedByFilterStayInEventWhileOthersArePickedUp() {
        stateManager.setEnabled(uuid, true);
        filterManager.setMode(uuid, FilterMode.WHITELIST);
        filterManager.toggleItem(uuid, Material.DIAMOND);
        ItemStack diamond = stack(Material.DIAMOND, 1);
        Item cobbleItem = drop(stack(Material.COBBLESTONE, 1));
        drop(diamond);

        listener.onBlockDropItem(event);

        assertEquals(List.of(cobbleItem), items);
        verify(inventory).addItem(diamond);
        verify(inventory, times(1)).addItem(any(ItemStack[].class));
    }

    @Test
    void partialOverflowIsDroppedInBlockWorldAndOnlyPickedAmountRecorded() {
        stateManager.setEnabled(uuid, true);
        ItemStack dirt = stack(Material.DIRT, 10);
        ItemStack leftover = stack(Material.DIRT, 4);
        drop(dirt);
        when(inventory.addItem(dirt)).thenReturn(new HashMap<>(Map.of(0, leftover)));

        listener.onBlockDropItem(event);

        assertTrue(items.isEmpty());
        verify(actionBar).record(player, Material.DIRT, 6);
        verify(blockWorld).dropItemNaturally(dropLocation, leftover);
        verify(player, never()).getWorld();
    }

    @Test
    void fullInventoryDropsEverythingAndRecordsNothing() {
        stateManager.setEnabled(uuid, true);
        ItemStack dirt = stack(Material.DIRT, 5);
        ItemStack leftover = stack(Material.DIRT, 5);
        drop(dirt);
        when(inventory.addItem(dirt)).thenReturn(new HashMap<>(Map.of(0, leftover)));

        listener.onBlockDropItem(event);

        assertTrue(items.isEmpty(), "item is still removed from the event and re-dropped");
        verifyNoInteractions(actionBar);
        verify(blockWorld).dropItemNaturally(dropLocation, leftover);
    }
}
