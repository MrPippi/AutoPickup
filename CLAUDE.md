# AutoPickup Plugin — AI Assistant Guide

## Project Overview

A Paper Minecraft plugin (Java 21, Maven) that auto-pickups block drops directly into the player's inventory when mining, with per-player toggle support and an optional item filter (whitelist/blacklist/none) configurable through an in-game GUI.

- **Plugin version:** 1.0.0
- **Paper API:** 1.21.1-R0.1-SNAPSHOT (API version `1.21`)
- **Java:** 21 (required — enforced by `maven-enforcer-plugin`)
- **Build output:** `target/AutoPickup-1.0.0.jar`

---

## Build & Test

```bash
# Build the jar
mvn clean package

# Run tests only (no server required)
mvn test

# Using the Maven wrapper (if mvn not on PATH)
./mvnw clean package
./mvnw test
```

**Requirements:** JDK 21 exactly (`[21,22)` range enforced). Set `JAVA_HOME` to JDK 21 before building.

> **Note:** `mvn test` requires internet access to `repo.papermc.io` to download `paper-api`. Surefire reports land in `target/surefire-reports/`.

---

## Source Structure

```
src/main/java/com/autopickup/
├── AutoPickupPlugin.java               Main plugin class (lifecycle, config, messaging)
├── command/
│   ├── AutoPickupCommand.java          /autopickup command executor (on/off/mode/reload)
│   └── AutoPickupTabCompleter.java     Tab-completer (suggests on/off/mode/reload)
├── gui/
│   └── FilterGuiListener.java          6-row chest GUI for per-player item filter management
├── listener/
│   └── BlockBreakListener.java         BlockDropItemEvent handler (core pickup + filter logic)
├── manager/
│   ├── PlayerStateManager.java         Per-player enabled state: in-memory + YAML persistence
│   └── FilterManager.java              Per-player filter mode and item list: in-memory + YAML persistence
├── model/
│   └── FilterMode.java                 Enum: NONE / WHITELIST / BLACKLIST
└── placeholder/
    └── AutoPickupPlaceholder.java      PlaceholderAPI expansion (%autopickup% / %autopickup_status%)

src/main/resources/
├── plugin.yml      Plugin metadata, commands, permissions
├── config.yml      settings.default-enabled + all user-facing command messages
└── gui.yml         All GUI text, button labels, and mode display names

src/test/java/com/autopickup/
├── AutoPickupPluginTest.java           Unit tests for legacyToMiniMessage() (pure string logic)
├── model/FilterModeTest.java           Unit tests for FilterMode enum (next(), getDisplayName())
├── manager/PlayerStateManagerTest.java Unit tests for PlayerStateManager (TempDir I/O)
├── manager/FilterManagerTest.java      Unit tests for FilterManager (shouldPickup, TempDir I/O)
└── command/AutoPickupCommandTest.java  Unit tests for AutoPickupCommand (Mockito mocks)
```

---

## Key Architecture

### AutoPickupPlugin (main class)
- Calls `saveDefaultConfig()`, then `loadGuiConfig()` (saves `gui.yml` if absent), then reads `settings.default-enabled`
- Instantiates `PlayerStateManager` and `FilterManager`; both are `.load()`ed on enable and `.save()`d on disable
- Creates `FilterGuiListener` and passes it to `AutoPickupCommand`
- Registers `BlockBreakListener` (takes both managers) and `FilterGuiListener` as event listeners
- PlaceholderAPI integration is **optional** and loaded via reflection so the plugin runs without PAPI on the classpath
- `getMessage(String path)` reads from `config.yml`; `getGuiMessage(String path)` reads from `gui.yml`; both parse `&` color codes and MiniMessage tags via `legacyToMiniMessage()` + `MiniMessage.deserialize()`
- `reload()` method reloads `config.yml`, `gui.yml`, player state, and filters — called by `/autopickup reload`

### BlockBreakListener
- Listens on `BlockDropItemEvent` at `EventPriority.HIGHEST` with `ignoreCancelled = true`
- Skips: null player, `CREATIVE` game mode, players with auto-pickup disabled
- Snapshots items and clears the event's item list (so nothing drops automatically)
- For each item: if `filterManager.shouldPickup()` returns false, drops it naturally at the block location; otherwise adds it to inventory
- Overflow items (full inventory) are dropped at the **block's world** (`dropLocation.getWorld()`, not `player.getWorld()`)

### PlayerStateManager
- State map: `ConcurrentHashMap<UUID, Boolean>` (thread-safe)
- Persists to `plugins/AutoPickup/players.yml` under the `players:` key
- Dirty flag: `save()` is a no-op when nothing changed; called explicitly after each toggle and in `onDisable()`
- `load()` is called once on startup and again on `/autopickup reload`

### FilterManager
- Mode map: `ConcurrentHashMap<UUID, FilterMode>` (thread-safe)
- Item list map: `ConcurrentHashMap<UUID, Set<Material>>` (thread-safe)
- Persists to `plugins/AutoPickup/filters.yml` under the `filters:` key
- Dirty flag: `save()` is a no-op when nothing changed; called after each GUI interaction and in `onDisable()`
- `shouldPickup(UUID, Material)` — returns `true` if the item should be auto-picked given the player's mode:
  - `NONE`: always returns `true`
  - `WHITELIST`: returns `true` only if material is in the list
  - `BLACKLIST`: returns `true` only if material is **not** in the list
- `toggleItem(UUID, Material)` — adds material if absent, removes if present
- `clearList(UUID)` — replaces the player's set with a fresh empty set

### FilterMode (enum)
- Three values: `NONE`, `WHITELIST`, `BLACKLIST`
- Each has a hardcoded `§`-code fallback display name and description
- `getDisplayName(FileConfiguration guiConfig)` / `getDescription(FileConfiguration guiConfig)` — reads from `gui.yml` `modes.<NAME>.display` / `.description`, falls back to hardcoded value
- `next()` — cycles: `NONE → WHITELIST → BLACKLIST → NONE`

### AutoPickupCommand
- `/autopickup` (alias `/ap`)
- Subcommands:
  - `reload` — requires `autopickup.reload` (default: op); calls `plugin.reload()` and sends `messages.reloaded`; works for console and players
  - `mode` — requires `autopickup.mode` (default: true); players-only; calls `filterGuiListener.openGui(player, 0, "")`
  - `on` / `off` — requires `autopickup.use` (default: true); players-only; sets state explicitly
  - _(no args)_ — requires `autopickup.use`; players-only; toggles current state
  - _(unrecognised arg)_ — sends `messages.invalid-usage`
- `stateManager.save()` is called after every successful on/off/toggle

### AutoPickupTabCompleter
- Suggests `["on", "off", "mode"]` to all senders; adds `"reload"` if sender has `autopickup.reload`
- Filters suggestions by the already-typed prefix
- Only completes the first argument; returns empty list for subsequent args

### FilterGuiListener
- 6-row chest inventory (54 slots):
  - **Slots 0–44:** material item grid, 45 items per page; each item represents a `Material`
  - **Slot 45:** `◀ Previous Page` (ARROW) or disabled (GRAY_DYE) on first page
  - **Slot 46:** `Filter Mode` (COMPARATOR) — click to cycle `NONE → WHITELIST → BLACKLIST → NONE`
  - **Slot 47:** `Search Items` (SPYGLASS) — closes GUI, prompts player to type in chat
  - **Slot 48:** `Clear Filter List` (BARRIER) — removes all materials from player's filter
  - **Slots 49–51:** filler (BLACK_STAINED_GLASS_PANE)
  - **Slot 52:** `Close` (RED_STAINED_GLASS_PANE)
  - **Slot 53:** `Next Page ▶` (ARROW) or disabled (GRAY_DYE) on last page
- Material list is lazily computed once (double-checked lock): all `isItem() && !isAir() && !LEGACY_` materials, sorted by name
- Search: clicking slot 47 stores a `pendingSearch` session, closes the inventory, and prompts the player; the next `AsyncChatEvent` from that player (caught at `LOWEST` priority) is cancelled and used as the search term (`"cancel"` restores the previous search)
- Session cleanup: `InventoryCloseEvent` with reason != `OPEN_NEW` removes the session; `PlayerQuitEvent` removes both `sessions` and `pendingSearch`
- `openGui(Player, int page, String searchTerm)` — public API to open or re-open the GUI at a specific page/search

### AutoPickupPlaceholder
- Identifier: `autopickup`; `persist()` returns `true` (survives PAPI reloads)
- `%autopickup%` and `%autopickup_status%` both return `"ON"` or `"OFF"`
- Handles `null`/empty/`"status"` params uniformly

---

## Configuration

### `config.yml`
```yaml
settings:
  default-enabled: false   # State for players who have never used /autopickup

messages:
  toggled-on:      "&aAuto-pickup has been &fenabled&a."
  toggled-off:     "&cAuto-pickup has been &fdisabled&c."
  no-permission:   "&cYou do not have permission to use this command."
  players-only:    "&cThis command can only be used by players."
  invalid-usage:   "&cUsage: /autopickup [on|off|mode|reload]"
  reloaded:        "&aConfiguration reloaded."
```

All message values support both `&` legacy color codes and MiniMessage tags. Accessed via `AutoPickupPlugin.getMessage(String path)`.

### `gui.yml`
Controls all GUI text. Supports `&` color codes. Runtime placeholders replaced in-code:

- `title.format` — inventory title; placeholders: `{mode}`, `{page}`, `{total_pages}`
- `title.search-suffix` — appended to title when a search is active; placeholder: `{search}`
- `buttons.*` — per-button `name`, `lore`, disabled variants; placeholders: `{page}`, `{total_pages}`, `{count}`, `{search_display}`
- `modes.NONE|WHITELIST|BLACKLIST.display` / `.description` — mode labels shown in the Mode button lore
- `item-slot.selected.*` / `item-slot.unselected.*` — name prefix and lore for grid items
- `messages.search-prompt`, `.filter-cleared`, `.mode-changed` — in-game feedback messages

Accessed via `AutoPickupPlugin.getGuiConfig()` (raw `FileConfiguration`) or `AutoPickupPlugin.getGuiMessage(String path)` (parses to Adventure `Component`).

### `plugin.yml`
```yaml
commands:
  autopickup:
    usage: /<command> [on|off|mode|reload]
    aliases: [ap]

permissions:
  autopickup.use:     default: true   # Toggle on/off
  autopickup.mode:    default: true   # Open filter GUI
  autopickup.reload:  default: op     # Reload configuration
```

---

## Data Files (runtime, auto-created)

### `plugins/AutoPickup/players.yml`
```yaml
players:
  aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee: true
  11111111-2222-3333-4444-555555555555: false
```
Keys are UUID strings. Invalid UUID keys are silently skipped on load.

### `plugins/AutoPickup/filters.yml`
```yaml
filters:
  aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee:
    mode: WHITELIST
    items:
      - DIAMOND_ORE
      - COAL_ORE
  11111111-2222-3333-4444-555555555555:
    mode: NONE
    items: []
```
Unknown material names and invalid UUID keys are silently skipped on load.

---

## Message Handling

`AutoPickupPlugin.legacyToMiniMessage(String text)` converts a string that may contain:
- Legacy `&` codes (`&a`, `&c`, `&l`, etc.) → MiniMessage tags (`<green>`, `<red>`, `<bold>`, etc.)
- Hex color codes `&#RRGGBB` → `<#RRGGBB>`
- Plain MiniMessage tags (pass through unchanged)

The result is then parsed by `MiniMessage.deserialize()`. **Never use `ChatColor` or `LegacyComponentSerializer` as the primary message API** — always go through `plugin.getMessage()` or `plugin.getGuiMessage()` for Adventure `Component` output. (`FilterGuiListener` uses `LegacyComponentSerializer.legacySection()` only as a convenience for building inventory/item display strings where `§` codes are used directly, but all chat messages to players go through the plugin's methods.)

---

## Testing Conventions

- **No game server needed** — tests use Mockito mocks for Paper API types and `@TempDir` for real YAML I/O
- `AutoPickupPluginTest`: exercises `legacyToMiniMessage()` — pure string conversion, no mocks needed
- `FilterModeTest`: exercises `next()` cycle and `getDisplayName()` fallback/config override
- `PlayerStateManagerTest`: uses `@TempDir File` for real YAML I/O without a server; covers default state, roundtrip save/load, invalid UUID handling
- `FilterManagerTest`: uses `@TempDir File`; covers `shouldPickup()` for all modes, toggle, clear, roundtrip, invalid key/mode/material handling
- `AutoPickupCommandTest`: mocks `AutoPickupPlugin`, `PlayerStateManager`, `FilterGuiListener`, `Player`, `CommandSender`; `lenient().when(plugin.getMessage(anyString()))` returns `Component.text(path)` for verifiable message key assertions

---

## Dependencies

| Dependency | Version | Scope |
|---|---|---|
| `io.papermc.paper:paper-api` | 1.21.1-R0.1-SNAPSHOT | compile |
| `me.clip:placeholderapi` | 2.11.6 | compile, optional |
| `org.junit.jupiter:junit-jupiter` | 5.10.2 | test |
| `org.mockito:mockito-core` | 5.8.0 | test |
| `org.mockito:mockito-junit-jupiter` | 5.8.0 | test |

Repositories: `https://repo.papermc.io/repository/maven-public/` and `https://repo.extendedclip.com/content/repositories/placeholderapi/`

---

## Conventions & Rules

1. **Java 21** — use modern features (records, pattern matching, `var`, streams). The enforcer will reject other JDK versions.
2. **Adventure API for all messages** — never use `ChatColor`; always go through `AutoPickupPlugin.getMessage()` / `getGuiMessage()` for user-facing text. `FilterGuiListener` may use `LegacyComponentSerializer.legacySection()` internally for inventory display construction only.
3. **No static plugin references** — pass `AutoPickupPlugin`, `PlayerStateManager`, `FilterManager`, or `FilterGuiListener` via constructor injection.
4. **PlaceholderAPI is optional** — any code that directly references PAPI classes must remain in `placeholder/` and be loaded only via reflection in `AutoPickupPlugin.onEnable`.
5. **Save on change, save on disable** — `stateManager.save()` and `filterManager.save()` must be called after every mutation (command toggle, GUI interaction) and in `onDisable()`.
6. **Thread safety** — both managers use `ConcurrentHashMap`; keep it that way if modifying state from async contexts. The `AsyncChatEvent` handler in `FilterGuiListener` schedules GUI re-open back on the main thread via `Bukkit.getScheduler().runTask()`.
7. **Event priority** — `BlockDropItemEvent` is handled at `HIGHEST` with `ignoreCancelled = true` so other plugins can cancel drops first.
8. **Overflow drops use block world** — always call `dropLocation.getWorld()`, not `player.getWorld()`, to avoid cross-world item drops.
9. **Filter-excluded items drop naturally** — items excluded by the player's whitelist/blacklist are dropped at the block location, not silently discarded.
10. **Lazy material list** — `FilterGuiListener.allItemMaterials()` uses double-checked locking to compute the material list once at runtime. Do not call `Material.values()` at class-load time in contexts that may run before the server registry is ready.
11. **GUI session lifecycle** — `sessions` is only removed on `InventoryCloseEvent` with reason != `OPEN_NEW`. `pendingSearch` is populated when Search is clicked and consumed (or discarded on quit) by `AsyncChatEvent`. Always clean both maps on `PlayerQuitEvent`.
12. **No new files without need** — prefer editing existing files; keep the package structure flat and minimal.
