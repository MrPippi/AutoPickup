package com.autopickup.gui;

import com.autopickup.AutoPickupPlugin;
import com.autopickup.manager.FilterManager;
import com.autopickup.model.FilterMode;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.configuration.file.FileConfiguration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages the Filter GUI: builds/opens the inventory, handles all click interactions,
 * and captures chat input for the search feature.
 *
 * Layout (6-row chest, 54 slots):
 *   Rows 0-4  (slots  0-44): item material grid – 45 items per page
 *   Row 5     (slots 45-53): navigation/control bar
 *     45: ◀ Prev   46: Mode   47: Search   48: Clear   49-51: filler   52: Close   53: Next ▶
 */
public class FilterGuiListener implements Listener {

    // --- Constants ---
    private static final int GUI_SIZE       = 54;
    private static final int ITEMS_PER_PAGE = 45;
    private static final int SLOT_PREV      = 45;
    private static final int SLOT_MODE      = 46;
    private static final int SLOT_SEARCH    = 47;
    private static final int SLOT_CLEAR     = 48;
    private static final int SLOT_CLOSE     = 52;
    private static final int SLOT_NEXT      = 53;

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    /**
     * Admin-only / unobtainable items excluded from the filter GUI.
     * These items cannot be obtained in normal survival gameplay and have no
     * relevance to block-drop auto-pickup.
     */
    private static final Set<Material> ADMIN_ONLY_ITEMS = Set.of(
            Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK,
            Material.COMMAND_BLOCK_MINECART,
            Material.STRUCTURE_BLOCK,
            Material.STRUCTURE_VOID,
            Material.JIGSAW,
            Material.BARRIER,
            Material.LIGHT,
            Material.DEBUG_STICK,
            Material.KNOWLEDGE_BOOK
    );

    /**
     * All obtainable item-type materials (no legacy / air / admin-only), sorted by name.
     * Computed lazily on first use so that class loading does not trigger the
     * Paper registry (which requires a live server) — this keeps unit tests happy.
     */
    private static volatile List<Material> cachedMaterials;

    private static List<Material> allItemMaterials() {
        if (cachedMaterials == null) {
            synchronized (FilterGuiListener.class) {
                if (cachedMaterials == null) {
                    cachedMaterials = Arrays.stream(Material.values())
                            .filter(m -> m.isItem() && !m.isAir()
                                    && !m.name().startsWith("LEGACY_")
                                    && !ADMIN_ONLY_ITEMS.contains(m))
                            .sorted(Comparator.comparing(Enum::name))
                            .toList();
                }
            }
        }
        return cachedMaterials;
    }

    // --- State ---

    /** Session data for a currently-open GUI. */
    public record GuiSession(int page, String searchTerm, List<Material> materials) {}

    /** UUID → session for players who currently have the GUI open. */
    private final Map<UUID, GuiSession> sessions = new ConcurrentHashMap<>();

    /** UUID → session saved when a player closed the GUI to type a search term. */
    private final Map<UUID, GuiSession> pendingSearch = new ConcurrentHashMap<>();

    private final AutoPickupPlugin plugin;
    private final FilterManager filterManager;

    public FilterGuiListener(AutoPickupPlugin plugin, FilterManager filterManager) {
        this.plugin = plugin;
        this.filterManager = filterManager;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Opens (or re-opens) the filter GUI for a player.
     *
     * @param player     the player
     * @param page       0-based page index (clamped automatically)
     * @param searchTerm item name filter – blank means show all
     */
    public void openGui(Player player, int page, String searchTerm) {
        UUID uuid = player.getUniqueId();

        List<Material> materials;
        if (searchTerm == null || searchTerm.isBlank()) {
            materials = allItemMaterials();
        } else {
            String lower = searchTerm.toLowerCase().replace(" ", "_");
            materials = allItemMaterials().stream()
                    .filter(m -> m.name().toLowerCase().contains(lower))
                    .toList();
        }

        int totalPages = Math.max(1, (int) Math.ceil(materials.size() / (double) ITEMS_PER_PAGE));
        page = Math.clamp(page, 0, totalPages - 1);

        GuiSession session = new GuiSession(page, searchTerm == null ? "" : searchTerm, materials);

        // Put the session BEFORE calling openInventory.  The old inventory's InventoryCloseEvent
        // fires synchronously inside openInventory with reason OPEN_NEW; we skip removal there.
        sessions.put(uuid, session);
        player.openInventory(buildInventory(uuid, session));
    }

    // -------------------------------------------------------------------------
    // Inventory builder
    // -------------------------------------------------------------------------

    private Inventory buildInventory(UUID uuid, GuiSession session) {
        FilterMode mode       = filterManager.getMode(uuid);
        Set<Material> list    = filterManager.getFilterList(uuid);
        int page              = session.page();
        List<Material> mats   = session.materials();
        int totalPages        = Math.max(1, (int) Math.ceil(mats.size() / (double) ITEMS_PER_PAGE));

        // --- Title ---
        String modeDisplay = mode.getDisplayName(plugin.getGuiConfig());
        String titleFormat = guiStr("title.format", "§8Filter §7| {mode} §8| §7{page}/{total_pages}");
        String titleStr = titleFormat
                .replace("{mode}", modeDisplay.replace('&', '§'))
                .replace("{page}", String.valueOf(page + 1))
                .replace("{total_pages}", String.valueOf(totalPages));
        if (!session.searchTerm().isBlank()) {
            String suffix = guiStr("title.search-suffix", "  §8[§7{search}§8]").replace("{search}", session.searchTerm());
            titleStr += suffix;
        }
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, LEGACY.deserialize(titleStr));

        // --- Item grid (slots 0-44) ---
        int startIdx = page * ITEMS_PER_PAGE;
        for (int slot = 0; slot < ITEMS_PER_PAGE; slot++) {
            int matIdx = startIdx + slot;
            if (matIdx >= mats.size()) break;
            inv.setItem(slot, buildMaterialItem(mats.get(matIdx), list.contains(mats.get(matIdx))));
        }

        // --- Control bar ---

        // 45: Previous page
        String prevLore = guiStr("buttons.prev.lore", "§8Page §7{page} §8/ §7{total_pages}")
                .replace("{page}", String.valueOf(page)).replace("{total_pages}", String.valueOf(totalPages));
        if (page > 0) {
            inv.setItem(SLOT_PREV, buildControl(Material.ARROW,
                    guiStr("buttons.prev.name", "§7◀ Previous Page"),
                    List.of(prevLore)));
        } else {
            inv.setItem(SLOT_PREV, buildControl(Material.GRAY_DYE,
                    guiStr("buttons.prev.name-disabled", "§8◀ Previous Page"),
                    List.of(guiStr("buttons.prev.lore-disabled", "§8Already on first page"))));
        }

        // 46: Mode toggle
        List<String> modeLore = new ArrayList<>();
        String loreCurrentPrefix = guiStr("buttons.mode.lore-current-prefix", "§f§l» ");
        String loreOtherPrefix = guiStr("buttons.mode.lore-other-prefix", "§8   ");
        for (FilterMode fm : FilterMode.values()) {
            String prefix = (fm == mode) ? loreCurrentPrefix : loreOtherPrefix;
            modeLore.add(prefix + fm.getDisplayName(plugin.getGuiConfig()).replace('&', '§'));
            modeLore.add("  " + fm.getDescription(plugin.getGuiConfig()).replace('&', '§'));
            modeLore.add("");
        }
        modeLore.add(guiStr("buttons.mode.lore-cycle", "§7Click to cycle mode"));
        inv.setItem(SLOT_MODE, buildControl(Material.COMPARATOR,
                guiStr("buttons.mode.name", "§eFilter Mode"), modeLore));

        // 47: Search
        String searchDisplay = session.searchTerm().isBlank()
                ? guiStr("buttons.search.placeholder-none", "§8(none)")
                : ("§f" + session.searchTerm());
        List<String> searchLore = guiStrList("buttons.search.lore", List.of(
                "§7Current: {search_display}", "", "§7Click, then type in chat", "§8Type §ccancel §8to keep current search"));
        searchLore = searchLore.stream().map(s -> s.replace("{search_display}", searchDisplay)).toList();
        inv.setItem(SLOT_SEARCH, buildControl(Material.SPYGLASS,
                guiStr("buttons.search.name", "§e🔍 Search Items"),
                searchLore));

        // 48: Clear
        List<String> clearLore = guiStrList("buttons.clear.lore", List.of("§7Items in list: §f{count}", "", "§7Click to remove all items"));
        clearLore = clearLore.stream().map(s -> s.replace("{count}", String.valueOf(list.size()))).toList();
        inv.setItem(SLOT_CLEAR, buildControl(Material.BARRIER,
                guiStr("buttons.clear.name", "§c✖ Clear Filter List"),
                clearLore));

        // 49-51: Filler
        ItemStack filler = buildFiller();
        inv.setItem(49, filler);
        inv.setItem(50, filler);
        inv.setItem(51, filler);

        // 52: Close
        inv.setItem(SLOT_CLOSE, buildControl(Material.RED_STAINED_GLASS_PANE,
                guiStr("buttons.close.name", "§c✖ Close"), List.of()));

        // 53: Next page
        String nextLore = guiStr("buttons.next.lore", "§8Page §7{page} §8/ §7{total_pages}")
                .replace("{page}", String.valueOf(page + 2)).replace("{total_pages}", String.valueOf(totalPages));
        if (page < totalPages - 1) {
            inv.setItem(SLOT_NEXT, buildControl(Material.ARROW,
                    guiStr("buttons.next.name", "§7Next Page ▶"),
                    List.of(nextLore)));
        } else {
            inv.setItem(SLOT_NEXT, buildControl(Material.GRAY_DYE,
                    guiStr("buttons.next.name-disabled", "§8Next Page ▶"),
                    List.of(guiStr("buttons.next.lore-disabled", "§8Already on last page"))));
        }

        return inv;
    }

    // -------------------------------------------------------------------------
    // Item builders
    // -------------------------------------------------------------------------

    private ItemStack buildMaterialItem(Material mat, boolean selected) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String prettyName = prettyName(mat);
        if (selected) {
            String prefix = guiStr("item-slot.selected.name-prefix", "§a✔ ");
            meta.displayName(LEGACY.deserialize(prefix + prettyName));
            List<String> lore = guiStrList("item-slot.selected.lore", List.of("§aSelected", "§7Click to remove from filter"));
            meta.lore(lore.stream().map(LEGACY::deserialize).collect(Collectors.toList()));
        } else {
            String prefix = guiStr("item-slot.unselected.name-prefix", "§7");
            meta.displayName(LEGACY.deserialize(prefix + prettyName));
            List<String> lore = guiStrList("item-slot.unselected.lore", List.of("§8Not selected", "§7Click to add to filter"));
            meta.lore(lore.stream().map(LEGACY::deserialize).collect(Collectors.toList()));
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildControl(Material mat, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(LEGACY.deserialize(name));
        if (!loreLines.isEmpty()) {
            meta.lore(loreLines.stream().map(LEGACY::deserialize).collect(Collectors.toList()));
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (!sessions.containsKey(uuid)) return;

        // Cancel all interactions while our GUI is open
        event.setCancelled(true);

        // Only process clicks in the top (our) inventory
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= GUI_SIZE) return;

        GuiSession session = sessions.get(uuid);
        if (session == null) return;

        int totalPages = Math.max(1,
                (int) Math.ceil(session.materials().size() / (double) ITEMS_PER_PAGE));

        if (slot < ITEMS_PER_PAGE) {
            // --- Item grid click ---
            int matIdx = session.page() * ITEMS_PER_PAGE + slot;
            if (matIdx < session.materials().size()) {
                filterManager.toggleItem(uuid, session.materials().get(matIdx));
                filterManager.save();
                openGui(player, session.page(), session.searchTerm());
            }

        } else if (slot == SLOT_PREV) {
            if (session.page() > 0) {
                openGui(player, session.page() - 1, session.searchTerm());
            }

        } else if (slot == SLOT_MODE) {
            FilterMode newMode = filterManager.getMode(uuid).next();
            filterManager.setMode(uuid, newMode);
            filterManager.save();
            String modeMsg = guiStr("messages.mode-changed", "§8[AutoPickup] §7Filter mode: {mode}")
                    .replace("{mode}", newMode.getDisplayName(plugin.getGuiConfig()).replace('&', '§'));
            player.sendMessage(LEGACY.deserialize(modeMsg));

            openGui(player, session.page(), session.searchTerm());

        } else if (slot == SLOT_SEARCH) {
            // Save session for after the player types, then close and prompt
            pendingSearch.put(uuid, session);
            player.closeInventory();
            player.sendMessage(LEGACY.deserialize(
                    guiStr("messages.search-prompt", "§8[AutoPickup] §eType item name to search §8(or §ccancel §8to go back)§e:")));

        } else if (slot == SLOT_CLEAR) {
            filterManager.clearList(uuid);
            filterManager.save();
            player.sendMessage(LEGACY.deserialize(
                    guiStr("messages.filter-cleared", "§8[AutoPickup] §7Filter list cleared.")));
            openGui(player, session.page(), session.searchTerm());

        } else if (slot == SLOT_CLOSE) {
            player.closeInventory();

        } else if (slot == SLOT_NEXT) {
            if (session.page() < totalPages - 1) {
                openGui(player, session.page() + 1, session.searchTerm());
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!sessions.containsKey(player.getUniqueId())) return;
        // Cancel any drag that touches our GUI slots
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < GUI_SIZE) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (!sessions.containsKey(uuid)) return;

        // OPEN_NEW: another inventory opened (e.g., page change) — keep session alive
        if (event.getReason() == InventoryCloseEvent.Reason.OPEN_NEW) return;

        // All other reasons (player close, plugin close for search, disconnect, etc.)
        sessions.remove(uuid);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        sessions.remove(uuid);
        pendingSearch.remove(uuid);
    }

    /** Captures the player's next chat message and uses it as a search term. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        GuiSession prevSession = pendingSearch.remove(uuid);
        if (prevSession == null) return;

        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText()
                .serialize(event.message()).trim();
        String finalSearch = text.equalsIgnoreCase("cancel") ? prevSession.searchTerm() : text;

        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> openGui(player, 0, finalSearch));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String modeColor(FilterMode mode) {
        return switch (mode) {
            case WHITELIST -> "§a";
            case BLACKLIST -> "§c";
            default        -> "§7";
        };
    }

    private String prettyName(Material m) {
        String[] parts = m.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(part.charAt(0)));
            sb.append(part.substring(1));
        }
        return sb.toString();
    }

    /** Gets a GUI config string and converts & to § for legacy serialization. */
    private String guiStr(String path, String fallback) {
        FileConfiguration cfg = plugin.getGuiConfig();
        if (cfg == null) return fallback.replace('&', '§');
        return cfg.getString(path, fallback).replace('&', '§');
    }

    private List<String> guiStrList(String path, List<String> fallback) {
        FileConfiguration cfg = plugin.getGuiConfig();
        if (cfg == null) {
            return fallback != null ? fallback.stream().map(s -> s.replace('&', '§')).toList() : List.of();
        }
        List<String> list = cfg.getStringList(path);
        if (list == null || list.isEmpty()) return fallback != null ? fallback.stream().map(s -> s.replace('&', '§')).toList() : List.of();
        return list.stream().map(s -> s.replace('&', '§')).toList();
    }
}
