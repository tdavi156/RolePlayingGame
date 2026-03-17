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
| `systems/BattleSystem.kt` | Drives battle flow, turn order, and animations. Items button currently disabled — to be enabled here. Turn advancement gated on result message dismissal after item use. |
| `systems/StatSystem.kt` | Handles stat recalculation and current value modification. Extended here to expose a full-stat check function. |
| `systems/InventorySystem.kt` | Singleton inventory source of truth. `removeItem()` called on successful consumable use. |
| `ui/views/BattleView.kt` | Battle UI layer. Items button wired here to fire `CombatInventoryOpenEvent`. |
| `ui/views/InventoryView.kt` | Existing inventory UI. Reused in combat with tab locked to Consumables via `isCombatMode` flag. |
| `ui/viewmodels/InventoryViewModel.kt` | Shared ViewModel for inventory. Extended with `isCombatMode` flag and combat-specific open/close event handling. |
| `ui/widgets/InventoryLeftPanel.kt` | Character list panel. Extended to show result messages and full-stat warnings inline. |
| `configurations/ConsumableItems.kt` | Consumable item config. Extended with `flashColor: Color` field per item. |
| `events/Events.kt` | Central event definitions. New combat inventory and flash events added here. |

---

## Next Feature

# Consumable Items in Combat

## Context
- The Items button in `BattleView` is already present but disabled — this feature enables it
- The existing `InventoryView` and `InventoryViewModel` are reused directly in combat, locked to the Consumables tab via an `isCombatMode` flag — no separate combat view is created
- `UseConsumableEvent` is shared between overworld and combat; an `isCombatItemUse: Boolean` flag (default `false`) distinguishes the two contexts for turn-advancement purposes
- Stat application logic is identical in both contexts — only turn advancement differs
- Full stat restriction (HP/mana already full) is checked after the player confirms a character, not before — character appears selectable, warning shown on confirm
- Turn does not advance until the player dismisses the result message in combat
- Flash animation reuses the existing hit-flash mechanism with a per-item color defined in `ConsumableItems.kt`

---

## Part 1 — Add `flashColor` to `ConsumableItemData`

Modify `configurations/ConsumableItems.kt`:
- Add `flashColor: Color` field to `ConsumableItemData` (LibGDX `Color`)
- Update all existing consumable entries with appropriate colors:
  - Health-restoring items → `Color.GREEN`
  - Mana-restoring items → `Color.BLUE`
- Update any existing construction sites of `ConsumableItemData` to include the new field
- Compilation must pass after this step

---

## Part 2 — Add Combat Lock Flag to `InventoryViewModel`

Modify `ui/viewmodels/InventoryViewModel.kt`:
- Add `isCombatMode by propertyNotify(false)` observable property
- When `isCombatMode == true`:
  - Force `activeTab = InventoryTab.CONSUMABLES` on open and prevent tab changes — Left/Right key cycling and tab header clicks are no-ops
  - Non-consumable tabs rendered faded and unselectable in the tab bar
- Add to `events/Events.kt`:
  - `class CombatInventoryOpenEvent : Event()`
  - `class CombatInventoryClosedEvent : Event()`
  - `class CombatItemUseDismissedEvent : Event()` — fired when result message is dismissed in combat
  - `class ItemUseFlashEvent(val characterIndex: Int, val flashColor: Color) : Event()`
- On `CombatInventoryOpenEvent`: set `isCombatMode = true`, reset `focusedItemIndex` to 0, set `activeTab = CONSUMABLES`
- On `CombatInventoryClosedEvent`: set `isCombatMode = false`
- Existing `InventoryOpenEvent` behavior unchanged — overworld flow unaffected

---

## Part 3 — Enable Items Button in `BattleView` and Wire to Inventory

Modify `ui/views/BattleView.kt` and its ViewModel:
- Enable the currently-disabled Items button
- On Items button press (keyboard or mouse): fire `CombatInventoryOpenEvent` on `uiStage`
- `InventoryView` overlays on top of the battle UI — same layout as overworld, tab bar locked to Consumables
- ESC or cancel from within the inventory while `isCombatMode == true`: fire `CombatInventoryClosedEvent`, return focus to battle action buttons — player's turn is NOT consumed

---

## Part 4 — Update `UseConsumableEvent` with Combat Flag

Modify `events/Events.kt`:
- Add `isCombatItemUse: Boolean = false` parameter to `UseConsumableEvent`
- Default is `false` — all existing overworld usages are unaffected without changes
- `InventoryViewModel` sets `isCombatItemUse = true` when firing the event from combat context (i.e. when `isCombatMode == true`)
- `BattleSystem` inspects this flag to determine whether to gate turn advancement on result message dismissal

---

## Part 5 — Extend `StatSystem` with Full Stat Check

Modify `systems/StatSystem.kt`:
- Add `isStatFull(characterIndex: Int, statType: ConsumableStatType): Boolean`
  - Returns `true` if the relevant current stat equals its maximum: `currentHealth == maxHealth` for `HEALTH`, `currentMana == maxMana` for `MANA`
- Called by `InventoryViewModel` after item use is confirmed on a character, before applying any stat delta
- Used identically in both combat and overworld contexts

---

## Part 6 — Result Message, Full Stat Warning, and Turn Gating

Modify `ui/viewmodels/InventoryViewModel.kt` and `ui/widgets/InventoryLeftPanel.kt`:

**After `UseConsumableEvent` is confirmed on a character:**
- Call `StatSystem.isStatFull()` for the item's `statType`
- If **stat is full**:
  - Show warning message in left panel: `"[CharacterName] is already at full [HP/MP]!"`
  - Do NOT apply stat delta; do NOT remove item from inventory
  - Return focus to character list for re-selection — turn is NOT consumed in combat
- If **stat is not full**:
  - Apply stat delta via `StatSystem`
  - Call `inventorySystem.removeItem(itemId, 1)` — removes entry if quantity reaches 0
  - Fire `ItemUseFlashEvent(characterIndex, item.flashColor)` on `gameStage`
  - Show result message in left panel: `"[CharacterName] recovered [value] HP!"` or equivalent
  - Result message requires player input (Enter or click) to dismiss
  - On dismiss in **combat**: fire `CombatItemUseDismissedEvent` on `gameStage`; close inventory overlay; `BattleSystem` then advances the turn
  - On dismiss in **overworld**: return focus to item list — no turn consumption

Modify `systems/BattleSystem.kt`:
- Listen for `CombatItemUseDismissedEvent` — advance turn to the next entity (same flow as after an attack action)
- Do NOT advance turn on `UseConsumableEvent` directly — always wait for dismissal

---

## Part 7 — Item Use Flash Animation

Modify the system handling the existing hit-flash effect (whichever system drives the white flash on hit):
- Handle `ItemUseFlashEvent(characterIndex, flashColor)`: tint the target character entity to `flashColor` for `0.2s`, then restore original color
- Reuse the same tint/restore action sequence as the existing hit-flash — only the color and duration differ
- Flash plays in both combat and overworld contexts — fired identically from `InventoryViewModel` in both cases
- `FLASH_DURATION = 0.2f` — define as a constant alongside existing flash duration constants, or reuse if already at the same value

---

## Part 8 — Verification Pass

- Confirm Items button is enabled in battle and opens `InventoryView` locked to Consumables tab
- Confirm Left/Right tab cycling and tab header clicks are disabled in combat mode; non-consumable tabs are faded
- Confirm ESC from combat inventory closes without consuming the player's turn
- Confirm full stat warning: message shown, item not consumed, focus returns to character list, turn not consumed
- Confirm successful use: stat delta applied, inventory quantity decremented (entry removed at 0), result message shown, flash animation plays in correct color
- Confirm turn advances only after result message is dismissed via `CombatItemUseDismissedEvent`
- Confirm overworld item use unchanged: no `isCombatItemUse` flag set, no turn gating, result message dismisses back to item list
- `./gradlew :core:compileKotlin` — must pass after each part

---

## Implementation Order

1. **Part 1** — Add `flashColor: Color` to `ConsumableItemData`; update all existing entries with appropriate colors
2. **Part 2** — Add `isCombatMode` flag to `InventoryViewModel`; add `CombatInventoryOpenEvent`, `CombatInventoryClosedEvent`, `CombatItemUseDismissedEvent`, `ItemUseFlashEvent` to `Events.kt`; wire tab-lock behavior
3. **Part 3** — Enable Items button in `BattleView`; fire `CombatInventoryOpenEvent` on press; wire ESC to close without consuming turn
4. **Part 4** — Add `isCombatItemUse: Boolean = false` to `UseConsumableEvent`; set to `true` when fired from combat context
5. **Part 5** — Add `isStatFull()` check function to `StatSystem` for `HEALTH` and `MANA` stat types
6. **Part 6** — Wire full stat warning and result message in `InventoryViewModel` and `InventoryLeftPanel`; gate combat turn advancement on `CombatItemUseDismissedEvent` in `BattleSystem`
7. **Part 7** — Implement `ItemUseFlashEvent` handler using existing hit-flash mechanism with per-item `flashColor` and `0.2s` duration
8. **Part 8** — Verification pass: combat lock, ESC behavior, full stat warning, successful use flow, turn gating, overworld unchanged, compilation after each part

---

## Key Files Reference

| File | Path |
|------|------|
| GameScreen | `core/src/main/kotlin/.../screens/GameScreen.kt` |
| BattleSystem | `core/src/main/kotlin/.../systems/BattleSystem.kt` |
| StatSystem | `core/src/main/kotlin/.../systems/StatSystem.kt` |
| InventorySystem | `core/src/main/kotlin/.../systems/InventorySystem.kt` |
| BattleView | `core/src/main/kotlin/.../ui/views/BattleView.kt` |
| InventoryView | `core/src/main/kotlin/.../ui/views/InventoryView.kt` |
| InventoryViewModel | `core/src/main/kotlin/.../ui/viewmodels/InventoryViewModel.kt` |
| InventoryLeftPanel | `core/src/main/kotlin/.../ui/widgets/InventoryLeftPanel.kt` |
| ConsumableItems | `core/src/main/kotlin/.../configurations/ConsumableItems.kt` |
| Events | `core/src/main/kotlin/.../events/Events.kt` |

## Verification

1. `./gradlew :core:compileKotlin` — must pass after each part
