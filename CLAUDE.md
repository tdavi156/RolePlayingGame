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

# Feature 26 — Dead Code Cleanup: Orphaned Files and Commented Debris

A pass over the architecture to remove code that is no longer referenced anywhere in the codebase — orphaned files from replaced systems, commented-out dead blocks, and test/debug artifacts. All removals confirmed via grep to have zero live references. Items that appear "unused" but are intentional placeholders for future roadmap features are explicitly preserved.

---

## Intentionally Preserved (NOT cleaned up)

These appear dormant but are scoped for future development:

- **`AttackComponent`** — Referenced by `AiEntity`/`AiSystem` for future complex battle AI behavior (per Feature 23 decision)
- **`MapView` structure** — The M key opens this view; kept as the skeleton for a future map display feature. Only the test label inside it is removed (see Part 4)
- **`BattleCompositions` id=5 "boss comp placeholder"** — Intentional boss encounter slot
- **`InventoryLeftPanel` quest items stub section** — Intentionally skeletal; waiting on quest item feature expansion
- **`AbilityView` geometric placeholder icons** — Code comments describing circles/rectangles used as icon proxies until real sprite assets exist; not dead code

---

## What Gets Deleted

| File | Reason |
|------|--------|
| `screens/InventoryScreen.kt` | Dev/test harness screen — creates its own isolated mini-world, never referenced by any other file, never navigated to from `GameScreen` |
| `ui/widgets/InventoryDragSourceTarget.kt` | Old drag-and-drop system from before Feature 12 overhaul — "preserved as generic UI utility" per roadmap but never imported anywhere after decoupling |
| `ui/widgets/InventorySlot.kt` | Only referenced by `InventoryDragSourceTarget.kt` (deleted above); zero live references after that deletion |
| `ui/viewmodels/ItemModel.kt` | ViewModel for the old drag-and-drop inventory — only referenced by `InventorySlot.kt` and `InventoryDragSourceTarget.kt`, both deleted |
| `quest/Quests.kt` | Contains `Quest` sealed interface and `KillQuest` with `TODO("Not yet implemented")` — never imported by any file; the real quest system uses `QuestConfigurations.kt` and `QuestSystem.kt` exclusively |
| `ui/widgets/CharacterInfo.kt` | Compact portrait+HP+mana bar widget — never instantiated anywhere; `CharacterInfoView` has its own inline stat labels |

---

## What Gets Modified

### Part 1 — `ui/views/MainGameView.kt`
Remove the large block of commented-out tooltip infrastructure (~40 lines total):
- 5 commented-out field declarations (`characterInfoToolTipLabel`, `inventoryToolTipLabel`, `skillToolTipLabel`, `questToolTipLabel`, `mapToolTipLabel`) at lines ~56–60
- The entire commented-out `stack { }` / `row()` block that builds tooltip labels at lines ~75–108
- All commented-out tooltip visibility/positioning calls scattered through the hover listeners (~12 additional lines)

### Part 2 — `ui/Skin.kt`
Remove the `TEST_LABEL` enum entry and its skin registration (only ever referenced by the commented-out tooltip code above):
- Remove `TEST_LABEL;` from the `Labels` enum
- Remove the `// test label` comment and the 3-line `label(Labels.TEST_LABEL.skinKey) { ... }` block from `loadLabels()`

### Part 3 — `ui/views/MapView.kt`
Remove the hardcoded test label (the view skeleton, `MapViewModel`, and DSL extension function are all kept):
- Remove the `table { label("test label on Map view", ...) }` block from the `init` block
- Remove the now-unused `label` import

---

## Key Files Reference

| File | Path |
|------|------|
| MainGameView | `core/src/main/kotlin/.../ui/views/MainGameView.kt` |
| Skin | `core/src/main/kotlin/.../ui/Skin.kt` |
| MapView | `core/src/main/kotlin/.../ui/views/MapView.kt` |

## Verification

1. `./gradlew :core:compileKotlin` — must pass after each part
