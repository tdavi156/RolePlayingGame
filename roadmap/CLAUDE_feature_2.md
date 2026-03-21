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
| `systems/InitializeGameSystem.kt` | Handles game startup — loads prefs, initializes systems. Pattern for loading/writing defaults. |
| `systems/StatSystem.kt` | Handles stat recalculation for player entities. Currently recalculates equipment-derived stats. Will be extended to support current HP/MP modification from consumables. |
| `components/ItemComponent.kt` | Currently holds equipped item data per entity. To be reworked — will only hold equipped item references by ID. |
| `ui/views/InventoryView.kt` | Existing inventory UI using drag-and-drop. Will be fully replaced by the new tabbed inventory view. |
| `input/PlayerKeyboardInputProcessor.kt` | Handles all keyboard input routing. Inventory open (`I` key) and in-inventory navigation handled here. |
| `ui/views/MainGameView.kt` | Contains the Inventory button that opens `InventoryView`. Will be rewired to new view. |
| `events/Events.kt` | Central event definitions. New inventory events added here. |

---

## Next Feature

# Inventory Rework — UI, Item Types, and Functionality

## Context
- The existing inventory is a drag-and-drop grid system — it has bugs, is hard to extend, and will be fully replaced
- Drag-and-drop infrastructure is preserved as a generic UI utility but formally decoupled from all inventory logic
- The new inventory is a tabbed, text-based list UI inspired by the Pokémon Bag layout: right side shows tabs + item list + info panel, left side shows context-sensitive character/item info
- Inventory is account-scoped, not entity-scoped — `InventorySystem` is a standalone singleton (like `ResourceSystem`), not a component on `PlayerEntity`
- Items stack by unique integer ID — same ID = same item, quantity tracked alongside. ID ranges are documented per category in comment blocks
- `InventoryComponent` on `PlayerEntity` is retained but reduced to holding only currently equipped item IDs
- Left-side context switches automatically when an item action (Equip/Use) is initiated; Q/E manual context switching is deferred to a future feature

---

## Part 1 — Rename Item Config, Create Stat Type Enums, Add Missing Fields

Rename `configurations/Items.kt` → `configurations/EquipmentItems.kt`:
- Rename `ItemData` → `EquipmentItemData`, `StatType` → `EquipmentStatType`
- Add fields: `id: Int`, `isSellable: Boolean = true`, `goldValue: Int = 10`
- ID range for equipment: `1000–1999` — document in a comment block at the top of the file:
  ```
  // ID Ranges:
  // Equipment:           1000–1999
  // Consumables:         2000–2999
  // Quest Items:         3000–3999
  // Battle Enchantments: 4000–4999
  ```
- Assign IDs to all existing equipment entries

Create `configurations/EquipmentStatType.kt`:
- Enum `EquipmentStatType` — move existing stat type values here from old `StatType`

Create `configurations/ConsumableStatType.kt`:
- Enum `ConsumableStatType` with values relevant to consumable effects: `HEALTH`, `MANA` (expand later as needed)

Create `configurations/BattleEnchantmentStatType.kt`:
- Enum `BattleEnchantmentStatType` with values for passive combat bonuses: `HEALTH`, `ATTACK`, `DEFENSE` (expand later as needed)

Update all existing references to `ItemData` and `StatType` to use the new renamed types — compilation must pass.

---

## Part 2 — Create `ConsumableItems.kt`, `QuestItems.kt`, `BattleEnchantmentItems.kt`

Create `configurations/ConsumableItems.kt`:
- Data class `ConsumableItemData`: `id: Int`, `itemName: String`, `uiAtlasKey: String`, `statType: ConsumableStatType`, `statValue: Int`, `goldValue: Int = 10`, `isSellable: Boolean = true`
- 3 dummy entries (IDs `2001–2003`), all using `"armor"` atlas key
- Commented add-item template at top of list

Create `configurations/QuestItems.kt`:
- Data class `QuestItemData`: `id: Int`, `itemName: String`, `uiAtlasKey: String`, `itemDescription: String`, `questId: Int = 0`, `isSellable: Boolean = false`
- 3 dummy entries (IDs `3001–3003`), all using `"boots"` atlas key
- Commented add-item template at top of list

Create `configurations/BattleEnchantmentItems.kt`:
- Data class `BattleEnchantmentItemData`: `id: Int`, `itemName: String`, `uiAtlasKey: String`, `itemDescription: String`, `statType: BattleEnchantmentStatType`, `statValue: Int`, `goldValue: Int = 0`, `isSellable: Boolean = false`
- 3 dummy entries (IDs `4001–4003`), all using `"sword2"` atlas key
- Commented add-item template at top of list

---

## Part 3 — Formally Decouple Drag-and-Drop from Inventory

Modify the existing drag-and-drop system:
- Remove all references to inventory slots, `InventoryComponent`, and item equipping from drag-and-drop classes
- Preserve all drag-and-drop infrastructure as a generic UI utility — no functional removal, just inventory-specific wiring stripped out
- Confirm drag-and-drop still compiles and does not throw at runtime after decoupling

---

## Part 4 — Create `InventorySystem.kt` as Singleton

Create `systems/InventorySystem.kt`:
- Extend `IntervalSystem()`, implement `EventListener`; register in `GameScreen.kt`
- Internal state — four typed lists, each entry is a data class pairing item data with quantity:
  - `equipment: MutableList<InventoryEntry<EquipmentItemData>>`
  - `consumables: MutableList<InventoryEntry<ConsumableItemData>>`
  - `questItems: MutableList<InventoryEntry<QuestItemData>>`
  - `enchantments: MutableList<InventoryEntry<BattleEnchantmentItemData>>`
- `InventoryEntry<T>(val item: T, var quantity: Int)` — defined as a small data class in the same file
- Stacking logic on `addItem()`: check if an entry with the same `id` exists — if so, increment `quantity`; otherwise add a new entry with `quantity = 1`
- Expose: `addItem()`, `removeItem()` (decrements quantity, removes entry at 0), `getEquippedIds(): Map<ItemCategory, Int?>` for `InventoryComponent` sync

---

## Part 5 — Rework `InventoryComponent` and Seed Starting Items

Modify `components/ItemComponent.kt` / `InventoryComponent`:
- Reduce to holding only currently equipped item IDs per slot: `Map<ItemCategory, Int?>` — one nullable ID per category
- Remove all owned-item list logic — `InventorySystem` is now the source of truth

Modify `systems/InitializeGameSystem.kt`:
- After existing init logic, seed starting inventory via `inventorySystem.addItem()`:
  - 5 equipment items (varied, from existing `EquipmentItems`)
  - 2 consumables (from dummy `ConsumableItems`)
  - 2 quest items (from dummy `QuestItems`)
  - 2 battle enchantments (from dummy `BattleEnchantmentItems`)

---

## Part 6 — Build `InventoryViewModel.kt`

Create `ui/viewmodels/InventoryViewModel.kt`:
- Extend `PropertyChangeSource`, implement `EventListener`; register on `uiStage`
- Enums defined here or in a shared file: `InventoryTab { EQUIPMENT, CONSUMABLES, QUEST_ITEMS, ENCHANTMENTS }`, `InventoryContext { LEFT, RIGHT }`
- Observable properties: `activeTab by propertyNotify(InventoryTab.EQUIPMENT)`, `focusedItemIndex by propertyNotify(0)`, `activeContext by propertyNotify(InventoryContext.RIGHT)`, `focusedCharacterIndex by propertyNotify(0)`, `pendingActionItem by propertyNotify<Any?>(null)` (holds item awaiting character selection)
- Computed accessors return the current tab's item list from `InventorySystem`
- On `InventoryOpenEvent`: reset `activeTab`, `focusedItemIndex`, `activeContext` to defaults

---

## Part 7 — Build Right-Side Panel (`InventoryRightPanel`)

Create `ui/widgets/InventoryRightPanel.kt`:
- **Top section** — tab bar: Equipment / Consumables / Quest Items / Battle Enchantments. Active tab highlighted. Left/Right arrow keys cycle tabs via `model.activeTab`.
- **Middle section** — scrollable dynamic item list. Each row: `uiAtlasKey` icon + item name + quantity badge if `quantity > 1`. Focused row highlighted. Up/Down arrow keys move `model.focusedItemIndex` (clamped to list size).
- **Bottom section** — item info display. Updates on focus change (keyboard or mouse hover). Never moves — fixed position, not a tooltip.
  - Equipment: raw stats list (`EquipmentStatType` → value pairs)
  - Consumable: stat type + value (e.g. "Restores 20 HP")
  - Quest item: `itemDescription` + `questId` stub
  - Enchantment: `itemDescription` + stat type/value
- Binds all sections to `InventoryViewModel` via `model.onPropertyChange()`

---

## Part 8 — Build Left-Side Context Panel (`InventoryLeftPanel`)

Create `ui/widgets/InventoryLeftPanel.kt`:
- Display changes based on `model.activeTab`:
  - **Equipment / Consumables**: character list. Each row shows character name, portrait placeholder, and HP bar. Focused row highlighted when `model.activeContext == LEFT`. Layout mirrors the left panel in the reference image.
  - **Quest Items**: stubbed display strings — `Quest ID: ----`, `Quest Name: ----`, `Quest Description: ----`, `Quest Progress: ----`
  - **Enchantments**: item name, description, stat type, and stat value from the focused enchantment entry
- Binds to `InventoryViewModel` via `model.onPropertyChange()`

---

## Part 9 — Assemble `InventoryView.kt`

Replace existing `ui/views/InventoryView.kt`:
- Full-screen `Table`, `setFillParent(true)`
- Composes `InventoryLeftPanel` (left) and `InventoryRightPanel` (right) side by side
- Both panels share the same `InventoryViewModel` instance
- DSL extension function following existing view patterns
- Rewire `PlayerKeyboardInputProcessor.kt` (`I` key) and `MainGameView.kt` (Inventory button) to open this new view

---

## Part 10 — Keyboard Navigation

Modify `input/PlayerKeyboardInputProcessor.kt`:
- When `InventoryView` is active and `model.activeContext == RIGHT`:
  - **Left / Right**: cycle `model.activeTab`, reset `model.focusedItemIndex` to 0
  - **Up / Down**: move `model.focusedItemIndex` within current list (clamped)
  - **Enter**: if item focused, trigger item action (see Part 11)
- When `model.activeContext == LEFT`:
  - **Up / Down**: move `model.focusedCharacterIndex` (clamped to party size)
  - **Enter**: confirm action on focused character (see Part 12)
  - **Esc / B**: cancel — clear `pendingActionItem`, return `activeContext` to `RIGHT`
- **Esc** when `activeContext == RIGHT`: close inventory, fire `InventoryClosedEvent`

---

## Part 11 — Item Action Context (Equip / Use / Cancel)

Modify `InventoryViewModel` and `InventoryRightPanel`:
- On Enter with a focused item in the right panel:
  - Equipment: show inline action options `Equip` / `Cancel` on the focused row
  - Consumables: show inline action options `Use` / `Cancel` on the focused row
  - Quest Items / Enchantments: no action options — Enter is a no-op on these tabs
- On `Equip` or `Use` selected: set `model.pendingActionItem` to the focused item, switch `model.activeContext` to `LEFT` — keyboard focus moves to the character list
- On `Cancel`: clear `model.pendingActionItem`, return `activeContext` to `RIGHT`

---

## Part 12 — Wire Actions to `StatSystem` and `InventorySystem`

Add event handlers (in `InventoryViewModel` or a dedicated handler):

**`EquipItemEvent(itemId: Int, characterIndex: Int)`**:
- Update `InventoryComponent` equipped slot for the target character with the new item ID
- Trigger `StatSystem` stat recalculation (existing pattern) — removes old item's `EquipmentStatType` bonuses, applies new item's bonuses
- Clear `model.pendingActionItem`, return `activeContext` to `RIGHT`

**`UseConsumableEvent(itemId: Int, characterIndex: Int)`**:
- Look up `ConsumableItemData` by ID from `ConsumableItems`
- Apply `statValue` delta to the target character's current stat via `StatSystem` — this requires a pass on `StatSystem` to support direct modification of transient current values (e.g. `currentHealth`, `currentMana`) separately from equipment-derived recalculation
- Call `inventorySystem.removeItem(itemId)` — decrements quantity, removes entry at 0
- Clear `model.pendingActionItem`, return `activeContext` to `RIGHT`

Add both events to `events/Events.kt`.

---

## Part 13 — Mouse Interaction

Modify `InventoryRightPanel`:
- Hovering an item row updates `model.focusedItemIndex` (info panel updates immediately)
- Clicking a row sets focus; double-clicking triggers the same action as Enter
- Tab headers are clickable — clicking sets `model.activeTab` and resets `focusedItemIndex`

Modify `InventoryLeftPanel`:
- Clicking a character row sets `model.focusedCharacterIndex`
- If `model.pendingActionItem != null`, clicking a character row also confirms the pending action (fires `EquipItemEvent` or `UseConsumableEvent`)

---

## Part 14 — Verification Pass

- Confirm drag-and-drop compiles and functions with no inventory references remaining
- Confirm stacking: adding two items with the same ID increments quantity, not list size
- Confirm equipped items are reflected correctly in `StatSystem` after equip action
- Confirm consumable use modifies `currentHealth`/`currentMana` and removes from inventory at quantity 0
- Confirm all 4 item type lists populate correctly from seeded starting items
- `./gradlew :core:compileKotlin` — must pass after each part

---

## Implementation Order

1. **Part 1** — Rename `Items.kt` → `EquipmentItems.kt`, rename types, add `id`/`isSellable`/`goldValue` fields, create `EquipmentStatType`, `ConsumableStatType`, `BattleEnchantmentStatType` enums in separate files
2. **Part 2** — Create `ConsumableItems.kt`, `QuestItems.kt`, `BattleEnchantmentItems.kt` with data classes and dummy entries per spec
3. **Part 3** — Formally decouple drag-and-drop from inventory — strip inventory-specific wiring, preserve generic infrastructure
4. **Part 4** — Create `InventorySystem` singleton with typed item lists, stacking logic, and `addItem`/`removeItem` methods; register in `GameScreen`
5. **Part 5** — Reduce `InventoryComponent` to equipped item IDs only; seed 11 starting items in `InitializeGameSystem` for testing
6. **Part 6** — Create `InventoryViewModel` with tab, focus, context, and pending action observable properties
7. **Part 7** — Build `InventoryRightPanel` widget — tab bar, dynamic item list with quantity badges, fixed item info panel at bottom
8. **Part 8** — Build `InventoryLeftPanel` widget — character list for equipment/consumable tabs, stubbed displays for quest/enchantment tabs
9. **Part 9** — Assemble new `InventoryView` from both panels; rewire `PlayerKeyboardInputProcessor` and `MainGameView` to use it
10. **Part 10** — Implement keyboard navigation — tab cycling, item focus, context switching, escape handling
11. **Part 11** — Implement item action context — inline Equip/Use/Cancel options on focused item, context switch to left panel on confirm
12. **Part 12** — Wire `EquipItemEvent` to `StatSystem` for equipment recalc; wire `UseConsumableEvent` to `StatSystem` for transient current-value modification; extend `StatSystem` to support `currentHealth`/`currentMana` deltas
13. **Part 13** — Implement mouse interaction — hover updates info panel, click sets focus, double-click triggers action, character row click confirms pending action
14. **Part 14** — Verification pass: drag-and-drop decoupling, stacking, stat recalc, consumable use, list population, compilation after each part

---

## Key Files Reference

| File | Path |
|------|------|
| GameScreen | `core/src/main/kotlin/.../screens/GameScreen.kt` |
| InitializeGameSystem | `core/src/main/kotlin/.../systems/InitializeGameSystem.kt` |
| StatSystem | `core/src/main/kotlin/.../systems/StatSystem.kt` |
| ItemComponent / InventoryComponent | `core/src/main/kotlin/.../components/ItemComponent.kt` |
| PlayerKeyboardInputProcessor | `core/src/main/kotlin/.../input/PlayerKeyboardInputProcessor.kt` |
| MainGameView | `core/src/main/kotlin/.../ui/views/MainGameView.kt` |
| Events | `core/src/main/kotlin/.../events/Events.kt` |
| **[RENAMED] EquipmentItems** | `core/src/main/kotlin/.../configurations/EquipmentItems.kt` |
| **[NEW] EquipmentStatType** | `core/src/main/kotlin/.../configurations/EquipmentStatType.kt` |
| **[NEW] ConsumableStatType** | `core/src/main/kotlin/.../configurations/ConsumableStatType.kt` |
| **[NEW] BattleEnchantmentStatType** | `core/src/main/kotlin/.../configurations/BattleEnchantmentStatType.kt` |
| **[NEW] ConsumableItems** | `core/src/main/kotlin/.../configurations/ConsumableItems.kt` |
| **[NEW] QuestItems** | `core/src/main/kotlin/.../configurations/QuestItems.kt` |
| **[NEW] BattleEnchantmentItems** | `core/src/main/kotlin/.../configurations/BattleEnchantmentItems.kt` |
| **[NEW] InventorySystem** | `core/src/main/kotlin/.../systems/InventorySystem.kt` |
| **[NEW] InventoryViewModel** | `core/src/main/kotlin/.../ui/viewmodels/InventoryViewModel.kt` |
| **[NEW] InventoryRightPanel** | `core/src/main/kotlin/.../ui/widgets/InventoryRightPanel.kt` |
| **[NEW] InventoryLeftPanel** | `core/src/main/kotlin/.../ui/widgets/InventoryLeftPanel.kt` |
| **[REPLACED] InventoryView** | `core/src/main/kotlin/.../ui/views/InventoryView.kt` |

## Verification

1. `./gradlew :core:compileKotlin` — must pass after each part
