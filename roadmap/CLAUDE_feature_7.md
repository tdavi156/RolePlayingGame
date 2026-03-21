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
| `systems/BattleSystem.kt` | Drives battle flow and turn order. Extended to handle `CastSpellEvent` and gate turn advancement on spell result dismissal. |
| `systems/StatSystem.kt` | Tracks stat numbers only — decrements `abilityPoints` on save. Does not own ability unlock logic. |
| `ui/views/BattleView.kt` | Battle UI layer. Action buttons renamed; Spells button enable/disable wired reactively; spell list panel added. |
| `ui/views/AbilityView.kt` | Currently a stub. Fully implemented as an ability tree UI in this feature. |
| `ui/viewmodels/AbilityViewModel.kt` | Currently a stub. Fully implemented with node state, pending unlock, and save/cancel flow. |
| `configurations/AbilityTrees.kt` | Does not exist yet. Defines all ability trees, nodes, effects, and prerequisites. |
| `components/AbilityComponent.kt` | Does not exist yet. Holds per-character runtime unlock state. |
| `systems/AbilitySystem.kt` | Does not exist yet. Owns all ability unlock logic, `AbilityComponent` mutation, and prefs persistence. |
| `systems/InitializeGameSystem.kt` | Seeds 3 starting ability points for testing. |
| `events/Events.kt` | Central event definitions. All new ability and spell events added here. |

---

## Next Feature

# Ability Points, Ability Tree, and Spells in Battle

## Context
- Battle action buttons renamed: `"Flee"` → `"Escape"`, `"Skills"` → `"Spells"`
- Spells button is dynamically enabled/disabled based on whether the active character has any skilled abilities in `AbilityComponent`
- Ability tree is defined in `AbilityTrees.kt` — one tree per character, all trees in a single file keyed by `characterId`
- Per-character unlock state lives in `AbilityComponent` on the player entity — not in the config
- `AbilitySystem` owns all unlock logic and `AbilityComponent` mutation; `StatSystem` only tracks the `abilityPoints` count
- `AbilityEffect` is a sealed class — parameterized effects (`DamageEnemy(amount)`, `HealSelf(amount)`) are extensible without restructuring the config
- Spell cast turn flow mirrors Feature 4 item use: result message shown, turn advances only after player dismisses it via `SpellCastDismissedEvent`
- Mana is deducted immediately on cast — no confirmation step
- Targeting is implicit per ability: `DamageEnemy` always targets the enemy, `HealSelf` always targets the caster
- Character switcher in `AbilityView` is present in the UI but no-op with one character — wired for future expansion
- 3 ability points seeded at init for testing — marked with a TODO for removal
- Ability unlock state persists to prefs as part of character data

---

## Part 1 — Rename Battle Action Buttons and Wire Spells Enable/Disable

Modify `ui/views/BattleView.kt` and its ViewModel:
- Rename button labels: `"Flee"` → `"Escape"`, `"Skills"` → `"Spells"`
- Add `spellsButtonEnabled by propertyNotify(false)` to BattleViewModel
- On battle start: check `AbilityComponent.unlockedAbilityIds` on the player entity — if non-empty, set `spellsButtonEnabled = true`; otherwise `false`
- Listen for `AbilitySkillChangedEvent` (Part 2): re-evaluate and update `spellsButtonEnabled` reactively
- Add `AbilitySkillChangedEvent(val entity: Entity)` to `events/Events.kt`

---

## Part 2 — Create `AbilityComponent` and `AbilityTrees.kt`

Create `components/AbilityComponent.kt`:
- Holds per-character runtime ability state: `unlockedAbilityIds: MutableSet<Int>`
- `isSkilled(abilityId: Int): Boolean` convenience method
- Added to the player entity; loaded from prefs on game start (Part 3)

Create `configurations/AbilityTrees.kt`:
- Sealed class `AbilityEffect`: `data class DamageEnemy(val amount: Int)`, `data class HealSelf(val amount: Int)` — add new subclasses here for future effect types
- Data class `AbilityNode`: `id: Int`, `name: String`, `description: String`, `manaCost: Int`, `abilityPointCost: Int = 1`, `prerequisiteIds: List<Int>`, `atlasKey: String`, `effect: AbilityEffect`
- Data class `AbilityTree`: `characterId: Int`, `nodes: List<AbilityNode>`
- Define `ABILITY_TREE_CHARACTER_1` with 3 nodes:
  - Node 1 (`id=1`): `"Ability 1"`, `DamageEnemy(5)`, mana cost 2, no prerequisites, `atlasKey = "ability_1"`
  - Node 2 (`id=2`): `"Ability 2"`, `HealSelf(5)`, mana cost 3, prerequisiteIds = `[1]`, `atlasKey = "ability_2"`
  - Node 3 (`id=3`): `"Ability 3"`, `DamageEnemy(20)`, mana cost 10, prerequisiteIds = `[2]`, `atlasKey = "ability_3"`
- Top-level registry: `val ABILITY_TREES: Map<Int, AbilityTree>` keyed by `characterId`
- Include a commented template block for adding new nodes:
  ```
  // AbilityNode(
  //     id = 4,
  //     name = "Ability Name",
  //     description = "Short description of what it does.",
  //     manaCost = 5,
  //     prerequisiteIds = listOf(3),
  //     atlasKey = "ability_4",
  //     effect = AbilityEffect.DamageEnemy(15)
  // ),
  ```

Add to `events/Events.kt`:
- `class AbilityPointsSaveEvent(val entity: Entity, val pendingIds: Set<Int>) : Event()`
- `class AbilityViewOpenEvent : Event()`
- `class AbilityViewClosedEvent : Event()`
- `class CastSpellEvent(val abilityId: Int, val casterEntity: Entity) : Event()`
- `class SpellCastDismissedEvent : Event()`

---

## Part 3 — Load `AbilityComponent` State from Prefs on Startup

Modify `systems/InitializeGameSystem.kt`:
- After existing init logic, load unlocked ability IDs from prefs into `AbilityComponent` on the player entity — same pattern as loading settings/resources
- If prefs key absent (first launch), write empty set as default
- Pref key constant defined in `AbilityComponent` companion object: `KEY_UNLOCKED_ABILITY_IDS`

---

## Part 4 — Create `AbilitySystem.kt`

Create `systems/AbilitySystem.kt`:
- Extend `IntervalSystem()`, implement `EventListener`; register in `GameScreen.kt`
- Handle `AbilityPointsSaveEvent(entity, pendingIds)`:
  - Add all `pendingIds` to `AbilityComponent.unlockedAbilityIds`
  - Save updated unlocked IDs to prefs via `KEY_UNLOCKED_ABILITY_IDS`
  - Fire `AbilitySkillChangedEvent(entity)` — triggers Spells button re-evaluation in `BattleView`
  - Fire `AbilityViewClosedEvent`
- Handle `AbilitySkillChangedEvent`: no-op here — consumed by BattleViewModel directly
- `AbilitySystem` is the sole mutator of `AbilityComponent` — nothing else modifies `unlockedAbilityIds` directly

Modify `systems/StatSystem.kt`:
- Handle `AbilityPointsSaveEvent`: decrement `statComponent.abilityPoints` by `pendingIds.size`
- `StatSystem` tracks the count only — it does not inspect which IDs were spent

---

## Part 5 — Build `AbilityViewModel.kt`

Rework `ui/viewmodels/AbilityViewModel.kt` (currently placeholder):
- Extend `PropertyChangeSource`, implement `EventListener`; register on `uiStage`
- Data class `AbilityNodeUiState`: wraps `AbilityNode` with computed display flags: `isSkilled: Boolean`, `isUnlockable: Boolean` (all prerequisites met, not yet skilled, points available), `isPending: Boolean`, `isLocked: Boolean` (prerequisites not yet met)
- Observable properties: `abilityPoints by propertyNotify(0)`, `activeCharacterIndex by propertyNotify(0)`, `nodes by propertyNotify(emptyList<AbilityNodeUiState>())`, `pendingUnlockIds by propertyNotify(emptySet<Int>())`, `hasUnsavedChanges by propertyNotify(false)`, `showCancelConfirm by propertyNotify(false)`, `showSaveConfirm by propertyNotify(false)`
- On `AbilityViewOpenEvent`: load tree for `activeCharacterIndex` from `ABILITY_TREES`; compute `AbilityNodeUiState` for each node against `AbilityComponent`; populate `abilityPoints` from `StatComponent`; reset all pending state
- `+` on a node: only if `isUnlockable` — add to `pendingUnlockIds`, decrement `abilityPoints`, recompute all node states (a newly pending node may unlock the next node's `isUnlockable`)
- `-` on a pending node: remove from `pendingUnlockIds`, increment `abilityPoints`, recompute node states — cannot remove already-skilled nodes
- `hasUnsavedChanges = pendingUnlockIds.isNotEmpty()`

---

## Part 6 — Build `AbilityView.kt`

Rework `ui/views/AbilityView.kt` (currently placeholder):
- Extend `Table(skin)`, mix `KTable`, `setFillParent(true)`
- **Header**: `"Ability Points: X"` + character switcher: `[<] CharacterName [>]` — arrows present but no-op with one character; updates `model.activeCharacterIndex` when multi-character is supported
- **Tree panel**: nodes as circles connected by vertical lines, top to bottom
  - Each node: ability icon from `abilityIcons.atlas` via `atlasKey`, node name below icon
  - Visual states: skilled (filled/bright), pending (highlighted border), unlockable (dim, `[+]` visible), locked (greyed out, no button)
  - `[-]` button visible only when `isPending` for that node
  - `[+]` button visible only when `isUnlockable`; disabled when `abilityPoints == 0`
  - Connecting lines between nodes — dimmed if the lower node is locked, normal if unlockable or skilled
- **Footer**: Save and Cancel buttons — same confirm/warning flow as `SkillView` (Feature 6):
  - Cancel with unsaved changes: `"Discard unsaved changes?"` Yes/No
  - Save: `"Changes cannot be undone. Confirm?"` Yes/No — Yes fires `AbilityPointsSaveEvent`
- Bind all elements to `AbilityViewModel` via `model.onPropertyChange()`
- DSL extension function following existing view patterns

---

## Part 7 — Build Spell List Panel in `BattleView`

Modify `ui/views/BattleView.kt` and BattleViewModel:
- When `"Spells"` is selected (and `spellsButtonEnabled == true`): replace the 4 action buttons with a scrollable spell list — contained within existing bottom panel dimensions, matching the reference image layout (rows on left, info on right)
- BattleViewModel exposes `availableSpells`: list of `AbilityNode` filtered to `AbilityComponent.unlockedAbilityIds`, ordered by node ID ascending
- Each row: `[Spell Name] ---- [X MP]`
  - Faded and unselectable if `currentMana < manaCost`
  - Focused row updates a fixed description area (right side of panel per reference image) — never obscures other rows
- Enter or click on a valid (affordable) spell: fire `CastSpellEvent(abilityId, casterEntity)` on `gameStage`
- Clicking an unaffordable spell: brief inline message `"Not enough MP"` — does not end turn
- ESC: return to 4 action buttons panel without consuming the turn

---

## Part 8 — Handle `CastSpellEvent` in `BattleSystem`

Modify `systems/BattleSystem.kt`:
- Handle `CastSpellEvent(abilityId, casterEntity)`:
  - Look up `AbilityNode` by `abilityId` from `ABILITY_TREES`
  - Deduct `manaCost` from caster's current mana via `StatSystem`
  - Apply `AbilityEffect`:
    - `DamageEnemy(amount)`: deal damage to enemy — fire `FloatingTextEvent(enemy, amount.toString(), Fonts.DAMAGE)`, trigger hit flash (established pattern)
    - `HealSelf(amount)`: restore HP to caster — fire `FloatingTextEvent(caster, "+${amount} HP", Fonts.DAMAGE)`, trigger heal flash (green); add TODO comment for heal font
  - Show result message in battle log: `"[CharacterName] cast [SpellName]!"` + effect line
  - Gate turn advancement on `SpellCastDismissedEvent` — same pattern as `CombatItemUseDismissedEvent` in Feature 4
- Handle `SpellCastDismissedEvent`: advance turn to next entity

---

## Part 9 — Seed Starting Ability Points for Testing

Modify `systems/InitializeGameSystem.kt`:
- After existing init logic, fire `GainAbilityPointEvent(playerEntity)` three times — uses established event pattern from Feature 6
- Add comment:
  ```
  // TODO: Remove after abilities testing is complete
  ```

---

## Part 10 — Verification Pass

- Confirm button labels: `"Escape"` and `"Spells"` in `BattleView`
- Confirm Spells button disabled with no skilled abilities; enabled after at least one ability is skilled and saved via `AbilitySkillChangedEvent`
- Confirm ability tree node visual states: locked, unlockable, pending, skilled — correct per prerequisite chain
- Confirm prerequisite enforcement: Node 2 not unlockable until Node 1 is skilled or pending; Node 3 not unlockable until Node 2
- Confirm pending state: `[-]` only available for pending nodes; recomputes downstream `isUnlockable` correctly
- Confirm cancel confirm on unsaved changes; save confirm before applying
- Confirm `AbilitySystem` updates `AbilityComponent` and saves to prefs; `StatSystem` decrements `abilityPoints` count only
- Confirm ability unlock state survives game restart via prefs
- Confirm spell list in battle shows only skilled abilities, ordered by node ID
- Confirm insufficient mana: spell row faded, unselectable, `"Not enough MP"` message shown
- Confirm spell effects: `DamageEnemy` deals correct damage with hit flash and floating text; `HealSelf` restores correct HP with heal flash and floating text
- Confirm mana deducted immediately on cast
- Confirm turn advances only after result message dismissed via `SpellCastDismissedEvent`
- Confirm 3 starting ability points seeded at init
- `./gradlew :core:compileKotlin` — must pass after each part

---

## Implementation Order

1. **Part 1** — Rename `"Flee"` → `"Escape"`, `"Skills"` → `"Spells"`; add `spellsButtonEnabled` to BattleViewModel; wire to `AbilitySkillChangedEvent`
2. **Part 2** — Create `AbilityComponent`; create `AbilityTrees.kt` with sealed `AbilityEffect`, 3 nodes, and `ABILITY_TREES` registry; add all new events to `Events.kt`
3. **Part 3** — Load `AbilityComponent` unlocked IDs from prefs in `InitializeGameSystem`
4. **Part 4** — Create `AbilitySystem` to own unlock logic and `AbilityComponent` mutation; extend `StatSystem` to decrement point count only
5. **Part 5** — Build `AbilityViewModel` with `AbilityNodeUiState` computed flags, pending unlock tracking, and save/cancel state
6. **Part 6** — Build `AbilityView` — tree panel with node circles, connecting lines, visual states, character switcher stub, save/cancel confirm flow
7. **Part 7** — Build spell list panel in `BattleView` — scrollable rows, description area, faded unaffordable spells, ESC returns to action buttons
8. **Part 8** — Handle `CastSpellEvent` in `BattleSystem` — mana deduction, effect application, floating text, result message, turn gated on `SpellCastDismissedEvent`
9. **Part 9** — Seed 3 starting ability points in `InitializeGameSystem` for testing
10. **Part 10** — Verification pass: button labels, Spells enable/disable, tree node states, prerequisite enforcement, save/cancel flows, prefs persistence, spell list, cast effects, turn gating

---

## Key Files Reference

| File | Path |
|------|------|
| GameScreen | `core/src/main/kotlin/.../screens/GameScreen.kt` |
| BattleSystem | `core/src/main/kotlin/.../systems/BattleSystem.kt` |
| StatSystem | `core/src/main/kotlin/.../systems/StatSystem.kt` |
| InitializeGameSystem | `core/src/main/kotlin/.../systems/InitializeGameSystem.kt` |
| BattleView | `core/src/main/kotlin/.../ui/views/BattleView.kt` |
| Events | `core/src/main/kotlin/.../events/Events.kt` |
| **[NEW] AbilityComponent** | `core/src/main/kotlin/.../components/AbilityComponent.kt` |
| **[NEW] AbilityTrees** | `core/src/main/kotlin/.../configurations/AbilityTrees.kt` |
| **[NEW] AbilitySystem** | `core/src/main/kotlin/.../systems/AbilitySystem.kt` |
| **[REWORKED] AbilityViewModel** | `core/src/main/kotlin/.../ui/viewmodels/AbilityViewModel.kt` |
| **[REWORKED] AbilityView** | `core/src/main/kotlin/.../ui/views/AbilityView.kt` |

## Verification

1. `./gradlew :core:compileKotlin` — must pass after each part
