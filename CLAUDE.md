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
| `systems/BattleSystem.kt` | Core battle driver. Extended significantly — comp resolution, escrow, dynamic turn order, enemy selection. |
| `systems/StatSystem.kt` | Fires `CombatSpeedChangedEvent` when speed is modified mid-combat — triggers turn order recalc. |
| `systems/QuestSystem.kt` | Already handles `EnemyKilledEvent` from Feature 8 — no changes needed, fires per individual kill. |
| `systems/ResourceSystem.kt` | Gold released from escrow to `ResourceSystem` on full victory only. |
| `systems/InventorySystem.kt` | Pending loot released to inventory on full victory only. |
| `ui/views/BattleView.kt` | Enemy selection indicator wired here; reward screen extended for multi-enemy summary. |
| `components/StatComponent.kt` | Already has `speed` field — used for turn order sorting. |
| `events/Events.kt` | `CombatSpeedChangedEvent` added here. |
| `maps/map_1.tmx` | Existing spawners updated with optional `battleCompId` field. |
| `maps/map_1_house_1.tmx` | Existing spawners updated with optional `battleCompId` field. |

---

## Next Feature

# Multiple Enemy Battles

## Context
- Battle composition is resolved via a **hybrid approach**: spawners with a non-null `battleCompId` use a fixed `BattleComp`; spawners with `battleCompId = null` trigger a random roll from `RANDOM_COMPS` for the encountered enemy type
- Random comps may include mixed enemy types — not limited to the trigger enemy type
- **EXP** is awarded immediately on each individual enemy kill — never held in escrow
- **Gold and loot** are held in escrow until full victory — discarded entirely on player defeat
- This fully prevents the boss item farming exploit: killing the boss then intentionally losing discards all pending gold and loot; EXP already granted remains
- Turn order is speed-based and **dynamically recalculated** whenever any entity's speed changes mid-combat — recalc takes effect from the next turn, never displacing the currently acting entity
- Enemy selection uses a world-space arrow/highlight indicator positioned over the entity using its `PhysicsComponent` — not a UI element
- Last selected enemy is remembered between turns; auto-advances to next living enemy if the previously selected enemy died
- `EnemyKilledEvent` fires per individual kill mid-combat — quest progress updates in real time

---

## Part 1 — Create `BattleCompositions.kt` Config

Create `configurations/BattleCompositions.kt`:
- Data class `BattleComp`: `id: Int`, `unit1: EnemyType`, `unit2: EnemyType? = null`, `unit3: EnemyType? = null`
  - `unit1` is always required; `unit2` and `unit3` are optional — order determines spawn position in battle
- Define named fixed compositions for testing:
  - `id=1`: single green slime
  - `id=2`: 2 green slimes
  - `id=3`: 3 green slimes
  - `id=4`: 2 green slimes + 1 blue slime (mixed)
  - `id=5`: boss comp placeholder (1 blue slime as stand-in until a boss enemy type exists)
- Define random roll tables per `EnemyType`:
  ```
  val RANDOM_COMPS: Map<EnemyType, List<BattleComp>>
  // e.g. EnemyType.GREEN_SLIME -> listOf(comp of 1, comp of 2, comp of 3)
  // e.g. EnemyType.BLUE_SLIME  -> listOf(comp of 1 blue, comp of 1 blue + 1 green, comp of 2 blue)
  ```
- Top-level fixed registry: `val BATTLE_COMPS: Map<Int, BattleComp>` keyed by `id`
- Include commented template for adding new comps:
  ```
  // BattleComp(
  //     id = 6,
  //     unit1 = EnemyType.GREEN_SLIME,
  //     unit2 = EnemyType.BLUE_SLIME,
  //     unit3 = null
  // ),
  ```

---

## Part 2 — Add `battleCompId` to Spawner and Enemy Config

Modify the enemy/spawner configuration data class:
- Add `battleCompId: Int? = null`
  - `null` = random roll from `RANDOM_COMPS[enemyType]`
  - Non-null = fixed lookup from `BATTLE_COMPS[battleCompId]`

Modify existing spawner objects in `map_1.tmx` and `map_1_house_1.tmx`:
- Leave all standard enemy spawners with `battleCompId` absent (defaults to `null`) — random rolls apply
- No spawner needs a fixed comp yet; field is present for future boss/blocker use

---

## Part 3 — Battle Composition Resolution in `BattleSystem`

Modify `systems/BattleSystem.kt`:
- On battle trigger: read `battleCompId` from the encountered entity's config
- If `battleCompId != null`: look up fixed `BattleComp` from `BATTLE_COMPS`
- If `battleCompId == null`: randomly select one `BattleComp` from `RANDOM_COMPS[enemyType]` — uniform random selection across the list
- Create battle entities for each non-null unit slot in the resolved comp
- Store the resolved `BattleComp` as combat session state — used by escrow (Part 5), turn order (Part 6), and reward screen (Part 8)

---

## Part 4 — Quest Kill Progress Per Individual Kill

Modify `systems/BattleSystem.kt`:
- Fire `EnemyKilledEvent(enemyType)` immediately on each individual enemy death — not deferred to end of combat
- `QuestSystem` already consumes this event from Feature 8 — no changes to `QuestSystem`
- Quest progress increments mid-combat; `QuestStateChangedEvent` fires in real time
- `QuestView` reflects updated progress after combat ends (or if opened mid-overworld after combat)

---

## Part 5 — Reward Escrow System

Modify `systems/BattleSystem.kt`:
- **EXP**: awarded immediately on each enemy death via `StatSystem` — never held
- **Gold and loot**: accumulated in session-scoped escrow per kill:
  - `pendingGold: Int` — sum of `goldReward` from each killed enemy
  - `pendingLoot: MutableList<ItemData?>` — loot roll result per kill (null = no drop), held but not added to inventory
- On **full victory** (all enemies defeated):
  - Release `pendingGold` to `ResourceSystem.resources.gold`; call `resourceSystem.saveResources()`
  - Release each non-null entry in `pendingLoot` to `InventorySystem` via `addItem()`
  - Fire combined `BattleRewardEvent` with total EXP (already granted), total gold, and loot list for reward screen display
- On **player defeat**:
  - Discard `pendingGold` and `pendingLoot` entirely — no gold or items granted
  - EXP already awarded per kill is not revoked
- Clear all escrow state (`pendingGold = 0`, `pendingLoot.clear()`) on combat end regardless of outcome

---

## Part 6 — Speed-Based Dynamic Turn Order

Modify `systems/BattleSystem.kt`:
- At combat start: build initial turn order by sorting all living combat entities by `statComponent.speed` descending; ties broken randomly (`Random.nextBoolean()`)
- Add `CombatSpeedChangedEvent(entity: Entity)` to `events/Events.kt`
- Modify `systems/StatSystem.kt`: fire `CombatSpeedChangedEvent(entity)` on `gameStage` any time `speed` is modified on an entity during an active combat session
- `BattleSystem` handles `CombatSpeedChangedEvent`: rebuild the full turn order list immediately by re-sorting all living entities by current speed
  - The entity whose turn is **currently active** is not displaced — new order takes effect from the next turn
  - Rebuilt list maintains correct relative ordering for all waiting entities
- Dead entities removed from the turn order list immediately on death
- Turn order list cleared on combat end

---

## Part 7 — Enemy Selection UI — Arrow/Highlight Indicator

Modify `ui/views/BattleView.kt` and BattleViewModel:
- Add `selectedEnemyIndex: Int` to BattleViewModel — persists between player turns
- Add `enemySelectionActive by propertyNotify(false)` — true when player is choosing a target

**Selection trigger**:
- Player selects Attack or a Spell:
  - 1 living enemy → auto-confirm, no selection UI shown
  - 2+ living enemies → set `enemySelectionActive = true`, default to `selectedEnemyIndex` (last selected, or 0 if first turn)

**Indicator rendering**:
- When `enemySelectionActive == true`: render an arrow or highlight indicator in the game world over the currently selected enemy entity
- Position derived from the enemy entity's `PhysicsComponent` world-space coordinates projected to screen — same pattern as floating text positioning
- Indicator updates as `selectedEnemyIndex` changes

**Keyboard navigation in selection mode**:
- Right / Down: cycle `selectedEnemyIndex` forward through living enemies (wraps)
- Left / Up: cycle backward (wraps)
- If only 2 enemies: any arrow key swaps to the other
- Enter or mouse click on an enemy entity: confirm selection, fire action event, set `enemySelectionActive = false`
- ESC: cancel selection, return to action button panel, do not consume turn

**Post-action default**:
- After the turn resolves: if `selectedEnemyIndex` points to a now-dead enemy, advance to the next living enemy index automatically
- This becomes the default selection for the player's next turn

---

## Part 8 — Multi-Enemy Reward Screen

Modify the reward screen overlay in `ui/views/BattleView.kt`:
- Extend `BattleRewardEvent` to carry a list of per-enemy rewards rather than a single reward
- Reward screen displays a combined summary:
  - Total EXP gained (sum of all per-kill EXP — shown for reference even though already granted)
  - Total gold gained (from escrow release)
  - Item drop rows — one row per non-null loot entry: icon + item name; omitted entirely if no drops
- Layout scales dynamically: 0, 1, 2, or 3 item rows depending on drops
- Single confirm/dismiss button — same `RewardDismissedEvent` pattern as Feature 1

---

## Part 9 — Verification Pass

- Confirm hybrid comp resolution: non-null `battleCompId` always uses the fixed comp; null triggers random roll from correct enemy type pool
- Confirm mixed-type random comps spawn correct entity types in the correct positional slots
- Confirm `EnemyKilledEvent` fires per individual kill mid-combat — quest progress increments in real time
- Confirm EXP awarded immediately on kill; gold and loot held in escrow
- Confirm boss exploit prevention: player defeat after killing one enemy discards all pending gold and loot; EXP already granted is retained
- Confirm speed-based turn order at combat start
- Confirm dynamic turn order recalc on `CombatSpeedChangedEvent`: currently acting entity not displaced; new order takes effect next turn
- Confirm enemy selection: single enemy auto-selects; 2+ enemies show indicator; Right/Down and Left/Up cycle correctly; last selection remembered; dead enemy auto-advances to next living
- Confirm ESC in selection mode returns to action buttons without consuming turn
- Confirm reward screen shows correct combined EXP, gold, and all loot drops from all kills
- Confirm reward screen layout scales correctly for 0, 1, 2, and 3 item drops
- `./gradlew :core:compileKotlin` — must pass after each part

---

## Implementation Order

1. **Part 1** — Create `BattleCompositions.kt` with `BattleComp` data class, fixed comp registry, and random comp tables per enemy type
2. **Part 2** — Add `battleCompId: Int? = null` to spawner/enemy config; update existing spawners in `.tmx` files
3. **Part 3** — Resolve battle comp in `BattleSystem` on combat trigger — fixed lookup or random roll; create entities per slot
4. **Part 4** — Fire `EnemyKilledEvent` per individual kill in `BattleSystem`; confirm `QuestSystem` updates in real time
5. **Part 5** — Implement escrow: EXP awarded immediately; gold and loot held until full victory; discarded on defeat; cleared on combat end
6. **Part 6** — Implement speed-based turn order at combat start; add `CombatSpeedChangedEvent`; `StatSystem` fires it on speed change; `BattleSystem` recalcs immediately without displacing active entity
7. **Part 7** — Implement enemy selection mode: `selectedEnemyIndex` persistence, world-space indicator, keyboard cycling, auto-advance on death, ESC cancel
8. **Part 8** — Extend reward screen for multi-enemy summary: combined EXP/gold display, dynamic item drop rows (0–3)
9. **Part 9** — Verification pass: comp resolution, kill events, escrow correctness, exploit prevention, turn order, dynamic recalc, enemy selection, reward screen

---

## Key Files Reference

| File | Path |
|------|------|
| GameScreen | `core/src/main/kotlin/.../screens/GameScreen.kt` |
| BattleSystem | `core/src/main/kotlin/.../systems/BattleSystem.kt` |
| StatSystem | `core/src/main/kotlin/.../systems/StatSystem.kt` |
| QuestSystem | `core/src/main/kotlin/.../systems/QuestSystem.kt` |
| ResourceSystem | `core/src/main/kotlin/.../systems/ResourceSystem.kt` |
| InventorySystem | `core/src/main/kotlin/.../systems/InventorySystem.kt` |
| BattleView | `core/src/main/kotlin/.../ui/views/BattleView.kt` |
| StatComponent | `core/src/main/kotlin/.../components/StatComponent.kt` |
| Events | `core/src/main/kotlin/.../events/Events.kt` |
| map_1.tmx | `assets/maps/map_1.tmx` |
| map_1_house_1.tmx | `assets/maps/map_1_house_1.tmx` |
| **[NEW] BattleCompositions** | `core/src/main/kotlin/.../configurations/BattleCompositions.kt` |

## Verification

1. `./gradlew :core:compileKotlin` — must pass after each part
