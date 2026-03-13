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

## Key Overworld Files

| File | Purpose |
|------|---------|
| `screens/GameScreen.kt` | Main screen. Owns `gameStage`, `uiStage`, `entityWorld`. Manages system enable/disable, UI layer transitions (fade in/out), input processors. `render()` drives the ECS tick. |
| `ui/views/MainGameView.kt` | Overworld HUD overlay. Player info, experience bar, menu buttons (inventory, skills, quests, map, menu). Built with Scene2D DSL. |
| `ui/viewmodels/MainGameViewModel.kt` | Bridges ECS events to HUD properties. Listens for `EntityTakeDamageEvent`, `EntityRespawnEvent`, `EntityAddItemEvent`. Exposes `playerLife`, `expAmount`, `lootText`. |
| `systems/MapSystem.kt` | Loads Tiled maps, places player at spawners/portals, persists map state to preferences. Responds to `PortalEvent`. `setMap(mapName, targetPortalId)` handles overworld transitions. |
| `systems/MoveSystem.kt` | Converts movement input (`MoveComponent.cos`/`sin` + speed) into Box2D impulses. Handles image flip for facing direction. Operates on entities with `MoveComponent`, `PhysicsComponent`, `StatComponent`. |

---

## Next Feature

# Overworld Mechanics Update

## Context
The overworld currently allows NPC entities to move freely via AI behavior trees, and the player can attack with spacebar. This feature removes those real-time combat mechanics from the overworld (preserving them for battle), adds a dedicated E key interaction system for NPCs/signs/items, and builds out the CharacterInfoView with full player stats.

---

## Part 1: Disable NPC Overworld Movement & Player Overworld Attack

**Why combined:** Both changes use the same mechanism — a set of systems to disable in overworld mode.

### Files to modify:

**`screens/GameScreen.kt`**
- Add a new `overworldDisabledSystems` set containing `AiSystem::class` and `AttackSystem::class`
- Add a `disableOverworldSystems()` helper that disables all systems in that set
- Call `disableOverworldSystems()` at the end of `show()` (after systems are registered as listeners)
- Call `disableOverworldSystems()` in `exitBattleMode()` after line 271 (`entityWorld.systems.forEach { it.enabled = true }`) — since that blanket re-enables everything
- Call `disableOverworldSystems()` in `pauseWorld(false)` path (resume) — since that also re-enables systems

**`input/PlayerKeyboardInputProcessor.kt`**
- Remove the SPACE key handler (lines 162-168) that sets `doAttack = true`
- The `attackComponents` constructor parameter and import can be removed since nothing else uses it

---

## Part 2: Add E Key Interaction System

### Files to modify:

**`events/Events.kt`**
- Add `class InteractionEvent : Event()`

**New file: `systems/InteractionSystem.kt`**
- `@AllOf([PlayerComponent::class, PhysicsComponent::class, MoveComponent::class])` — iterates over player entity
- Listens for `InteractionEvent` via `EventListener` interface, sets `interactionRequested = true` flag
- On `onTickEntity`: if `interactionRequested` is false, early return. Otherwise:
  - Get player position, size, offset from `PhysicsComponent`
  - Get facing direction from `MoveComponent`
  - Build a directional AABB rectangle in front of the player (reuse the same hitbox logic from `AttackSystem` lines 83-116 but with a slightly wider/more forgiving range)
  - Query `physicsWorld` for entities in the AABB
  - For each found entity: set `dialogComponent.interactingEntity` or `lootComponent.interactingEntity` to the player entity (same as AttackSystem lines 153-160 does currently)
  - Skip self, skip non-hitbox-sensor fixtures (same filters as AttackSystem)
  - Reset `interactionRequested = false`

**`input/PlayerKeyboardInputProcessor.kt`**
- Replace E key placeholder (lines 169-174) with: `gameStage.fire(InteractionEvent())`
- Add import for `InteractionEvent`

**`screens/GameScreen.kt`**
- Register `InteractionSystem` in the ECS world systems block (add `add<InteractionSystem>()` after `DialogSystem`)

---

## Part 3: Build Out CharacterInfoView

### Files to modify:

**`ui/viewmodels/CharacterInfoViewModel.kt`** (rewrite from skeleton)
- Store `world` and `gameStage` as properties (currently constructor params are unused)
- Create player family: `world.family(allOf = arrayOf(PlayerComponent::class))`
- Create stat mapper: `world.mapper<StatComponent>()`
- Add observable properties via `propertyNotify`:
  - `playerLevel: Int`, `playerExperience: Int`, `playerExperienceToNext: Int`
  - `playerCurrentHealth: Float`, `playerMaxHealth: Float`
  - `playerCurrentMana: Float`, `playerMaxMana: Float`
  - `playerAttack: Float`, `playerDefense: Float`, `playerSpeed: Float`
- Add `refreshStats()` method that reads all stats from the player entity's `StatComponent` and updates all properties
- Listen for `EntityTakeDamageEvent` (update HP) and `EntityRespawnEvent` (full refresh)

**`ui/views/CharacterInfoView.kt`** (rewrite from placeholder)
- Store `model` as a property (currently shadowed)
- Layout using Scene2D DSL inside a `FRAME_BGD` background table:
  - Title label: "Character"
  - Level row
  - XP bar (grey background + green fill, like the existing exp bar pattern in MainGameView)
  - HP row: label + value
  - Mana row: label + value
  - Attack row: label + value
  - Defense row: label + value
  - Speed row: label + value
- Bind each label to the corresponding ViewModel property via `model.onPropertyChange(...)`
- Override `setVisible()` to call `model.refreshStats()` when becoming visible — ensures stats are fresh every time the C key opens the panel

---

## Implementation Order

1. **Part 1** — Disable AiSystem + AttackSystem, remove SPACE handler (quick, low risk)
2. **Part 2** — InteractionSystem + E key wiring (medium complexity, new file)
3. **Part 3** — CharacterInfoView + ViewModel (self-contained UI work)

---

## Key Files Reference

| File | Path |
|------|------|
| GameScreen | `core/src/main/kotlin/.../screens/GameScreen.kt` |
| PlayerKeyboardInputProcessor | `core/src/main/kotlin/.../input/PlayerKeyboardInputProcessor.kt` |
| Events | `core/src/main/kotlin/.../events/Events.kt` |
| AttackSystem (reference for AABB) | `core/src/main/kotlin/.../systems/AttackSystem.kt` |
| AiSystem | `core/src/main/kotlin/.../systems/AiSystem.kt` |
| DialogSystem | `core/src/main/kotlin/.../systems/DialogSystem.kt` |
| CharacterInfoViewModel | `core/src/main/kotlin/.../ui/viewmodels/CharacterInfoViewModel.kt` |
| CharacterInfoView | `core/src/main/kotlin/.../ui/views/CharacterInfoView.kt` |
| StatComponent | `core/src/main/kotlin/.../components/StatComponent.kt` |

## Verification

1. `./gradlew :core:compileKotlin` — must pass after each part
2. Run the game:
   - NPC entities (slimes, old man) should stand still in the overworld
   - Spacebar should do nothing
   - Walk up to a sign/NPC, press E — dialog should appear
   - Press C — CharacterInfoView should show level, HP, mana, attack, defense, speed
   - Enter battle — battle should work exactly as before (attacks, AI unaffected)
   - Exit battle — overworld systems should remain disabled (NPCs still, no attack)
