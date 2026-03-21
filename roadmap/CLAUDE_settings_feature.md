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
| `ui/views/MenuView.kt` | Contains the UI elements to access Settings |
| `systems/AudioSystem.kt` | Handles all audio, music and effects via events. When an event is handled, add a sound to the queue, and the soundSystem checks the queue on each tick |
| `systems/BattleSystem.kt` | Contains events for Battle actions and animations. When an animation would need to be played in battle, it uses the SLIDE_DURATION in the companion object on line 619. This will need to account for the settings. |

---

## Next Feature

# Implement Settings UI and functionality

## Context
Settings Data Class (not an entity/component):

Loaded from prefs on game start (defaults if first launch), written to prefs only on Save
Other systems read from this class directly at runtime, never from prefs
---

## Part 1 — Settings Data Class

Create `configurations/Settings.kt`:
- Data class `Settings` with fields: `masterVolume: Int = 100`, `musicVolume: Int = 100`, `effectsVolume: Int = 100`, `combatAnimationSpeed: CombatAnimationSpeed = CombatAnimationSpeed.NORMAL`, `autoClearText: Boolean = false`
- Enum class `CombatAnimationSpeed { NORMAL, FAST, OFF }` in the same file
- Define companion object constants for pref keys: `KEY_MASTER_VOLUME`, `KEY_MUSIC_VOLUME`, `KEY_EFFECTS_VOLUME`, `KEY_COMBAT_ANIMATION_SPEED`, `KEY_AUTO_CLEAR_TEXT` — used in both Part 3 and Part 8 to avoid string duplication

---

## Part 2 — SettingsSystem

Create `systems/SettingsSystem.kt`:
- Extend `IntervalSystem()`, implement `EventListener`
- Hold `var settings = Settings()` as the single runtime source of truth
- No prefs access here — prefs are loaded in Part 3 and written in Part 8
- Register `SettingsSystem` in `GameScreen.kt` alongside the other systems so it is available globally via the `World` injection

---

## Part 3 — Load Settings Prefs on Startup

Modify `systems/InitializeGameSystem.kt`:
- Access `SettingsSystem` via world injection (same pattern as other systems are accessed)
- After the existing game init logic, add a settings prefs block: if the key `KEY_MASTER_VOLUME` is absent from `preferences`, write all `Settings` defaults to prefs using `preferences.flush { ... }` (KTX); otherwise read each key and assign to `settingsSystem.settings`
- Use the same `"rolePlayingGamePrefs"` preferences instance already present in this file — do not open a second one

---

## Part 4 — Build the SettingsView UI

Create `ui/viewmodels/SettingsViewModel.kt`:
- Extend `PropertyChangeSource`, implement `EventListener`
- Observable properties: `masterVolume by propertyNotify(100)`, `musicVolume by propertyNotify(100)`, `effectsVolume by propertyNotify(100)`, `combatAnimationSpeed by propertyNotify(CombatAnimationSpeed.NORMAL)`, `autoClearText by propertyNotify(false)`, `focusedRow by propertyNotify(0)`
- Register on `uiStage` (not `gameStage`) as listener — settings is a UI-layer concern

Create `ui/views/SettingsView.kt`:
- Extend `Table(skin)`, mix `KTable`, call `setFillParent(true)`
- Layout: vertically stacked rows, each row is a labeled sub-box containing the control
  - Rows 0-2: Master / Music / Effects volume — displayed as a numeric percentage with Left/Right arrow indicators; snaps to 10% increments (0–100)
  - Row 3: Combat Animation Speed — three inline options (Normal / Fast / Off); active option highlighted
  - Row 4: Auto Clear Text — two inline options (On / Off); active option highlighted
  - Row 5: Save and Cancel buttons side-by-side
- Highlight the currently focused row visually (e.g., tinted background or border drawable)
- Bind all controls to `SettingsViewModel` properties via `model.onPropertyChange()`
- Add a DSL extension function `KWidget<S>.settingsView(model, skin, init)` following the existing `dialogView` pattern

---

## Part 5 — Wire SettingsView to Reflect Current Settings on Open

Modify `ui/viewmodels/SettingsViewModel.kt`:
- Add a new event `SettingsOpenEvent : Event()` in `Events.kt`
- In `MenuView.kt`, fire `SettingsOpenEvent` (on `uiStage`) when the Settings button is clicked and show the `SettingsView`
- `SettingsViewModel.handle()` listens for `SettingsOpenEvent`: copies all fields from `settingsSystem.settings` into the ViewModel's observable properties and resets `focusedRow` to 0
- This ensures the view always opens reflecting saved state, never stale UI state

---

## Part 6 — Keyboard Navigation

Modify `input/PlayerKeyboardInputProcessor.kt`:
- When `SettingsView` is active (track via a flag set on open/close), route key events through settings logic instead of game movement
- **Up / Down**: decrement / increment `focusedRow` (clamped 0–5), fire `SettingsFocusChangedEvent(row)` or directly update ViewModel
- **Left / Right**:
  - Rows 0-2: adjust volume by ±10, clamped 0–100
  - Row 3: cycle `CombatAnimationSpeed` left/right through [NORMAL, FAST, OFF]
  - Row 4: toggle `autoClearText`
  - Row 5: move focus between Save and Cancel (treat as two sub-columns)
- **Enter**: on rows 0-4, no special action (value already changed); on row 5, confirm the focused button (Save or Cancel)
- **Esc**: same as Cancel — close without saving

---

## Part 7 — Mouse Interaction

Modify `ui/views/SettingsView.kt`:
- Each row sub-box adds a `ClickListener`: on click, set `model.focusedRow` to that row's index
- Volume rows: clicking the left arrow decrements by 10, right arrow increments by 10
- Radio option buttons (rows 3-4): clicking an option sets that value immediately
- Save / Cancel buttons: standard `ChangeListener` triggering save/cancel logic
- Mouse movement re-takes highlight context: each row adds a `MouseEnterListener` (or `InputListener.enter`) that updates `model.focusedRow` when the cursor enters the row — this overrides keyboard focus

---

## Part 8 — Wire Save

In `SettingsViewModel` or a `SettingsSaveEvent` handler:
- On Save confirmed: copy all ViewModel property values into `settingsSystem.settings`
- Write to prefs: open the same `"rolePlayingGamePrefs"` instance and `preferences.flush { set each Settings key to its value }` using the key constants from Part 1
- Fire a `SettingsClosedEvent : Event()` (add to `Events.kt`) to signal `MenuView` to hide `SettingsView`
- Do not reset ViewModel properties — they remain in sync with the now-saved settings

---

## Part 9 — Wire Cancel / Esc

In `SettingsViewModel`:
- On Cancel / Esc: fire `SettingsClosedEvent` only — do NOT modify `settingsSystem.settings`, do NOT write to prefs
- `MenuView` listens for `SettingsClosedEvent` and hides the `SettingsView`
- The next time `SettingsView` opens, Part 5 logic re-reads from `settingsSystem.settings`, discarding any unsaved UI changes

---

## Part 10 — Volume Calculation in AudioSystem

Modify `systems/AudioSystem.kt`:
- Inject `SettingsSystem` via world (same pattern other systems use)
- Replace the hardcoded `it.play(1f)` on line 31 with a computed volume:
  - Effects channel: `(settingsSystem.settings.masterVolume / 100f) * (settingsSystem.settings.effectsVolume / 100f)`
  - Music channel: `(settingsSystem.settings.masterVolume / 100f) * (settingsSystem.settings.musicVolume / 100f)`
- Apply the computed volume to each sound/music `play()` call in the queue
- No caching of volume — compute fresh each tick so changes apply immediately without restart

---

## Part 11 — Combat Animation Speed in BattleSystem

Modify `systems/BattleSystem.kt`:
- Inject `SettingsSystem` via world
- Replace the hardcoded `SLIDE_DURATION` usages with a computed duration at the point each animation sequence is assembled:
  - `CombatAnimationSpeed.NORMAL`: multiply `SLIDE_DURATION` by 2 (slower, more cinematic)
  - `CombatAnimationSpeed.FAST`: use `SLIDE_DURATION` as-is (current speed)
  - `CombatAnimationSpeed.OFF`: skip all `moveTo`/`delay`/`flash` actions entirely — jump directly to the result state with duration `0f`
- Keep `SLIDE_DURATION` and other constants unchanged in the companion object — treat them as the "fast" baseline
- Apply the same multiplier to `FLASH_DURATION` and `HIT_FLASH_DELAY` for consistency, but NOT to `END_DELAY_SECONDS` (that is a post-battle pause, not animation)

---

## Part 12 — AutoClearText in Battle

Modify `systems/BattleSystem.kt` (or whichever system drives combat log advancement):
- Inject `SettingsSystem` via world
- When displaying a battle log message that currently waits for `BattleLogDismissedEvent`:
  - If `settingsSystem.settings.autoClearText == false`: keep existing behavior — wait for player input
  - If `settingsSystem.settings.autoClearText == true`: schedule an `Actions.delay(1.5f)` followed by firing `BattleLogDismissedEvent` automatically
- The auto-dismiss timer resets each time a new log message is shown

---

## Part 13 — UI Ding Sound Effects

Modify `events/Events.kt`:
- Add `class SettingsUiSoundEvent : Event()` (placeholder — no audio file needed yet, just a no-op or log)

Modify `ui/views/SettingsView.kt`:
- Fire `SettingsUiSoundEvent` on `uiStage` in every interaction handler: volume arrow clicks, radio/toggle selection changes, Save click, Cancel click

Modify `systems/AudioSystem.kt`:
- Handle `SettingsUiSoundEvent` in the `handle()` method — queue a placeholder sound (can be the same asset used for UI interactions elsewhere, or a silent stub until a real asset is added)
- Route it through the effects volume channel (same computation as Part 10 effects volume)

---

## Implementation Order

1. **Part 1** — Create the Settings data class with fields: MasterVolume, MusicVolume, EffectsVolume (all default 100), CombatAnimationSpeed (enum: Normal/Fast/Off, default Normal), AutoClearText (bool, default false)
2. **Part 2** — Create a SettingsSystem that holds the active Settings instance and exposes it globally for other systems to read at runtime
3. **Part 3** — Update the initialize game flow to load settings prefs on startup — if prefs don't exist, write defaults; if they do, populate the Settings instance from them
4. **Part 4** — Build the SettingsView UI matching the mockup — 3 volume sliders (10% increment snapping), 3-option radio for combat animations, on/off toggle for auto clear text, Save and Cancel buttons
5. **Part 5** — Wire the SettingsView to read from the current Settings instance when opened, so it always reflects current values
6. **Part 6** — Implement keyboard navigation — track active sub-box context, Up/Down switches sub-box with highlight, Left/Right adjusts value within sub-box, Enter advances to Save then confirms, Esc closes without saving
7. **Part 7** — Implement mouse interaction — clicks adjust controls and shift sub-box highlight context, mouse movement re-takes highlight context from keyboard
8. **Part 8** — Wire Save — applies UI values to the Settings instance, flushes all settings to prefs, closes menu
9. **Part 9** — Wire Cancel / Esc — closes menu without modifying the Settings instance or writing to prefs
10. **Part 10** — Update the volume calculation logic used by audio playback to read MasterVolume and the relevant channel volume from SettingsSystem and compute the final output volume
11. **Part 11** — Update combat animation playback to read CombatAnimationSpeed from SettingsSystem — 2x slower for Normal, half of that for Fast, skip entirely for Off
12. **Part 12** — Update combat text boxes to read AutoClearText from SettingsSystem — wait for input if off, auto-clear after 1.5s if on
13. **Part 13** — Add UI ding sound effects(placeholder) to all setting interactions (sliders, radio buttons, Save/Cancel), routed through the Effects volume channel

---

## Key Files Reference

| File | Path |
|------|------|
| GameScreen | `core/src/main/kotlin/.../screens/GameScreen.kt` |
| MenuView | `core/src/main/kotlin/.../ui/views/MenuView.kt` |
| PlayerKeyboardInputProcessor | `core/src/main/kotlin/.../input/PlayerKeyboardInputProcessor.kt` |
| Events | `core/src/main/kotlin/.../events/Events.kt` |
| AudioSystem | `core/src/main/kotlin/.../systems/AudioSystem.kt` |
| BattleSystem | `core/src/main/kotlin/.../systems/BattleSystem.kt` |
| InitializeGameSystem | `core/src/main/kotlin/.../systems/InitializeGameSystem.kt` |
| PropertyChangeSource | `core/src/main/kotlin/.../ui/viewmodels/PropertyChangeSource.kt` |
| Skin | `core/src/main/kotlin/.../ui/Skin.kt` |
| **[NEW] Settings** | `core/src/main/kotlin/.../configurations/Settings.kt` |
| **[NEW] SettingsSystem** | `core/src/main/kotlin/.../systems/SettingsSystem.kt` |
| **[NEW] SettingsViewModel** | `core/src/main/kotlin/.../ui/viewmodels/SettingsViewModel.kt` |
| **[NEW] SettingsView** | `core/src/main/kotlin/.../ui/views/SettingsView.kt` |

## Verification

1. `./gradlew :core:compileKotlin` — must pass after each part