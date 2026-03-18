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
| `systems/DialogSystem.kt` | Drives dialog flow. Currently holds dialog data and `getDialog()` — both moved to `DialogConfigurations.kt` in this feature. |
| `systems/InteractionSystem.kt` | Already handles entity contact detection and routes to dialog/shop/loot. Unchanged in this feature. |
| `systems/InitializeGameSystem.kt` | Extended to load quest state from prefs on startup. |
| `systems/BattleSystem.kt` | Fires `EnemyKilledEvent` on enemy death — consumed by `QuestSystem` to update kill progress. |
| `components/DialogComponent.kt` | Simplified to hold only `dialogId: DialogId`. |
| `components/NonPlayerComponent.kt` | Extended with `dialogId: DialogId = DialogId.NO_DIALOG` field. |
| `systems/EntityCreationSystem.kt` | Extended to add `DialogComponent` when `config.dialogId != NO_DIALOG`. |
| `Dialog.kt` | DSL state machine for dialog flow. Refactored — all action functions fire events only, no side effects. |
| `ui/views/QuestView.kt` | Currently a stub. Fully implemented in this feature. |
| `ui/viewmodels/QuestViewModel.kt` | Currently a stub. Fully implemented in this feature. |
| `ui/views/MainGameView.kt` | Quest button added alongside existing HUD buttons. |
| `input/PlayerKeyboardInputProcessor.kt` | `Q` hotkey added to open `QuestView`. |
| `events/Events.kt` | All new dialog, quest, and kill events added here. |
| `maps/map_1.tmx` | `questman` entity added here with `dialogId = QUEST_MAN`. |

---

## Next Feature

# Dialog System Refactor and Quest System

## Context
- Dialog data and `DialogId` enum are extracted from `DialogSystem.kt` and `DialogComponent.kt` into a new `DialogConfigurations.kt` — `DialogSystem` becomes a pure state machine with no embedded data
- `Dialog.kt` action functions (`acceptQuest`, `endDialog`, etc.) fire events only — all side effects handled by the appropriate systems
- Quest state is tracked in a standalone `QuestSystem` singleton — not tied to any entity
- Quest conditions are checked automatically when kill events occur — `QuestView` updates reactively via `QuestStateChangedEvent`
- Quest completion requires returning to the quest giver — `CONDITIONS_MET` status is set automatically on kill, but `COMPLETED` is only set during the reward delivery dialog node
- Quest state persists to prefs as part of game data
- `QuestCondition` is a sealed class — future condition types (`CollectItem`, `ReachLocation`, `TalkToNPC`) are new subclasses, no structural changes needed
- Dialog flows in `DialogConfigurations.kt` require runtime access to `QuestSystem` for status-based branching — inject via a static accessor or world injection at the point the dialog is evaluated, not at config load time

---

## Part 1 — Extract Dialog Config to `DialogConfigurations.kt`

Create `configurations/DialogConfigurations.kt`:
- Move `DialogId` enum from `DialogComponent.kt` into this file — `NO_DIALOG` remains the default value
- Move all dialog flow definitions out of `DialogSystem.kt` (`getDialog()` function) into this file — each `DialogId` maps to a dialog flow built with the existing `Dialog.kt` DSL
- All future dialog flows are defined here — `DialogSystem.kt` contains no dialog data

Modify `systems/DialogSystem.kt`:
- Remove `getDialog()` function and all embedded dialog data
- Look up flows from `DialogConfigurations.kt` by `DialogId` at runtime
- No other behavioral changes — existing flow iteration logic unchanged

Modify `components/DialogComponent.kt`:
- Remove `DialogId` enum (now in `DialogConfigurations.kt`)
- Simplify to a single field: `dialogId: DialogId`

---

## Part 2 — Add `dialogId` to `NonPlayerConfiguration` and Wire Entity Creation

Modify `components/NonPlayerComponent.kt`:
- Add field to `NonPlayerConfiguration`: `dialogId: DialogId = DialogId.NO_DIALOG`

Modify `systems/EntityCreationSystem.kt`:
- When creating a `NON_PLAYER` entity: if `config.dialogId != DialogId.NO_DIALOG`, add `DialogComponent(config.dialogId)` to the entity
- Restores dialog component assignment removed during earlier refactor
- No changes to entity creation for other component types

---

## Part 3 — Refactor `Dialog.kt` Action Functions to Fire Events

Modify `Dialog.kt`:
- All action functions become thin event wrappers — no logic, no side effects:
  - `acceptQuest(questId: Int)` → fires `AcceptQuestEvent(questId)` on `gameStage`
  - `endDialog()` → fires `EndDialogEvent` on `gameStage`; `DialogSystem` handles cleanup
  - `completeQuest(questId: Int)` → fires `CompleteQuestEvent(questId)` on `gameStage`
  - `giveItem(itemId: Int)` → fires `DialogGiveItemEvent(itemId)` on `gameStage`
  - `healPlayer()` → fires `DialogHealPlayerEvent()` on `gameStage`
- Add all new events to `events/Events.kt`:
  - `class AcceptQuestEvent(val questId: Int) : Event()`
  - `class CompleteQuestEvent(val questId: Int) : Event()`
  - `class EndDialogEvent : Event()`
  - `class DialogGiveItemEvent(val itemId: Int) : Event()`
  - `class DialogHealPlayerEvent : Event()`
  - `class QuestStateChangedEvent(val questId: Int) : Event()`
  - `class QuestViewOpenEvent : Event()`
  - `class QuestViewClosedEvent : Event()`

---

## Part 4 — Update Dialog UI to Use New Drawables

Modify the dialog view/widget files:
- Replace old assets with drawables consistent with `InventoryView` and `BattleView` style
- Layout: two layered boxes
  - **Upper box**: dialog text — scales vertically with text length, does not clip
  - **Lower box**: dynamic option buttons — one button per option in the current node; button count changes as nodes progress
  - Button text scales to fit label length without clipping
- Keyboard navigation: Left/Right (or Up/Down) cycles between option buttons; Enter confirms focused option
- Mouse click on any button triggers that option
- All button actions delegate to the option's `action` lambda — no dialog logic in the view

---

## Part 5 — Create `QuestConfigurations.kt`

Create `configurations/QuestConfigurations.kt`:
- Sealed class `QuestCondition`:
  - `data class KillEnemy(val enemyType: EnemyType, val requiredCount: Int)`
  - Add new subclasses here for future condition types (`CollectItem`, `ReachLocation`, `TalkToNPC`)
- Data class `QuestReward`: `goldAmount: Int` — extensible for item rewards later
- Data class `QuestConfig`: `questId: Int`, `questName: String`, `questDescription: String`, `condition: QuestCondition`, `reward: QuestReward`
- Define `KILL_BLUE_SLIME_QUEST`:
  - `questId = 1`, `questName = "Slime Exterminator"`, `questDescription = "Kill 1 blue slime for the questman."`
  - `condition = KillEnemy(EnemyType.BLUE_SLIME, requiredCount = 1)`
  - `reward = QuestReward(goldAmount = 100)`
- Top-level registry: `val QUESTS: Map<Int, QuestConfig>` keyed by `questId`
- Include commented template for adding new quests:
  ```
  // QuestConfig(
  //     questId = 2,
  //     questName = "Quest Name",
  //     questDescription = "Quest description.",
  //     condition = QuestCondition.KillEnemy(EnemyType.GREEN_SLIME, requiredCount = 5),
  //     reward = QuestReward(goldAmount = 50)
  // ),
  ```

---

## Part 6 — Create `QuestSystem.kt`

Create `systems/QuestSystem.kt`:
- Extend `IntervalSystem()`, implement `EventListener`; register in `GameScreen.kt`
- Enum `QuestStatus { NOT_STARTED, ACTIVE, CONDITIONS_MET, COMPLETED }`
- Data class `QuestState(val questId: Int, var progress: Int = 0, var status: QuestStatus = NOT_STARTED)`
- Internal state: `questStates: MutableMap<Int, QuestState>` — loaded from prefs on startup (Part 8)
- Companion object pref key constants: `KEY_QUEST_STATES`
- Handle `AcceptQuestEvent(questId)`:
  - Add or update entry with `status = ACTIVE`, `progress = 0`
  - Save to prefs; fire `QuestStateChangedEvent(questId)`
- Handle `EnemyKilledEvent(enemyType)` (add to `Events.kt` if not already present — fired by `BattleSystem` on enemy death):
  - For each `ACTIVE` quest whose `condition` is `KillEnemy` matching `enemyType`: increment `progress`
  - If `progress >= requiredCount`: set `status = CONDITIONS_MET`
  - Save to prefs; fire `QuestStateChangedEvent(questId)` for each updated quest
- Handle `CompleteQuestEvent(questId)`:
  - Set `status = COMPLETED`
  - Look up `QuestReward` from `QUESTS` — apply `goldAmount` to `ResourceSystem.resources.gold`; call `resourceSystem.saveResources()`
  - Save to prefs; fire `QuestStateChangedEvent(questId)`
- Expose query method: `getState(questId: Int): QuestState` — used by dialog nodes to branch on status

---

## Part 7 — Add `questman` Entity and Full Quest Dialog Flow

Modify `maps/map_1.tmx`:
- Add `questman` entity with `NonPlayerConfiguration` field `dialogId = QUEST_MAN`

Add `DialogId.QUEST_MAN` to `DialogConfigurations.kt` and define the full dialog flow.
All 5 test scenarios are covered by status-based branching at Node 0:

```
val questManDialog = dialog(DialogId.QUEST_MAN.name) {

    // Entry node — branches based on current quest status
    node(0, "") {
        action = {
            when (questSystem.getState(1).status) {
                QuestStatus.NOT_STARTED  -> goToNode(1)
                QuestStatus.ACTIVE       -> goToNode(3)
                QuestStatus.CONDITIONS_MET -> goToNode(4)
                QuestStatus.COMPLETED    -> goToNode(5)
            }
        }
    }

    // Node 1 — Quest offer (NOT_STARTED)
    node(1, "Can you kill 1 blue slime for me?") {
        option("Accept") {
            action = {
                acceptQuest(1)
                goToNode(2)
            }
        }
        option("Decline") {
            action = { endDialog() }
        }
    }

    // Node 2 — Accepted confirmation
    node(2, "Great! Come back when it's done.") {
        option("Okay") {
            action = { endDialog() }
        }
    }

    // Node 3 — Quest in progress (ACTIVE)
    node(3, "You haven't finished yet — come back when the blue slime is defeated.") {
        option("Okay") {
            action = { endDialog() }
        }
    }

    // Node 4 — Conditions met, reward delivery (CONDITIONS_MET)
    node(4, "You did it! Here's your reward — 100 gold.") {
        option("Thanks") {
            action = {
                completeQuest(1)
                endDialog()
            }
        }
    }

    // Node 5 — Already completed (COMPLETED)
    node(5, "Thanks again for helping me!") {
        option("No problem") {
            action = { endDialog() }
        }
    }
}
```

Note: `questSystem` in the dialog DSL requires runtime injection — pass `QuestSystem` reference at the point the dialog is evaluated, not at config load time. The implementer should ensure `DialogConfigurations.kt` receives this reference via world injection or a passed context object when `getDialog()` is called.

---

## Part 8 — Load and Save Quest State via Prefs

Modify `systems/InitializeGameSystem.kt`:
- Inject `QuestSystem` via world (same pattern as `ResourceSystem`, `SettingsSystem`)
- On startup: check for `KEY_QUEST_STATES` in `"rolePlayingGamePrefs"` — if absent write empty defaults; if present deserialize and populate `QuestSystem.questStates`
- Use the existing `preferences` instance — do not open a second one

---

## Part 9 — Fire `EnemyKilledEvent` from `BattleSystem`

Modify `systems/BattleSystem.kt`:
- After enemy death (already handled for reward resolution in Feature 1): fire `EnemyKilledEvent(enemyType)` on `gameStage`
- If `EnemyKilledEvent` already exists from Feature 1, reuse it — do not create a duplicate
- Add `class EnemyKilledEvent(val enemyType: EnemyType) : Event()` to `Events.kt` if not present

---

## Part 10 — Build `QuestViewModel.kt`

Rework `ui/viewmodels/QuestViewModel.kt` (currently placeholder):
- Extend `PropertyChangeSource`, implement `EventListener`; register on `uiStage`
- Data class `QuestRowUiState`: `questId: Int`, `questName: String`, `progressText: String` (e.g. `"Slimes Killed: 0/1"`), `rewardText: String`, `isComplete: Boolean`
- Observable properties: `activeQuests by propertyNotify(emptyList<QuestRowUiState>())`, `completedQuests by propertyNotify(emptyList<QuestRowUiState>())`, `focusedQuest by propertyNotify<QuestRowUiState?>(null)`, `activePanelFocus by propertyNotify(QuestPanelFocus.ACTIVE)` — enum `QuestPanelFocus { ACTIVE, COMPLETED }`
- On `QuestViewOpenEvent` and `QuestStateChangedEvent`: rebuild both lists from `QuestSystem` state — active lists show `ACTIVE` and `CONDITIONS_MET` quests; completed list shows `COMPLETED` quests

---

## Part 11 — Build `QuestView.kt`

Rework `ui/views/QuestView.kt` (currently placeholder):
- Extend `Table(skin)`, mix `KTable`, `setFillParent(true)`
- Layout: two side-by-side boxes
  - **Active Quests** (left box): scrollable list of quest name rows; `CONDITIONS_MET` quests shown with a visual indicator (e.g. `"★ Ready to deliver"`)
  - **Completed Quests** (right box): scrollable list of completed quest name rows
  - Left/Right arrow keys switch `activePanelFocus` between boxes; Up/Down navigates rows within the focused box
- **Info panel** below both boxes — updates on focus change or hover:
  - Active quest: quest name, description, progress (e.g. `"Slimes Killed: 0/1"`), reward summary
  - Completed quest: quest name, final progress (e.g. `"Slimes Killed: 1/1"`), reward received
- Bind all elements to `QuestViewModel` via `model.onPropertyChange()`
- DSL extension function following existing view patterns

---

## Part 12 — Wire `QuestView` Access (Hotkey + Button)

Modify `input/PlayerKeyboardInputProcessor.kt`:
- Add hotkey (`Q`) to fire `QuestViewOpenEvent` on `uiStage` — same pattern as `I`, `K`, `J`

Modify `ui/views/MainGameView.kt`:
- Add Quest button alongside Inventory / Skill / Ability buttons — fires `QuestViewOpenEvent` on press

---

## Part 13 — Verification Pass (5 Test Scenarios + General)

Verify all 5 dialog test scenarios:
- **Test 1**: Init → talk to questman → Decline. Re-interact → quest offered again (status still `NOT_STARTED`)
- **Test 2**: Init → talk to questman → Accept. Quest appears in Active Quests list in `QuestView` (`progress = 0/1`)
- **Test 3**: After accepting → immediately re-interact with questman → `"You haven't finished yet"` node shown
- **Test 4**: Accept quest → kill blue slime → `QuestView` updates to show `CONDITIONS_MET` (`"★ Ready to deliver"`) → re-interact with questman → reward node shown → 100 gold added → quest moves to Completed list
- **Test 5**: After completion → re-interact with questman → `"Thanks again"` node shown, no quest options

Additional checks:
- Confirm `QuestStateChangedEvent` updates `QuestView` lists reactively without reopening the view
- Confirm quest progress and status persist correctly across game restart via prefs
- Confirm `EnemyKilledEvent` only increments progress for `ACTIVE` quests with matching enemy type
- Confirm dialog UI uses new drawables; option buttons are dynamic per node; button text does not clip
- Confirm `Dialog.kt` action functions fire events only — no direct side effects remain
- Confirm `DialogId` enum and all dialog flows live in `DialogConfigurations.kt` — none remain in `DialogSystem.kt` or `DialogComponent.kt`
- `./gradlew :core:compileKotlin` — must pass after each part

---

## Implementation Order

1. **Part 1** — Extract `DialogId` enum and all dialog flows to `DialogConfigurations.kt`; simplify `DialogComponent` to `dialogId` field only; remove data from `DialogSystem`
2. **Part 2** — Add `dialogId` field to `NonPlayerConfiguration`; restore `DialogComponent` assignment in `EntityCreationSystem`
3. **Part 3** — Refactor `Dialog.kt` action functions to fire events only; add all new events to `Events.kt`
4. **Part 4** — Update dialog UI to use new drawables; dynamic option buttons; keyboard and mouse navigation
5. **Part 5** — Create `QuestConfigurations.kt` with sealed `QuestCondition`, `QuestReward`, `QuestConfig`, and `KILL_BLUE_SLIME_QUEST`
6. **Part 6** — Create `QuestSystem` singleton — handles `AcceptQuestEvent`, `EnemyKilledEvent`, `CompleteQuestEvent`; exposes `getState()` query
7. **Part 7** — Add `questman` to `map_1.tmx`; define `QUEST_MAN` dialog flow in `DialogConfigurations.kt` covering all 5 test scenarios
8. **Part 8** — Load and save quest state via prefs in `InitializeGameSystem`
9. **Part 9** — Fire `EnemyKilledEvent` from `BattleSystem` on enemy death; reuse existing event if present
10. **Part 10** — Build `QuestViewModel` with active/completed list state and reactive `QuestStateChangedEvent` handling
11. **Part 11** — Build `QuestView` — two-panel layout, info panel, keyboard navigation between panels
12. **Part 12** — Wire `QuestView` to `Q` hotkey and MainGameView button
13. **Part 13** — Verification pass: all 5 test scenarios, reactive UI updates, prefs persistence, dialog UI, event purity in `Dialog.kt`

---

## Key Files Reference

| File | Path |
|------|------|
| GameScreen | `core/src/main/kotlin/.../screens/GameScreen.kt` |
| DialogSystem | `core/src/main/kotlin/.../systems/DialogSystem.kt` |
| InteractionSystem | `core/src/main/kotlin/.../systems/InteractionSystem.kt` |
| BattleSystem | `core/src/main/kotlin/.../systems/BattleSystem.kt` |
| InitializeGameSystem | `core/src/main/kotlin/.../systems/InitializeGameSystem.kt` |
| EntityCreationSystem | `core/src/main/kotlin/.../systems/EntityCreationSystem.kt` |
| Dialog | `core/src/main/kotlin/.../Dialog.kt` |
| DialogComponent | `core/src/main/kotlin/.../components/DialogComponent.kt` |
| NonPlayerComponent | `core/src/main/kotlin/.../components/NonPlayerComponent.kt` |
| PlayerKeyboardInputProcessor | `core/src/main/kotlin/.../input/PlayerKeyboardInputProcessor.kt` |
| MainGameView | `core/src/main/kotlin/.../ui/views/MainGameView.kt` |
| Events | `core/src/main/kotlin/.../events/Events.kt` |
| map_1.tmx | `assets/maps/map_1.tmx` |
| **[NEW] DialogConfigurations** | `core/src/main/kotlin/.../configurations/DialogConfigurations.kt` |
| **[NEW] QuestConfigurations** | `core/src/main/kotlin/.../configurations/QuestConfigurations.kt` |
| **[NEW] QuestSystem** | `core/src/main/kotlin/.../systems/QuestSystem.kt` |
| **[REWORKED] QuestViewModel** | `core/src/main/kotlin/.../ui/viewmodels/QuestViewModel.kt` |
| **[REWORKED] QuestView** | `core/src/main/kotlin/.../ui/views/QuestView.kt` |

## Verification

1. `./gradlew :core:compileKotlin` — must pass after each part
