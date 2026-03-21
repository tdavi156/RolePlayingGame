# Project: Android RPG Game

## Tech Stack
- **Language:** Kotlin
- **Framework:** LibGDX + KTX extensions
- **ECS:** Fleks (entity component system)
- **UI:** Scene2D (MVVM pattern: Views, ViewModels, Widgets)
- **Build:** `./gradlew :core:compileKotlin` to verify compilation
- **Source root:** `core/src/main/kotlin/com/github/jacks/roleplayinggame/`

---

## Architecture Overview

### UI Pattern (MVVM)
- **Views** (`ui/views/`) extend `Table`, use `setFillParent(true)`, built with KTX Scene2D DSL
- **ViewModels** (`ui/viewmodels/`) extend `PropertyChangeSource`, implement `EventListener`, hold observable properties via `propertyNotify`
- **Widgets** (`ui/widgets/`) are reusable UI components (extend `Table` or `WidgetGroup`)
- Views bind to ViewModel properties via `model.onPropertyChange(ViewModel::prop) { value -> ... }`

### Event System
- Events fired on `gameStage` via `gameStage.fire(SomeEvent())`
- Systems and ViewModels register as `EventListener` on `gameStage`
- Events are synchronous — `fire()` blocks until all listeners return
- `system.enabled = false` only stops `onTick`/`onTickEntity`, NOT event handlers

### Viewports
- `gameStage`: `FitViewport(24f, 13.5f)` — world units, fixed aspect ratio
- `uiStage`: `ScreenViewport()` — pixel-based, 1 unit = 1 pixel

### Skin System (`ui/Skin.kt`)
- `Drawables`, `Labels`, `Buttons` enums for atlas-based lookups
- Access: `skin[Drawables.LIFE_BAR]`, `skin[Fonts.SMALL]`

---

## Key Files

| File | Purpose |
|------|---------|
| `screens/GameScreen.kt` | Main screen. Owns `gameStage`, `uiStage`, `entityWorld`. Manages system enable/disable, UI layer transitions (fade in/out), input processors. `render()` drives the ECS tick. |

---

## Next Feature

# Feature 12 — Save System Overhaul (Prefs → JSON Serialization)

Remove all `Preferences`-based saving in favor of a central `SaveManager` class that serializes game state to JSON files using LibGDX's built-in `Json`. One `game_save.json` for all game state, a separate `settings.json` for user preferences so settings survive a new game. `SaveManager` is the **single point of responsibility** for all file I/O — no system writes files directly.

### What's currently saved via prefs (all migrated by this feature):
| System / Class | What it saves |
|---|---|
| `PartySystem` | Per-character stats (20+ fields each), combat slots, active character ID |
| `ResourceSystem` | Gold |
| `QuestSystem` | Quest status + progress per quest |
| `MapSystem` | Current map name, spawner `isSpawned` + `currentTime` |
| `SpawnerSystem` | `isSpawned` on spawn event |
| `InitializeGameSystem` | Coordinates all loading; seeds defaults on first run |
| `SettingsViewModel` | Audio volumes, animation speed, auto-clear text |

### Known gap being fixed:
`InventorySystem` has **no save/load today** — inventory reseeds every boot from hardcoded defaults. This feature adds full inventory persistence.

### Save file layout:
- `save/game_save.json` — all game state (party, resources, inventory, quests, map/spawners)
- `save/settings.json` — user preferences (audio, display options) — preserved on new game

### Temp data (battle entry) — stays in-memory only:
`preBattleMapName`, `preBattlePlayerX/Y` in `MapSystem` are already in-memory. No disk save needed for these.

### `SaveManager` API summary:
- `hasSave(): Boolean` — checks if `game_save.json` exists
- `saveFull(data: GameSaveData)` — write to disk, update cache
- `gatherAndSave(world: World)` — collect from all systems, then `saveFull()`
- `load(): GameSaveData?` — read from disk (null = no save, use defaults)
- `saveSettings(data: SettingsSaveData)` / `loadSettings(): SettingsSaveData?`
- `findSpawnerState(spawnerId, mapId): SpawnerEntrySaveData?` — lookup from cache for `SpawnerSystem`

---

## Implementation Order

1. **Part 1 — `SaveData.kt` + `SaveManager.kt` + inject into `GameScreen`**
   - Create `systems/SaveData.kt`: all `*SaveData` data classes (`CharacterSaveData`, `PartySaveData`, `ResourceSaveData`, `ItemEntrySaveData`, `InventorySaveData`, `QuestEntrySaveData`, `SpawnerEntrySaveData`, `MapSaveData`, `GameSaveData`, `SettingsSaveData`). All fields have default values for LibGDX Json no-arg construction. `ArrayList<T>` used for object lists; comma-separated `String` for int lists (ability IDs, combat slots).
   - Create `systems/SaveManager.kt`: implements all API above using `com.badlogic.gdx.utils.Json` + `Gdx.files.local()`. Caches the last loaded/saved `GameSaveData` in memory for fast spawner lookups.
   - Add `var currentMapName: String = "map_1"` to `MapSystem` (set on every `setMap()` / `setBattleMap()` / `returnToOverworld()`).
   - Add `fun collectSpawnerSaveData(): ArrayList<SpawnerEntrySaveData>` to `MapSystem` (iterates spawner family from ECS).
   - Construct `SaveManager` in `GameScreen`, add as ECS world injectable.

2. **Part 2 — Rework `InitializeGameSystem`**
   - Replace `is_game_initialized` prefs check with `saveManager.hasSave()`.
   - On **no save** (new game): seed defaults for party/resources/settings; call `saveManager.gatherAndSave(world)` to write initial save file.
   - On **save found**: `saveManager.load()` → distribute `GameSaveData` to `PartySystem` (characters, slots), `ResourceSystem` (gold), `InventorySystem` (items), `QuestSystem` (quest states); `saveManager.loadSettings()` → `SettingsSystem`.
   - `seedStartingInventory()` only runs when no save exists.
   - Remove all `Preferences` usage from `InitializeGameSystem`.
   - Remove `resetOnStart` prefs clear from `RolePlayingGame` (replace with save file delete if still needed for testing).

3. **Part 3 — Strip prefs from `PartySystem`, `ResourceSystem`, `QuestSystem`**
   - `PartySystem`: remove `saveCharacterData()`, `saveCombatSlots()`, `saveActiveCharacter()`, all prefs fields. Callers that previously called these now call `saveManager.gatherAndSave(world)` instead (in `StatSystem`, `AbilitySystem`, `PartySystem.unlockCharacter()`).
   - `ResourceSystem`: remove `saveResources()` and prefs field. Call sites (QuestSystem reward, ShopSystem) call `saveManager.gatherAndSave(world)`.
   - `QuestSystem`: remove `saveState()`, `loadState()`, and prefs field. `handle()` calls `saveManager.gatherAndSave(world)` after state changes.
   - Remove all `ktx.preferences` imports from these systems.

4. **Part 4 — Strip prefs from `MapSystem` + `SpawnerSystem`**
   - `MapSystem`: remove prefs field and all inline `preferences.flush {}` calls. `saveCurrentMapData()` → `saveManager.gatherAndSave(world)`. Spawner state is gathered via `collectSpawnerSaveData()` already added in Part 1.
   - `SpawnerSystem`: remove prefs field. On `MapChangeEvent`, read initial `isSpawned` / `currentTime` from `saveManager.findSpawnerState(spawnerId, mapId)` (null = default unspawned). On `onTickEntity` spawn, do NOT write prefs; `saveManager` will capture state on the next `gatherAndSave` call.
   - Remove all `ktx.preferences` imports from both systems.

5. **Part 5 — `InventorySystem` persistence + `SettingsViewModel` + `MenuViewModel`**
   - Add `fun restoreInventory(data: InventorySaveData)` to `InventorySystem` to populate lists from saved `ItemEntrySaveData` entries (looks up item config by ID and restores quantity).
   - `SettingsViewModel.save()`: remove raw prefs call; call `saveManager.saveSettings(SettingsSaveData(...))` instead.
   - `MenuViewModel`: remove dead `planetaryIdlePrefs` field; add `saveManager: SaveManager` constructor param; wire the save game action to `saveManager.gatherAndSave(world)`.
   - Update `GameScreen` to pass `saveManager` to `MenuViewModel`.
   - Remove `ktx.preferences` imports from `SettingsViewModel` and `MenuViewModel`.

---

## Key Files Reference

| File | Path |
|------|------|
| GameScreen | `core/src/main/kotlin/.../screens/GameScreen.kt` |
| SaveManager | `core/src/main/kotlin/.../saveManager/SaveManager.kt` |
| SaveData | `core/src/main/kotlin/.../saveManager/SaveData.kt` |
| CharacterData | `core/src/main/kotlin/.../saveManager/CharacterData.kt` |
| InitializeGameSystem | `core/src/main/kotlin/.../systems/InitializeGameSystem.kt` |
| PartySystem | `core/src/main/kotlin/.../systems/PartySystem.kt` |
| ResourceSystem | `core/src/main/kotlin/.../systems/ResourceSystem.kt` |
| QuestSystem | `core/src/main/kotlin/.../systems/QuestSystem.kt` |
| MapSystem | `core/src/main/kotlin/.../systems/MapSystem.kt` |
| SpawnerSystem | `core/src/main/kotlin/.../systems/SpawnerSystem.kt` |
| InventorySystem | `core/src/main/kotlin/.../systems/InventorySystem.kt` |
| SettingsViewModel | `core/src/main/kotlin/.../ui/viewmodels/SettingsViewModel.kt` |
| MenuViewModel | `core/src/main/kotlin/.../ui/viewmodels/MenuViewModel.kt` |
| RolePlayingGame | `core/src/main/kotlin/.../RolePlayingGame.kt` |

## Verification

1. `./gradlew :core:compileKotlin` — must pass after each part
