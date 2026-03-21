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
| `systems/StatSystem.kt` | Central handler for all stat values, recalculation, EXP/level logic, and skill point application. Extended significantly in this feature. |
| `systems/FloatingTextSystem.kt` | Handles `FloatingTextEvent` — fires `"LEVEL UP!"` text on level up following established pattern. |
| `components/StatComponent.kt` | Holds all entity stat data. Extended with skill point, ability point, and invested point tracking fields. |
| `ui/views/SkillView.kt` | Currently a placeholder. Fully implemented in this feature. |
| `ui/viewmodels/SkillViewModel.kt` | Currently a placeholder. Fully implemented in this feature. |
| `ui/views/AbilityView.kt` | Does not exist yet. Created as a placeholder in this feature. |
| `ui/viewmodels/AbilityViewModel.kt` | Does not exist yet. Created as a placeholder in this feature. |
| `ui/views/MainGameView.kt` | Contains overworld HUD buttons. Skill and Ability buttons added here. |
| `input/PlayerKeyboardInputProcessor.kt` | Keyboard input routing. Hotkeys for SkillView and AbilityView added here. |
| `events/Events.kt` | Central event definitions. All new level/skill/ability events added here. |

---

## Next Feature

# EXP, Level Ups, Skill Points, and Ability Points

## Context
- EXP and levels are already implemented as placeholders — this feature reworks the formula, stat gains, and adds skill/ability point systems
- EXP formula: `req(n) = n * 50 * (1.15 ^ n)` — soft exponential scaling. Reference thresholds (first 10 levels):
  ```
  // L1→2:  58     L2→3:  132    L3→4:  263    L4→5:  439    L5→6:  669
  // L6→7:  967    L7→8:  1342   L8→9:  1811   L9→10: 2394   L10→11: 3107
  ```
- Level up grants: `+10 maxHP`, `+5 maxMana`, `+1 skillPoints`, `+1 abilityPoints` — no other passive stat boosts
- `GainSkillPointEvent` and `GainAbilityPointEvent` are decoupled from level-up — future non-level sources (quests, items) can fire them independently
- Skill/ability points are tracked in `StatComponent` and saved as part of character data — not independently to prefs. This avoids desync risk and prepares for a future serialization-based save system overhaul
- `skillPointsInvestedAttack` and `skillPointsInvestedDefense` are tracked as separate fields (not collapsed into raw stats) — preserves investment history for a future respec feature
- `-` button in `SkillView` is only enabled for pending (unsaved) allocations — enforced in `SkillViewModel`, not just the View
- Level up notification is floating text only — `SkillView` is never forced open automatically
- `AbilityView` and `AbilityViewModel` are created as placeholders only — ability spending is deferred to a future feature

---

## Part 1 — Update EXP Formula and Level-Up Stat Gains in `StatSystem`

Modify `systems/StatSystem.kt`:
- Replace current linear EXP formula with: `req(n) = n * 50 * (1.15 ^ n)`
- Document formula and first 10 level thresholds in a comment block (values above)
- On level up, apply only: `statComponent.maxHealth += 10`, `statComponent.maxMana += 5`
- Remove any other passive stat boosts currently granted on level up
- After stat changes, fire the following events (defined in Part 2):
  - `LevelUpEvent(entity, newLevel)`
  - `GainSkillPointEvent(entity)`
  - `GainAbilityPointEvent(entity)`
- Save character state to prefs after level up

---

## Part 2 — Extend `StatComponent` and Define New Events

Modify `components/StatComponent.kt`:
- Add fields:
  - `skillPoints: Int = 0` — available unspent skill points
  - `abilityPoints: Int = 0` — available unspent ability points
  - `skillPointsInvestedAttack: Int = 0` — cumulative points spent on attack
  - `skillPointsInvestedDefense: Int = 0` — cumulative points spent on defense
- Invested point fields are never decremented after save — tracked separately from raw stat values
- Effective stat bonuses computed at recalc time: `attack += skillPointsInvestedAttack * 2`, `defense += skillPointsInvestedDefense * 1`

Add to `events/Events.kt`:
- `class LevelUpEvent(val entity: Entity, val newLevel: Int) : Event()`
- `class GainSkillPointEvent(val entity: Entity) : Event()`
- `class GainAbilityPointEvent(val entity: Entity) : Event()`
- `class SkillPointsSaveEvent(val entity: Entity, val pendingAttackPoints: Int, val pendingDefensePoints: Int) : Event()`
- `class SkillPointsChangedEvent(val entity: Entity) : Event()`
- `class SkillViewOpenEvent : Event()`
- `class SkillViewClosedEvent : Event()`
- `class AbilityViewOpenEvent : Event()`
- `class AbilityViewClosedEvent : Event()`

---

## Part 3 — Handle `GainSkillPointEvent` and `GainAbilityPointEvent` in `StatSystem`

Modify `systems/StatSystem.kt`:
- Handle `GainSkillPointEvent(entity)`: increment `statComponent.skillPoints`; save character state to prefs
- Handle `GainAbilityPointEvent(entity)`: increment `statComponent.abilityPoints`; save character state to prefs
- Both handlers are intentionally separate from level-up logic — any future source (quest reward, item) can fire them independently without triggering a level up

---

## Part 4 — Level Up Floating Text Notification

Modify `systems/FloatingTextSystem.kt` (or `StatSystem.kt` at point of firing):
- On `LevelUpEvent`: fire `FloatingTextEvent(entity, "LEVEL UP!", Fonts.DAMAGE)` — follows the established pattern from Feature 5
- Add TODO comment:
  ```
  // TODO: Replace with Fonts.LEVEL_UP (gold) once "levelup.fnt" asset is created
  ```
- Floating text fires over the levelled entity; existing bounce/fade behavior unchanged

---

## Part 5 — Build `SkillViewModel.kt`

Modify `ui/viewmodels/SkillViewModel.kt` (currently placeholder):
- Extend `PropertyChangeSource`, implement `EventListener`; register on `uiStage`
- Observable properties:
  - `availableSkillPoints by propertyNotify(0)`
  - `pendingAttackPoints by propertyNotify(0)`
  - `pendingDefensePoints by propertyNotify(0)`
  - `hasUnsavedChanges by propertyNotify(false)` — true when any pending value > 0
  - `showCancelConfirm by propertyNotify(false)`
  - `showSaveConfirm by propertyNotify(false)`
  - `investedAttackPoints by propertyNotify(0)` — read from `StatComponent` on open, for display
  - `investedDefensePoints by propertyNotify(0)` — read from `StatComponent` on open, for display
- On `SkillViewOpenEvent`: populate `availableSkillPoints`, `investedAttackPoints`, `investedDefensePoints` from `StatComponent`; reset all pending values to 0
- `availableSkillPoints` decrements on `+` press, increments on `-` press (pending reversal only)
- `-` enabled only when `pendingPoints > 0` for that stat — cannot reverse previously saved investments
- `hasUnsavedChanges = pendingAttackPoints > 0 || pendingDefensePoints > 0`
- On `SkillPointsChangedEvent`: refresh all displayed values from `StatComponent`

---

## Part 6 — Build `SkillView.kt`

Modify `ui/views/SkillView.kt` (currently placeholder):
- Extend `Table(skin)`, mix `KTable`, `setFillParent(true)`
- Layout:
  - **Header**: `"Skill Points Available: X"` — reactive to `model.availableSkillPoints`
  - **Stat rows** (Attack and Defense):
    - Stat name | `[-]` `[pending pts]` `[+]` | resulting value preview (e.g. `"→ 14 ATK"`, `"→ 7 DEF"`)
    - Resulting value = current base stat + (invested + pending) * multiplier
    - `-` button visible and enabled only when `pendingPoints > 0` for that row
    - `+` button disabled when `availableSkillPoints == 0`
  - **Footer**: Save and Cancel buttons side by side
- **Cancel flow**: if `hasUnsavedChanges`, set `showCancelConfirm = true` — inline confirmation `"Discard unsaved changes?"` with Yes/No; Yes resets pending state and fires `SkillViewClosedEvent`; No returns to skill view
- **Save flow**: set `showSaveConfirm = true` — inline confirmation `"Stat changes cannot be undone. Confirm?"` with Yes/No; Yes fires `SkillPointsSaveEvent`; No returns to skill view
- If no unsaved changes, Cancel closes immediately without confirmation
- Bind all elements via `model.onPropertyChange()`
- DSL extension function following existing view patterns

---

## Part 7 — Wire Save to `StatSystem` and Persist

Modify `systems/StatSystem.kt`:
- Handle `SkillPointsSaveEvent(entity, pendingAttackPoints, pendingDefensePoints)`:
  - `statComponent.skillPointsInvestedAttack += pendingAttackPoints`
  - `statComponent.skillPointsInvestedDefense += pendingDefensePoints`
  - `statComponent.skillPoints -= (pendingAttackPoints + pendingDefensePoints)`
  - Recalculate effective stats: `attack += skillPointsInvestedAttack * 2`, `defense += skillPointsInvestedDefense * 1`
  - Fire `SkillPointsChangedEvent(entity)` to trigger UI refresh
  - Save character state to prefs
  - Fire `SkillViewClosedEvent`

---

## Part 8 — Wire `SkillView` Access (Hotkey + Button)

Modify `input/PlayerKeyboardInputProcessor.kt`:
- Add hotkey (`K`) to fire `SkillViewOpenEvent` on `uiStage` — same pattern as `I` for inventory

Modify `ui/views/MainGameView.kt`:
- Add Skill button alongside the existing Inventory button — fires `SkillViewOpenEvent` on press

---

## Part 9 — Scaffold `AbilityView` and `AbilityViewModel` as Placeholders

Create `ui/viewmodels/AbilityViewModel.kt`:
- Extend `PropertyChangeSource`, implement `EventListener`; register on `uiStage`
- Observable property: `abilityPoints by propertyNotify(0)`
- On `AbilityViewOpenEvent`: populate `abilityPoints` from `StatComponent`
- No spending logic — placeholder only

Create `ui/views/AbilityView.kt`:
- Extend `Table(skin)`, mix `KTable`, `setFillParent(true)`
- Stub display: `"Ability Points: X — Coming Soon"`
- `abilityPoints` value bound to `AbilityViewModel` so it is visible and verifiable
- DSL extension function following existing view patterns

Modify `input/PlayerKeyboardInputProcessor.kt`:
- Add hotkey (`J`) to fire `AbilityViewOpenEvent`

Modify `ui/views/MainGameView.kt`:
- Add Ability button alongside Skill button — fires `AbilityViewOpenEvent` on press

---

## Part 10 — Verification Pass

- Confirm EXP formula: verify level up triggers at correct thresholds matching the documented comment block values
- Confirm level up grants exactly: `+10 maxHP`, `+5 maxMana`, `+1 skillPoints`, `+1 abilityPoints` — no other stat changes
- Confirm `"LEVEL UP!"` floating text fires over the entity on level up
- Confirm `GainSkillPointEvent` and `GainAbilityPointEvent` can be fired independently without a level up
- Confirm `SkillView`: available points decrement on `+`; `-` only enabled for pending allocations; resulting value preview updates in real time; cancel confirm shown only on unsaved changes; save confirm always shown before applying
- Confirm saved skill points correctly update `skillPointsInvestedAttack`/`Defense`, decrement `skillPoints`, and recalculate effective stats
- Confirm character state saved to prefs after level up and after skill point save
- Confirm `AbilityView` displays correct `abilityPoints` value from `StatComponent`
- Confirm `K` and `J` hotkeys and MainGameView buttons open correct views
- `./gradlew :core:compileKotlin` — must pass after each part

---

## Implementation Order

1. **Part 1** — Replace EXP formula with soft exponential; update level-up stat gains to HP/mana only; fire `LevelUpEvent`, `GainSkillPointEvent`, `GainAbilityPointEvent` on level up
2. **Part 2** — Add skill/ability point fields to `StatComponent`; add all new events to `Events.kt`
3. **Part 3** — Handle `GainSkillPointEvent` and `GainAbilityPointEvent` in `StatSystem` — increment respective point fields and save
4. **Part 4** — Fire `"LEVEL UP!"` floating text on `LevelUpEvent` via established `FloatingTextEvent` pattern
5. **Part 5** — Build `SkillViewModel` with available/pending/invested point tracking and unsaved change state
6. **Part 6** — Build `SkillView` with stat rows, `[-][pts][+]` controls, resulting value preview, and save/cancel confirm flows
7. **Part 7** — Handle `SkillPointsSaveEvent` in `StatSystem` — apply invested points, recalculate stats, save to prefs, fire closed event
8. **Part 8** — Wire `SkillView` to `K` hotkey and MainGameView button
9. **Part 9** — Scaffold `AbilityView` and `AbilityViewModel` as placeholders; wire to `J` hotkey and MainGameView button
10. **Part 10** — Verification pass: EXP thresholds, level-up grants, floating text, skill point UI flows, stat recalc correctness, persistence, ability placeholder display

---

## Key Files Reference

| File | Path |
|------|------|
| GameScreen | `core/src/main/kotlin/.../screens/GameScreen.kt` |
| StatSystem | `core/src/main/kotlin/.../systems/StatSystem.kt` |
| FloatingTextSystem | `core/src/main/kotlin/.../systems/FloatingTextSystem.kt` |
| StatComponent | `core/src/main/kotlin/.../components/StatComponent.kt` |
| PlayerKeyboardInputProcessor | `core/src/main/kotlin/.../input/PlayerKeyboardInputProcessor.kt` |
| MainGameView | `core/src/main/kotlin/.../ui/views/MainGameView.kt` |
| Events | `core/src/main/kotlin/.../events/Events.kt` |
| **[REWORKED] SkillView** | `core/src/main/kotlin/.../ui/views/SkillView.kt` |
| **[REWORKED] SkillViewModel** | `core/src/main/kotlin/.../ui/viewmodels/SkillViewModel.kt` |
| **[NEW] AbilityView** | `core/src/main/kotlin/.../ui/views/AbilityView.kt` |
| **[NEW] AbilityViewModel** | `core/src/main/kotlin/.../ui/viewmodels/AbilityViewModel.kt` |

## Verification

1. `./gradlew :core:compileKotlin` — must pass after each part
