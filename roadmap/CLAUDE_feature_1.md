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
| `systems/BattleSystem.kt` | Contains events for battle actions and animations. Drives battle flow and enemy death logic. |
| `systems/InitializeGameSystem.kt` | Handles game startup — loads prefs, initializes systems. Pattern for loading/writing defaults. |
| `ui/views/BattleView.kt` | Battle UI layer. Reward overlay will be shown here at end of battle. |
| `ui/views/CharacterInfoView.kt` | Displays character stats. Will be extended to show gold. |
| `components/ItemComponent.kt` | Holds item data per entity. To be refactored to reference items by name key. |

---

## Next Feature

# Items, Loot, and Gold Reward System

## Context
- `Resources` is account-scoped (not character-scoped) — gold must persist independently of the active character, as multiple characters will be supported in future
- Items are currently defined via `ItemType` enum in `ItemComponent.kt` — this will be migrated to a named data class config in `Items.kt`
- Loot drops are intentionally rare: typical enemies roll a drop chance first (e.g. 25%), then select randomly from an equal-weight item pool
- The reward screen is an overlay inside `BattleView` — battle does not conclude until the player dismisses it

---

## Part 1 — Create `Resources.kt` Data Class

Create `configurations/resources/Resources.kt`:
- Data class `Resources` with field: `gold: Int = 0`
- Companion object with pref key constant: `KEY_GOLD`
- Designed for future expansion — additional resource types (crystals, jewels, quest items) added here as new fields

---

## Part 2 — Create `ResourceSystem.kt`

Create `systems/ResourceSystem.kt`:
- Extend `IntervalSystem()`, implement `EventListener`
- Hold `var resources = Resources()` as single runtime source of truth
- No prefs access here — loaded in Part 3, saved on gold change
- Register in `GameScreen.kt` alongside other systems
- Expose a `saveResources(preferences)` method for other systems to call after modifying gold

---

## Part 3 — Load Resources from Prefs on Startup

Modify `systems/InitializeGameSystem.kt`:
- Inject `ResourceSystem` via world (same pattern as `SettingsSystem`)
- After existing init logic, check for `KEY_GOLD` in `"rolePlayingGamePrefs"`: if absent, write defaults via `preferences.flush { ... }`; if present, read and assign to `resourceSystem.resources`
- Use the existing `preferences` instance — do not open a second one

---

## Part 4 — Refactor `ItemComponent.kt` and `ItemModel.kt`

Modify `components/ItemComponent.kt` and `ItemModel.kt`:
- Remove `ItemType` enum as the source of item data
- Update `ItemComponent` and `ItemModel` to reference items by `name: String` key
- Item category, stats, and display data will now live in `Items.kt` (Part 5)
- Keep compilation passing — existing item references will be updated in Part 5

---

## Part 5 — Create `Items.kt` Configuration File

Create `configurations/Items.kt`:
- Enum class `ItemCategory { HELMET, WEAPON, ARMOR, BOOTS }`
- Data class `ItemData` with fields: `name: String`, `category: ItemCategory`, `uiAtlasKey: String`, `stats: Map<StatType, Int>` — `Map` enforces no duplicate `StatType` per item
- Migrate all existing `ItemType` entries into `ItemData` instances, preserving original values exactly
- Include a commented template block at the top of the item list for adding new items:
  ```
  // ItemData(
  //     name = "Item Name",
  //     category = ItemCategory.WEAPON,
  //     uiAtlasKey = "atlas_key",
  //     stats = mapOf(StatType.ATTACK_DAMAGE to 10, StatType.DEFENSE to 2)
  // ),
  ```

---

## Part 6 — Create `ItemPools.kt` Configuration File

Create `configurations/ItemPools.kt`:
- Named pools as `List<String>` of item name keys: `TIER_1_ITEMS`, `TIER_2_ITEMS` (populated from migrated items — assign tiers based on item strength)
- Utility functions:
  - `rollForDrop(chance: Int): Boolean` — takes 1–100, returns true if random roll ≤ chance
  - `rollRandomItem(pool: List<String>): ItemData` — returns a uniformly random `ItemData` from the pool by name lookup against `Items.kt`
- Include a comment block for adding new pools and assigning items to tiers

---

## Part 7 — Add Reward Fields to Enemy Configuration

Modify the existing enemy config file:
- Add fields per enemy type: `expReward: Int`, `goldReward: Int`, `lootPool: List<String>?` (null = no item drop possible), `lootChance: Int` (1–100)
- Typical enemy example: `lootChance = 25`, `lootPool = TIER_1_ITEMS`
- Boss example: `lootChance = 100`, `lootPool = listOf("Specific Item Name")` for a guaranteed named drop
- Populate all existing enemy types with appropriate values

---

## Part 8 — Resolve Rewards in `BattleSystem.kt`

Modify `systems/BattleSystem.kt`:
- After enemy death, resolve rewards: collect fixed `expReward` and `goldReward` from the enemy config
- If `lootPool` is non-null: call `rollForDrop(enemy.lootChance)` — if true, call `rollRandomItem(enemy.lootPool)` to select an item
- Package results into a `BattleRewardData` object: `expGained: Int`, `goldGained: Int`, `itemDropped: ItemData?`
- Fire `BattleRewardEvent(rewardData)` on `gameStage`

---

## Part 9 — Create `RewardViewModel.kt`

Create `ui/viewmodels/RewardViewModel.kt`:
- Extend `PropertyChangeSource`, implement `EventListener`; register on `uiStage`
- Observable properties: `expGained by propertyNotify(0)`, `goldGained by propertyNotify(0)`, `itemDropped by propertyNotify<ItemData?>(null)`
- On `BattleRewardEvent`: populate all properties from `rewardData`, apply `goldGained` delta to `resourceSystem.resources.gold`, call `resourceSystem.saveResources(preferences)`

---

## Part 10 — Build `RewardView.kt` Overlay Widget

Create `ui/views/RewardView.kt` (or `ui/widgets/RewardView.kt` if treated as a reusable widget):
- Extend `Table(skin)`, mix `KTable`
- Layout: EXP gained, gold gained, item drop row (icon from `uiAtlasKey` + item name, or blank if null)
- Confirm button fires `RewardDismissedEvent` on `uiStage`
- Bind all fields to `RewardViewModel` properties via `model.onPropertyChange()`
- Add DSL extension function following existing view patterns

---

## Part 11 — Wire RewardView as `BattleView` Overlay

Modify `ui/views/BattleView.kt`:
- Add `RewardView` as a hidden overlay, shown when `BattleRewardEvent` is received
- Player input (keyboard confirm or mouse click) fires `RewardDismissedEvent`
- `RewardDismissedEvent` hides the overlay and triggers the existing battle-end → overworld transition

---

## Part 12 — Display Gold in `CharacterInfoView`

Modify `ui/views/CharacterInfoView.kt` (or its ViewModel):
- Inject `ResourceSystem` via world
- Add a gold display field bound to `resourceSystem.resources.gold`
- Updates reactively when gold changes (e.g. after reward is applied in Part 9)

---

## Part 13 — Verification Pass

- Confirm all existing items migrated correctly from `ItemType` — stats and categories match original values
- Confirm gold persists correctly across game restart via prefs
- Confirm loot rolls behave correctly: 0% chance never drops, 100% always drops
- `./gradlew :core:compileKotlin` must pass after each part

---

## Implementation Order

1. **Part 1** — Create `Resources.kt` data class with `gold: Int = 0` and pref key constants in `configurations/resources/`
2. **Part 2** — Create `ResourceSystem` holding the active `Resources` instance; register in `GameScreen`
3. **Part 3** — Update `InitializeGameSystem` to load gold from prefs on startup — write defaults if absent, populate system if present
4. **Part 4** — Refactor `ItemComponent` and `ItemModel` to reference items by string name key, removing `ItemType` enum dependency
5. **Part 5** — Create `Items.kt` with `ItemData` data class and `ItemCategory` enum; migrate all existing items; include commented add-item template
6. **Part 6** — Create `ItemPools.kt` with `TIER_1_ITEMS` / `TIER_2_ITEMS` pools and `rollForDrop()` / `rollRandomItem()` utility functions
7. **Part 7** — Add `expReward`, `goldReward`, `lootPool`, and `lootChance` fields to all existing enemy config entries
8. **Part 8** — Resolve rewards in `BattleSystem` after enemy death — roll drop chance, select item if applicable, fire `BattleRewardEvent`
9. **Part 9** — Create `RewardViewModel` to handle `BattleRewardEvent`, populate observable properties, and apply gold delta to `ResourceSystem`
10. **Part 10** — Build `RewardView` overlay widget — displays EXP, gold, and optional item drop with confirm button firing `RewardDismissedEvent`
11. **Part 11** — Wire `RewardView` into `BattleView` as a hidden overlay; show on reward event, dismiss on player input, then trigger existing battle-end transition
12. **Part 12** — Add gold display field to `CharacterInfoView`, bound reactively to `ResourceSystem`
13. **Part 13** — Verification pass: item migration, gold prefs persistence, loot roll correctness, compilation after each part

---

## Key Files Reference

| File | Path |
|------|------|
| GameScreen | `core/src/main/kotlin/.../screens/GameScreen.kt` |
| BattleSystem | `core/src/main/kotlin/.../systems/BattleSystem.kt` |
| InitializeGameSystem | `core/src/main/kotlin/.../systems/InitializeGameSystem.kt` |
| BattleView | `core/src/main/kotlin/.../ui/views/BattleView.kt` |
| CharacterInfoView | `core/src/main/kotlin/.../ui/views/CharacterInfoView.kt` |
| ItemComponent | `core/src/main/kotlin/.../components/ItemComponent.kt` |
| ItemModel | `core/src/main/kotlin/.../components/ItemModel.kt` |
| Events | `core/src/main/kotlin/.../events/Events.kt` |
| **[NEW] Resources** | `core/src/main/kotlin/.../configurations/resources/Resources.kt` |
| **[NEW] ResourceSystem** | `core/src/main/kotlin/.../systems/ResourceSystem.kt` |
| **[NEW] Items** | `core/src/main/kotlin/.../configurations/Items.kt` |
| **[NEW] ItemPools** | `core/src/main/kotlin/.../configurations/ItemPools.kt` |
| **[NEW] RewardViewModel** | `core/src/main/kotlin/.../ui/viewmodels/RewardViewModel.kt` |
| **[NEW] RewardView** | `core/src/main/kotlin/.../ui/views/RewardView.kt` |

## Verification

1. `./gradlew :core:compileKotlin` — must pass after each part
