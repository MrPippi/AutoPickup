# AutoPickup Plugin — AI Assistant Guide

## Project Overview

A Paper Minecraft plugin (Java 21, Maven) that auto-pickups block drops directly into the player's inventory when mining, with per-player toggle support.

- **Plugin version:** 1.0.0
- **Paper API:** 1.21.1-R0.1-SNAPSHOT (API version `1.21`)
- **Java:** 21 (required — enforced by `maven-enforcer-plugin`)
- **Build output:** `target/AutoPickup-1.0.0.jar`

---

## Build & Test

```bash
# Build the shaded jar
mvn clean package

# Run tests only (no server required)
mvn test

# Using the Maven wrapper (if mvn not on PATH)
./mvnw clean package
./mvnw test
```

**Requirements:** JDK 21 exactly (`[21,22)` range enforced). Set `JAVA_HOME` to JDK 21 before building.

---

## Source Structure

```
src/main/java/com/autopickup/
├── AutoPickupPlugin.java               Main plugin class (lifecycle, config, messaging)
├── command/
│   ├── AutoPickupCommand.java          /autopickup command executor
│   └── AutoPickupTabCompleter.java     Tab-completer (suggests on/off)
├── listener/
│   └── BlockBreakListener.java         BlockDropItemEvent handler (core pickup logic)
├── manager/
│   └── PlayerStateManager.java         Per-player state: in-memory + YAML persistence
└── placeholder/
    └── AutoPickupPlaceholder.java      PlaceholderAPI expansion (%autopickup% / %autopickup_status%)

src/main/resources/
├── plugin.yml      Plugin metadata, commands, permissions
└── config.yml      settings.default-enabled + all user-facing messages

src/test/java/com/autopickup/
├── command/AutoPickupCommandTest.java  Unit tests for command (Mockito, no server)
└── manager/PlayerStateManagerTest.java Unit tests for state manager (TempDir I/O)
```

---

## Key Architecture

### AutoPickupPlugin (main class)
- Calls `saveDefaultConfig()` then reads `settings.default-enabled`
- Instantiates `PlayerStateManager`, registers `BlockBreakListener` and `AutoPickupCommand`
- PlaceholderAPI integration is **optional** and loaded via reflection so the plugin runs without PAPI on the classpath
- `getMessage(String path)` reads from `config.yml` and parses `&` color codes via `LegacyComponentSerializer`

### BlockBreakListener
- Listens on `BlockDropItemEvent` at `EventPriority.HIGHEST` with `ignoreCancelled = true`
- Skips: null player, `CREATIVE` game mode, players with auto-pickup disabled
- Clears the event's item list (so items don't drop), adds stacks to inventory
- Overflow items (full inventory) are dropped at the **block's world** (`dropLocation.getWorld()`, not `player.getWorld()`)

### PlayerStateManager
- State map: `ConcurrentHashMap<UUID, Boolean>` (thread-safe)
- Persists to `plugins/AutoPickup/players.yml` under the `players:` key
- Dirty flag: `save()` is a no-op when nothing changed; called explicitly after each toggle
- `load()` is called once on startup; `save()` is called on disable and after each `/autopickup` use

### AutoPickupCommand
- `/autopickup` (alias `/ap`), permission: `autopickup.use` (default: `true`)
- No args → toggles current state
- `on` / `off` args → sets explicitly; any other arg → sends `messages.invalid-usage`
- Players-only; console gets `messages.players-only`

### AutoPickupTabCompleter
- Only suggests to `Player` senders with `autopickup.use` permission
- Returns filtered `["on", "off"]` for the first argument only

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
  invalid-usage:   "&cUsage: /autopickup [on|off]"
```

All message values support `&` color codes. Accessed via `AutoPickupPlugin.getMessage(String path)`.

### `plugin.yml`
- Command: `autopickup`, alias: `ap`
- Permission: `autopickup.use` (default `true` — all players can use it)

---

## Data File

`plugins/AutoPickup/players.yml` (auto-created):
```yaml
players:
  aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee: true
  11111111-2222-3333-4444-555555555555: false
```

Keys are UUID strings. Invalid UUID keys are silently skipped on load.

---

## Testing Conventions

- **No game server needed** — tests use Mockito mocks for Paper API types
- `AutoPickupCommandTest`: mocks `AutoPickupPlugin`, `PlayerStateManager`, `Player`, `CommandSender`
  - `lenient().when(plugin.getMessage(anyString()))` returns `Component.text(path)` so message key is verifiable
- `PlayerStateManagerTest`: uses `@TempDir File` for real YAML I/O without a server
- Run with `mvn test`; surefire reports land in `target/surefire-reports/`

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
2. **Adventure API for all messages** — never use `ChatColor`; always go through `AutoPickupPlugin.getMessage()` or `LegacyComponentSerializer`.
3. **No static plugin references** — pass `AutoPickupPlugin` or `PlayerStateManager` via constructor injection.
4. **PlaceholderAPI is optional** — any code that directly references PAPI classes must remain in `placeholder/` and be loaded only via reflection in `AutoPickupPlugin.onEnable`.
5. **Save on toggle, save on disable** — `stateManager.save()` must be called after `setEnabled()` in command code, and in `onDisable()`.
6. **Thread safety** — `PlayerStateManager` uses `ConcurrentHashMap`; keep it that way if modifying state from async contexts.
7. **Event priority** — `BlockDropItemEvent` is handled at `HIGHEST` with `ignoreCancelled = true` so other plugins can cancel drops first.
8. **Overflow drops use block world** — always call `dropLocation.getWorld()`, not `player.getWorld()`, to avoid cross-world item drops.
9. **Tests are offline** — do not introduce dependencies on a live Bukkit server in tests; mock everything Paper-related.
10. **No new files without need** — prefer editing existing files; keep the package structure flat and minimal.
