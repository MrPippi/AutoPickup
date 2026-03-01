# AutoPickup Plugin — AI Assistant Guide

## Project Overview

A Paper Minecraft plugin (Java 21, Maven) that auto-pickups block drops directly into the player's inventory when mining. Supports per-player toggle, whitelist/blacklist item filtering with a GUI, and PlaceholderAPI integration.

- **Plugin version:** 1.0.0
- **Paper API:** 1.21.1-R0.1-SNAPSHOT (API version `1.21`)
- **Java:** 21 (required — enforced by `maven-enforcer-plugin`)
- **Build output:** `target/AutoPickup-1.0.0.jar`

---

## Build

```bash
mvn clean package
```

**Requirements:** JDK 21 exactly (`[21,22)` range enforced). Set `JAVA_HOME` to JDK 21 before building.

Testing is done manually in-game; there is no `src/test` directory.

---

## Source Structure

```
src/main/java/com/mrpippi/
├── AutoPickupPlugin.java               Main plugin class (lifecycle, config, messaging)
├── model/
│   └── FilterMode.java                 Enum: NONE / WHITELIST / BLACKLIST
├── command/
│   ├── AutoPickupCommand.java          /autopickup [on|off|mode|reload] executor
│   └── AutoPickupTabCompleter.java     Tab-completer (suggests on/off/mode/reload)
├── gui/
│   └── FilterGuiListener.java          6-row chest GUI + chat search handler
├── listener/
│   └── BlockBreakListener.java         BlockDropItemEvent handler (core pickup logic)
├── manager/
│   ├── PlayerStateManager.java         Per-player on/off state: in-memory + YAML persistence
│   └── FilterManager.java              Per-player filter mode + item set persistence
└── placeholder/
    └── AutoPickupPlaceholder.java      PlaceholderAPI expansion (%autopickup% / %autopickup_status%)

src/main/resources/
├── plugin.yml      Plugin metadata, commands, permissions
├── config.yml      settings.default-enabled + command messages
├── gui.yml         All GUI button/title text (supports runtime placeholders)
└── lang.yml        Language messages (same structure as config messages)
```

Note: directory is `com/mrpippi/`; Java package declarations use `com.autopickup`.

---

## Key Architecture

### AutoPickupPlugin (main class)
- Calls `saveDefaultConfig()`, `loadGuiConfig()`, then reads `settings.default-enabled`
- Instantiates `PlayerStateManager` and `FilterManager`, registers listeners and command
- PlaceholderAPI integration is **optional** and loaded via reflection so the plugin runs without PAPI on the classpath
- `getMessage(String path)` reads from `config.yml`, converts `&` codes and MiniMessage tags via `legacyToMiniMessage()` + `MiniMessage.deserialize()`
- `getGuiMessage(String path)` does the same but reads from `gui.yml`
- `reload()` reloads all config/data files; called by `/autopickup reload`

### BlockBreakListener
- Listens on `BlockDropItemEvent` at `EventPriority.HIGHEST` with `ignoreCancelled = true`
- Skips: null player, `CREATIVE` game mode, players with auto-pickup disabled
- Per-item: calls `FilterManager.shouldPickup(uuid, material)` to respect whitelist/blacklist
- Clears allowed items from the event list, adds them to inventory
- Overflow items (full inventory) are dropped at the **block's world** (`dropLocation.getWorld()`, not `player.getWorld()`)

### PlayerStateManager
- State map: `ConcurrentHashMap<UUID, Boolean>` (thread-safe)
- Persists to `plugins/AutoPickup/players.yml` under the `players:` key
- Dirty flag: `save()` is a no-op when nothing changed; called explicitly after each toggle
- `load()` is called on startup and on `/reload`; `save()` is called on disable and after each toggle

### FilterManager
- Two maps: `ConcurrentHashMap<UUID, FilterMode>` and `ConcurrentHashMap<UUID, Set<Material>>`
- Persists to `plugins/AutoPickup/filters.yml` under the `filters:` key
- `shouldPickup(UUID, Material)` encapsulates NONE/WHITELIST/BLACKLIST logic
- `toggleItem(UUID, Material)` adds or removes a material from the player's list
- Dirty flag and save/load lifecycle mirrors `PlayerStateManager`

### AutoPickupCommand
- `/autopickup` (alias `/ap`)
- `on` / `off` / no args → toggle/set state; requires `autopickup.use` (default: `true`)
- `mode` → opens filter GUI; requires `autopickup.mode` (default: `true`); players only
- `reload` → calls `plugin.reload()`; requires `autopickup.reload` (default: `op`)
- Any other arg → sends `messages.invalid-usage`
- Console blocked from player-only subcommands with `messages.players-only`

### AutoPickupTabCompleter
- Suggests `["on", "off", "mode", "reload"]` for the first argument
- Filters by prefix typed so far
- Only suggests to `Player` senders with `autopickup.use` permission

### FilterGuiListener
- Opens a 6-row chest GUI via `filterGuiListener.openGui(player, page, searchTerm)`
- Slots 0–44: paginated `Material` grid (45 items/page); click toggles in/out of filter list
- Control row (45–53): ◀ Prev | Mode cycle | Search | Clear | fillers | Close | Next ▶
- Mode cycles NONE → WHITELIST → BLACKLIST → NONE
- Search: clicking Search enters chat-input mode (`AsyncChatEvent`); type to filter, `cancel` to abort
- Uses `InventoryCloseEvent.Reason.OPEN_NEW` to keep the GUI session alive during page/mode changes
- `cachedMaterials` is lazy (populated on first open, not in a static initializer)

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
  toggled-on:    "&aAuto-pickup has been &fenabled&a."
  toggled-off:   "&cAuto-pickup has been &fdisabled&c."
  no-permission: "&cYou do not have permission to use this command."
  players-only:  "&cThis command can only be used by players."
  invalid-usage: "&cUsage: /autopickup [on|off|mode|reload]"
  reloaded:      "&aConfiguration reloaded."
```

All message values support `&` color codes and MiniMessage tags. Accessed via `AutoPickupPlugin.getMessage(String path)`.

### `gui.yml`
Controls all GUI text: title format, button names/lore, mode display names, item slot prefixes.
Supports the same color code syntax plus runtime placeholders: `{mode}`, `{page}`, `{total_pages}`, `{search}`, `{search_display}`, `{count}`.
Accessed via `AutoPickupPlugin.getGuiMessage(String path)` or `getGuiConfig()`.

### `lang.yml`
Mirrors the `messages:` block from `config.yml`. Same keys and syntax; reserved for future language switching.

### `plugin.yml`
- Command: `autopickup`, alias: `ap`
- Permissions: `autopickup.use` (default `true`), `autopickup.mode` (default `true`), `autopickup.reload` (default `op`)

---

## Data Files

`plugins/AutoPickup/players.yml`:
```yaml
players:
  aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee: true
  11111111-2222-3333-4444-555555555555: false
```

`plugins/AutoPickup/filters.yml`:
```yaml
filters:
  aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee:
    mode: WHITELIST
    items:
      - DIAMOND
      - GOLD_INGOT
```

Invalid UUID keys are silently skipped on load.

---

## Dependencies

| Dependency | Version | Scope |
|---|---|---|
| `io.papermc.paper:paper-api` | 1.21.1-R0.1-SNAPSHOT | compile |
| `me.clip:placeholderapi` | 2.11.6 | compile, optional |

Repositories: `https://repo.papermc.io/repository/maven-public/` and `https://repo.extendedclip.com/content/repositories/placeholderapi/`

---

## Conventions & Rules

1. **Java 21** — use modern features (records, pattern matching, `var`, streams). The enforcer will reject other JDK versions.
2. **Adventure API for all messages** — never use `ChatColor`; always go through `AutoPickupPlugin.getMessage()` / `getGuiMessage()` which use MiniMessage internally.
3. **No static plugin references** — pass `AutoPickupPlugin`, `PlayerStateManager`, `FilterManager` via constructor injection.
4. **PlaceholderAPI is optional** — any code that directly references PAPI classes must remain in `placeholder/` and be loaded only via reflection in `AutoPickupPlugin.onEnable`.
5. **Save on toggle, save on disable** — `stateManager.save()` and `filterManager.save()` must be called after mutations in command code, and in `onDisable()`.
6. **Thread safety** — both managers use `ConcurrentHashMap`; keep it that way if modifying state from async contexts (e.g. `AsyncChatEvent` in `FilterGuiListener`).
7. **Event priority** — `BlockDropItemEvent` is handled at `HIGHEST` with `ignoreCancelled = true` so other plugins can cancel drops first.
8. **Overflow drops use block world** — always call `dropLocation.getWorld()`, not `player.getWorld()`, to avoid cross-world item drops.
9. **No new files without need** — prefer editing existing files; keep the package structure flat and minimal.
