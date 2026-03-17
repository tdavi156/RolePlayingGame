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
| `systems/InteractionSystem.kt` | Handles entity interaction detection and routing. To be fully reworked — directional hitbox → circular radius, clean player/NPC separation, typed component dispatch. |
| `systems/InitializeGameSystem.kt` | Handles game startup — loads prefs, initializes systems. |
| `systems/InventorySystem.kt` | Singleton inventory source of truth. `addItem()` and `removeItem()` used by shop buy/sell. |
| `systems/ResourceSystem.kt` | Holds `Resources` (gold). Gold debited/credited on buy/sell; `saveResources()` called after each transaction. |
| `components/ItemComponent.kt` | `InventoryComponent` on PlayerEntity holds equipped item IDs per slot — used by sell guard to count equipped copies. |
| `input/PlayerKeyboardInputProcessor.kt` | All keyboard input routing. Shop navigation and ESC rollback handled here when `ShopView` is active. |
| `events/Events.kt` | Central event definitions. All new shop events added here. |
| `maps/map_1_house_1.tmx` | Map containing the oldman NPC entity. `ShopComponent` (shopId) added to this entity. |

---

## Next Feature

# Shops — Buying and Selling Items

## Context
- Shop inventory is infinite — purchasing never reduces stock
- Shop opens directly into BUY mode — no prior Buy/Sell/Leave selection screen
- BUY and SELL share a single `ShopViewModel` and `ShopView`, toggled via a mode flag
- `ShopComponent` holds only a `shopId` — full item lists are defined in `ShopConfigs.kt` and looked up at runtime
- Sell price is always `ceil(goldValue / 2f)` — half the buy price, rounded up
- Items currently equipped across any party member cannot be sold — available sell quantity = `ownedQuantity - equippedCount`
- ESC always rolls back one context level: quantity selector → item list → close shop
- Leave option is always present as the last row in every tab in both BUY and SELL modes — no state where the player is trapped

---

## Part 1 — Create `ShopConfigs.kt` and `ShopComponent`

Create `configurations/ShopConfigs.kt`:
- Data class `ShopConfig`: `shopId: Int`, `shopName: String`, typed item ID lists per tab: `equipmentIds: List<Int>`, `consumableIds: List<Int>`, `questIds: List<Int>`, `enchantmentIds: List<Int>`
- Define `OLDMAN_SHOP` as the first entry — populate with a reasonable spread of existing item IDs from each config for testing
- Include a commented template block for adding new shops:
  ```
  // ShopConfig(
  //     shopId = 1,
  //     shopName = "Shop Name",
  //     equipmentIds = listOf(1001, 1002),
  //     consumableIds = listOf(2001),
  //     questIds = emptyList(),
  //     enchantmentIds = emptyList()
  // ),
  ```

Create `components/ShopComponent.kt`:
- Data class / component holding only `shopId: Int`
- Add `ShopComponent` with the appropriate `shopId` to the oldman entity in `map_1_house_1.tmx`

---

## Part 2 — Full Rework of `InteractionSystem.kt`

Rework `systems/InteractionSystem.kt`:
- **Detection**: replace directional hitbox check with circular radius check around both the player entity and the NPC entity — interaction triggers if the two radii overlap
- **Player/NPC identification**: unambiguously determine which entity is the player and which is the NPC on every `InteractionEvent` — no directional assumptions
- **Typed dispatch**: after contact confirmed, inspect the NPC entity for component presence and route accordingly:
  - `LootComponent` → existing loot handling (behavior unchanged)
  - `DialogComponent` → existing `DialogSystem` (behavior unchanged)
  - `ShopComponent` → fire `ShopInteractionEvent(shopId)` to new `ShopSystem`
- Pause overworld mechanics (player movement, game tick systems) on interaction start; resume on `ShopClosedEvent`, `DialogClosedEvent`, or equivalent
- All existing interaction types (Loot, Dialog) must continue to function correctly after rework

---

## Part 3 — Create `ShopSystem.kt`

Create `systems/ShopSystem.kt`:
- Extend `IntervalSystem()`, implement `EventListener`; register in `GameScreen.kt`
- Enum `ShopMode { BUY, SELL }` defined here or in a shared enums file
- Handles `ShopInteractionEvent(shopId: Int)`: looks up `ShopConfig` from `ShopConfigs.kt`, stores as `activeShopConfig`, fires `ShopOpenEvent(shopConfig)`
- Transient session state: `activeShopConfig`, `shopMode`, `pendingItemId`, `pendingQuantity` — all cleared on shop close
- Handles `ShopBuyConfirmedEvent(itemId: Int, quantity: Int)`:
  - Deduct `goldValue * quantity` from `ResourceSystem.resources.gold`; call `resourceSystem.saveResources()`
  - Call `inventorySystem.addItem(itemId, quantity)` — stacking handled automatically
- Handles `ShopSellConfirmedEvent(itemId: Int, quantity: Int)`:
  - Calculate sell price: `ceil(goldValue / 2f) * quantity`
  - Add result to `ResourceSystem.resources.gold`; call `resourceSystem.saveResources()`
  - Call `inventorySystem.removeItem(itemId, quantity)`
- Handles `ShopClosedEvent`: clears all session state, fires event to resume overworld mechanics

---

## Part 4 — Build `ShopViewModel.kt`

Create `ui/viewmodels/ShopViewModel.kt`:
- Extend `PropertyChangeSource`, implement `EventListener`; register on `uiStage`
- Enum `ShopTab { EQUIPMENT, CONSUMABLES, QUEST_ITEMS, ENCHANTMENTS }`
- Observable properties: `shopMode by propertyNotify(ShopMode.BUY)`, `activeTab by propertyNotify(ShopTab.EQUIPMENT)`, `focusedItemIndex by propertyNotify(0)`, `pendingItemId by propertyNotify<Int?>(null)`, `pendingQuantity by propertyNotify(1)`, `insufficientGoldVisible by propertyNotify(false)`
- On `ShopOpenEvent`: store config, reset all properties to defaults (mode = BUY, tab = EQUIPMENT, focus = 0)
- **Buy mode computed accessors**: return items from `activeShopConfig` filtered by `activeTab`; annotate each with affordability flag (`goldValue <= ResourceSystem.resources.gold`)
- **Sell mode computed accessors**: return items from `InventorySystem` filtered by `activeTab`; for each item compute:
  - `availableQty = ownedQuantity - equippedCountAcrossAllCharacters` (via `InventoryComponent` equipped map)
  - `sellable = isSellable && availableQty > 0`
  - Items with `availableQty == 0` due to all copies being equipped are treated as unsellable (faded, unselectable) even if `isSellable = true`
- Quantity selector ceiling: BUY mode → clamped by `floor(playerGold / goldValue)`; SELL mode → clamped by `availableQty`

---

## Part 5 — Build `ShopView.kt`

Create `ui/views/ShopView.kt` as a non-fullscreen right-side overlay (overworld remains visible behind it):
- **Top bar**: BUY / SELL toggle (highlighted based on `shopMode`) + current gold display (e.g. `Gold: 500g`) — mirrors "Money" box in reference image
- **Tab bar**: Equipment / Consumables / Quest Items / Enchantments — same style as inventory right panel; Left/Right cycles tabs
- **Item list**: scrollable rows, Up/Down moves focus
  - Buy mode row: `[icon] Item Name -------- 50g` — unaffordable items rendered faded; selectable for description only
  - Sell mode row: `[icon] Item Name --- x2 --- 25g` — unsellable or fully-equipped items faded, no price shown, unselectable
  - Last entry in every tab, every mode: **Leave** — always selectable, always present
- **Description panel**: fixed at bottom of panel, updates on focus change or mouse hover — never moves, not a floating tooltip
- **Quantity selector**: appears inline on the focused row after item is selected (Enter/double-click); Left/Right adjusts `pendingQuantity` within computed ceiling; Enter confirms; ESC cancels back to item list
- **Insufficient gold popup**: small transient overlay message shown when player attempts to select an unaffordable item — auto-dismisses after ~1.5s, interaction continues
- Binds all elements to `ShopViewModel` via `model.onPropertyChange()`
- DSL extension function following existing view patterns

---

## Part 6 — Keyboard Navigation

Modify `input/PlayerKeyboardInputProcessor.kt`:

When `ShopView` is active and **no item pending**:
- **Left / Right**: cycle `model.activeTab`; reset `focusedItemIndex` to 0
- **Up / Down**: move `model.focusedItemIndex` (clamped; Leave is always last)
- **Tab** (or designated key): toggle `model.shopMode` between BUY and SELL; reset focus to 0
- **Enter**:
  - Leave row → fire `ShopClosedEvent`
  - Unaffordable item (buy) → set `insufficientGoldVisible = true`
  - Unsellable / fully-equipped item (sell) → no-op
  - Valid item → set `model.pendingItemId`, `model.pendingQuantity = 1`, show quantity selector
- **Esc**: fire `ShopClosedEvent`

When **quantity selector is active**:
- **Left / Right**: adjust `model.pendingQuantity` (clamped by ceiling)
- **Enter**: fire `ShopBuyConfirmedEvent` or `ShopSellConfirmedEvent`; clear pending state; return to item list
- **Esc**: clear `model.pendingItemId`; return to item list (no purchase/sale made)

---

## Part 7 — Mouse Interaction

Modify `ShopView.kt`:
- Hovering a row updates `model.focusedItemIndex` and refreshes description panel immediately
- Clicking a row sets focus
- Clicking an unaffordable item (buy) sets `insufficientGoldVisible = true`
- Clicking an unsellable / fully-equipped item (sell) → no-op
- Double-clicking a valid item sets `model.pendingItemId` and shows quantity selector (same as Enter)
- BUY / SELL toggle buttons clickable — sets `model.shopMode`, resets focus
- Tab headers clickable — sets `model.activeTab`, resets focus
- Clicking Leave row fires `ShopClosedEvent`

---

## Part 8 — Equipped Item Sell Guard

Extend sell-side computed accessors in `ShopViewModel`:
- For each equipment item in `InventorySystem`, count how many copies are currently equipped across all characters by inspecting the `InventoryComponent` equipped ID map per party member
- `availableQty = ownedQuantity - equippedCount`
- If `availableQty == 0`: render faded and unselectable — treated identically to `isSellable = false` in the UI, even though the item data itself is sellable
- Quantity selector ceiling in sell mode = `availableQty` (never allows selling equipped copies)
- Edge case — all copies equipped across multiple characters (e.g. 3x boots, 3 characters each wearing one): `availableQty = 0`, item shown faded and unselectable

---

## Part 9 — Session Cleanup and Leave Handling

In `ShopSystem.kt`:
- On `ShopClosedEvent`: clear `activeShopConfig`, `pendingItemId`, `pendingQuantity`, reset `shopMode` to BUY
- Fire event to `GameScreen` (or appropriate system) to re-enable player movement and overworld input processors
- Ensure ESC rollback is consistent at every depth:
  - Quantity selector active → clear pending, return to item list
  - Item list active → fire `ShopClosedEvent`, close shop, resume overworld
- Leave row always fires `ShopClosedEvent` regardless of current mode or tab

---

## Part 10 — Wire New Shop Events to `Events.kt`

Add to `events/Events.kt`:
- `class ShopInteractionEvent(val shopId: Int) : Event()`
- `class ShopOpenEvent(val shopConfig: ShopConfig) : Event()`
- `class ShopClosedEvent : Event()`
- `class ShopBuyConfirmedEvent(val itemId: Int, val quantity: Int) : Event()`
- `class ShopSellConfirmedEvent(val itemId: Int, val quantity: Int) : Event()`

---

## Part 11 — Verification Pass

- Confirm circular radius interaction detection fires correctly for oldman in `map_1_house_1.tmx`
- Confirm existing Dialog and Loot interactions still work correctly after `InteractionSystem` rework
- Confirm BUY: gold deducted at full `goldValue * quantity`; item added to inventory with correct stacking
- Confirm SELL: gold added at `ceil(goldValue / 2f) * quantity`; item quantity decremented correctly; entry removed at 0
- Confirm unaffordable items: faded in list, insufficient gold popup shown, no purchase made
- Confirm unsellable items: faded in list, unselectable, no price displayed
- Confirm equipped item sell guard: available sell quantity correctly excludes equipped copies across all party members
- Confirm ESC rollback at each depth: quantity selector → item list → shop closed
- Confirm Leave option present in every tab, every mode, always selectable
- Confirm gold persists correctly across shop close via `ResourceSystem.saveResources()`
- `./gradlew :core:compileKotlin` — must pass after each part

---

## Implementation Order

1. **Part 1** — Create `ShopConfigs.kt` with `OLDMAN_SHOP` config and commented template; create `ShopComponent`; add to oldman entity in `map_1_house_1.tmx`
2. **Part 2** — Full rework of `InteractionSystem` — circular radius detection, clean player/NPC identification, typed component dispatch for Loot/Dialog/Shop
3. **Part 3** — Create `ShopSystem` — handles `ShopInteractionEvent`, session state, `ShopBuyConfirmedEvent`, `ShopSellConfirmedEvent`, `ShopClosedEvent`
4. **Part 4** — Create `ShopViewModel` — mode flag, tab, focus, pending item/quantity, buy/sell computed accessors with affordability and sell guard logic
5. **Part 5** — Build `ShopView` — right-side overlay, top bar (mode toggle + gold), tab bar, item list with pricing/fading rules, description panel, quantity selector, insufficient gold popup
6. **Part 6** — Implement keyboard navigation — tab cycling, item focus, mode toggle, Enter/ESC context rollback at each depth
7. **Part 7** — Implement mouse interaction — hover, click, double-click, mode toggle, tab click, Leave click
8. **Part 8** — Implement equipped item sell guard in `ShopViewModel` computed accessors — subtract equipped count per party member, clamp sell quantity ceiling
9. **Part 9** — Wire session cleanup and Leave handling — `ShopClosedEvent` clears state, resumes overworld, ESC rollback consistent at all depths
10. **Part 10** — Add all new shop events to `Events.kt`
11. **Part 11** — Verification pass: interaction detection, existing interaction types, buy/sell correctness, all edge cases, ESC rollback, Leave presence, gold persistence

---

## Key Files Reference

| File | Path |
|------|------|
| GameScreen | `core/src/main/kotlin/.../screens/GameScreen.kt` |
| InteractionSystem | `core/src/main/kotlin/.../systems/InteractionSystem.kt` |
| InitializeGameSystem | `core/src/main/kotlin/.../systems/InitializeGameSystem.kt` |
| InventorySystem | `core/src/main/kotlin/.../systems/InventorySystem.kt` |
| ResourceSystem | `core/src/main/kotlin/.../systems/ResourceSystem.kt` |
| ItemComponent / InventoryComponent | `core/src/main/kotlin/.../components/ItemComponent.kt` |
| PlayerKeyboardInputProcessor | `core/src/main/kotlin/.../input/PlayerKeyboardInputProcessor.kt` |
| Events | `core/src/main/kotlin/.../events/Events.kt` |
| map_1_house_1.tmx | `assets/maps/map_1_house_1.tmx` |
| **[NEW] ShopConfigs** | `core/src/main/kotlin/.../configurations/ShopConfigs.kt` |
| **[NEW] ShopComponent** | `core/src/main/kotlin/.../components/ShopComponent.kt` |
| **[NEW] ShopSystem** | `core/src/main/kotlin/.../systems/ShopSystem.kt` |
| **[NEW] ShopViewModel** | `core/src/main/kotlin/.../ui/viewmodels/ShopViewModel.kt` |
| **[NEW] ShopView** | `core/src/main/kotlin/.../ui/views/ShopView.kt` |

## Verification

1. `./gradlew :core:compileKotlin` — must pass after each part
