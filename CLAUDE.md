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

# Feature 23 — Remove Dead Life/Death/Attack Systems

All overworld combat infrastructure (`LifeSystem`, `DeathSystem`, `AttackSystem`, `LifeComponent`, `DeathComponent`) became dead code when turn-based battle replaced real-time overworld combat. These systems still exist in the codebase, register in the ECS world, and run every tick — but process nothing. `AttackSystem` is permanently disabled via `overworldDisabledSystems`. `LifeSystem` runs but `takeDamage` is never set (no attacker). `DeathSystem` iterates zero entities (no `DeathComponent` is ever added). Three events they fire (`EntityAttackEvent`, `EntityDeathEvent`, `EntityTakeDamageEvent`, `EntityRespawnEvent`) are never received in a meaningful way. All real battle death logic lives in `BattleSystem` and is unaffected.

**Audio note:** `AudioSystem` currently handles `EntityAttackEvent` and `EntityDeathEvent` for attack/death sounds — both of which never fire. Wiring battle audio to `BattleSystem` is a future feature; for now these dead handlers are simply removed.

**`AttackComponent` is preserved** — `AiEntity` references it and the AI system will use it for future complex battle enemy behavior.

---

## What Gets Deleted

| File | Reason |
|------|--------|
| `systems/LifeSystem.kt` | Dead — `takeDamage` never set, `isDead` never true in overworld |
| `systems/DeathSystem.kt` | Dead — `DeathComponent` never added, iterates zero entities |
| `systems/AttackSystem.kt` | Dead — permanently in `overworldDisabledSystems`, never runs |
| `components/LifeComponent.kt` | Dead — only `takeDamage` field was relevant; all other fields superseded by `StatsProvider` |
| `components/DeathComponent.kt` | Dead — never added to any entity |

---

## What Gets Modified

### Part 1 — `events/Events.kt`
Remove 4 dead events that are never fired (or whose only firer is being deleted):
- `EntityAttackEvent` — only fired by `AttackSystem` (deleted)
- `EntityDeathEvent` — only fired by `LifeSystem` (deleted)
- `EntityRespawnEvent` — only fired by `DeathSystem` (deleted)
- `EntityTakeDamageEvent` — only fired by `LifeSystem` (deleted)

### Part 2 — `screens/GameScreen.kt`
- Remove `add<AttackSystem>()`, `add<DeathSystem>()`, `add<LifeSystem>()` from ECS world setup
- Remove `AttackSystem::class` from `overworldDisabledSystems` (keep `AiSystem::class` — the set remains valid with one entry)
- Remove `attackMapper` field (`by lazy { entityWorld.mapper<AttackComponent>() }`)
- Remove the attack state reset block in `exitBattleMode()` (the `playerFamily.forEach` block that clears `doAttack`/`AttackState.READY`) and its comment
- Remove imports: `AttackSystem`, `DeathSystem`, `LifeSystem`, `AttackComponent`, `AttackState`

### Part 3 — `systems/EntityCreationSystem.kt`
Remove `LifeComponent` additions from NPC entity creation. There are 3 `add<LifeComponent> { ... }` blocks — all in the NPC/non-player creation paths. These set `maxHealth` and `health` fields that `LifeSystem` never read.
- Remove all 3 `add<LifeComponent> { ... }` blocks
- Remove `LifeComponent` import
- `AttackComponent` additions are **kept** (used by `AiEntity` for future AI logic)

### Part 4 — `ui/viewmodels/MainGameViewModel.kt`
The view model listened to events that are now removed, and held properties that are already commented out in the view.
- Remove `lifeComponents` mapper field
- Remove `EntityTakeDamageEvent` handler block
- Remove `EntityRespawnEvent` handler block
- Remove `playerLife` and `enemyLife` `propertyNotify` fields (the `MainGameView` binding to `playerLife` is already commented out; `enemyLife` is never bound)
- Remove imports: `LifeComponent`, `EntityTakeDamageEvent`, `EntityRespawnEvent`

### Part 5 — `systems/AudioSystem.kt`
Remove the two dead event handlers whose source events are being deleted:
- Remove `EntityAttackEvent` handler (`queueSound(..._attack.wav)`)
- Remove `EntityDeathEvent` handler (`queueSound(..._death.wav)`)
- Remove imports: `EntityAttackEvent`, `EntityDeathEvent`

---

## Key Files Reference

| File | Path |
|------|------|
| GameScreen | `core/src/main/kotlin/.../screens/GameScreen.kt` |
| Events | `core/src/main/kotlin/.../events/Events.kt` |
| EntityCreationSystem | `core/src/main/kotlin/.../systems/EntityCreationSystem.kt` |
| MainGameViewModel | `core/src/main/kotlin/.../ui/viewmodels/MainGameViewModel.kt` |
| AudioSystem | `core/src/main/kotlin/.../systems/AudioSystem.kt` |

## Verification

1. `./gradlew :core:compileKotlin` — must pass after each part
