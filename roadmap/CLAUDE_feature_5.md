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
| `systems/LifeSystem.kt` | Currently contains floating text creation logic tied to life stat changes. Font also stored here. Both to be removed and replaced with `FloatingTextEvent`. |
| `systems/FloatingTextSystem.kt` | Existing system driving bounce/fade/removal of floating text entities. To be extended to implement `EventListener` and become the sole creator of `FloatingTextComponent` entities. |
| `systems/BattleSystem.kt` | Fires `FloatingTextEvent` at the point of damage, concurrent with existing hit flash. |
| `systems/StatSystem.kt` | Applies consumable stat deltas to player entities. Fires `FloatingTextEvent` after applying delta — has direct `Entity` reference and stat values needed to build display text. |
| `ui/Skin.kt` | Central font registry. `"damage.fnt"` moved here from `LifeSystem`. |
| `events/Events.kt` | Central event definitions. `FloatingTextEvent` added here. |

---

## Next Feature

# Floating Text — Refactor and Expansion

## Context
- Floating text creation is currently coupled to `LifeSystem` — this feature fully decouples it
- `FloatingTextEvent(sourceEntity, text, fontType)` is the single trigger for all floating text — always fired by a **system**, never by a ViewModel or View
- `FloatingTextSystem` is the sole creator of `FloatingTextComponent` entities — no other system creates them directly
- All systems that cause a stat change (damage, consumable use, future buff/debuff systems) follow the same pattern: apply the change, then fire `FloatingTextEvent` with the entity reference, display text, and font type
- Only `"damage.fnt"` exists currently — it is used for all floating text types. TODO comments mark where colored fonts should be swapped in once assets are created
- Consumable floating text fires in both combat and overworld — `FloatingTextSystem` handles both contexts identically

---

## Part 1 — Move Font Loading to `Skin.kt`

Modify `ui/Skin.kt`:
- Add `DAMAGE` to the `Fonts` enum (or equivalent font registry)
- Load `"damage.fnt"` at startup alongside other fonts
- Access pattern: `skin[Fonts.DAMAGE]` — consistent with all other font and drawable lookups

Modify `systems/LifeSystem.kt`:
- Remove font loading and font field storage entirely
- Replace any direct font reference with `skin[Fonts.DAMAGE]` — temporary, as font usage moves out of `LifeSystem` entirely in Part 2

---

## Part 2 — Define `FloatingTextEvent` and Decouple from `LifeSystem`

Add to `events/Events.kt`:
- `class FloatingTextEvent(val sourceEntity: Entity, val text: String, val fontType: Fonts) : Event()`
- `sourceEntity` provides position via its `PhysicsComponent` — `FloatingTextSystem` reads `startingLocation` from there

Modify `systems/LifeSystem.kt`:
- Remove all `FloatingTextComponent` creation logic
- At the point where a life/damage value change is registered, fire `FloatingTextEvent(entity, damageAmount.toString(), Fonts.DAMAGE)` on `gameStage`
- `LifeSystem` now has no remaining floating text concern — font field removed, component creation removed

---

## Part 3 — Rework `FloatingTextSystem` to Handle Events

Modify `systems/FloatingTextSystem.kt`:
- Implement `EventListener`; register on `gameStage`
- Handle `FloatingTextEvent`: read `startingLocation` from `sourceEntity`'s `PhysicsComponent`, create a new entity with `FloatingTextComponent` populated with position, text, and font
- `FloatingTextSystem` is now the **sole** creator of `FloatingTextComponent` entities — this is the established pattern all other systems must follow
- Existing bounce/fade/removal tick logic unchanged — only the creation pathway changes

---

## Part 4 — Fire `FloatingTextEvent` for Damage in `BattleSystem`

Modify `systems/BattleSystem.kt`:
- At the point where a hit is registered and the white flash plays, fire `FloatingTextEvent(targetEntity, damageAmount.toString(), Fonts.DAMAGE)` on `gameStage`
- Fires concurrently with the hit flash — no additional delay
- `LifeSystem` still fires its own `FloatingTextEvent` for life changes outside of battle (Part 2) — `BattleSystem` handles the in-battle case explicitly at hit time so timing aligns with the flash animation

---

## Part 5 — Fire `FloatingTextEvent` for Consumable Use in `StatSystem`

Modify `systems/StatSystem.kt`:
- After applying a consumable stat delta to the target entity, look up the full `ConsumableItemData` by `itemId` from `ConsumableItems.kt` to retrieve `statType` and `statValue`
- Build display text from the applied values: `"+20 HP"` for `HEALTH`, `"+20 MP"` for `MANA`
- Fire `FloatingTextEvent(targetEntity, displayText, Fonts.DAMAGE)` on `gameStage`
- `StatSystem` has direct access to the `Entity` reference and all stat values — no position lookup required from the UI layer
- Add TODO comments on each stat type case for future font replacement:
  ```
  // TODO: Replace with Fonts.HEAL (green) once "heal.fnt" asset is created
  // TODO: Replace with Fonts.MANA (blue) once "mana.fnt" asset is created
  ```
- Fires in both combat and overworld — `FloatingTextSystem` handles both contexts identically

---

## Part 6 — Verification Pass

- Confirm font loads from `Skin.kt` via `skin[Fonts.DAMAGE]` — no font references remain in `LifeSystem`
- Confirm `LifeSystem` has no `FloatingTextComponent` creation logic remaining
- Confirm `FloatingTextSystem` is the sole creator of `FloatingTextComponent` entities
- Confirm damage floating text appears over the correct entity at hit time, concurrent with white flash, in both `LifeSystem` (overworld) and `BattleSystem` (combat) contexts
- Confirm consumable floating text appears over the target character in both combat and overworld after stat delta is applied
- Confirm existing bounce/fade/removal behavior is unchanged
- Confirm TODO comments are present in `StatSystem` for future font types
- `./gradlew :core:compileKotlin` — must pass after each part

---

## Implementation Order

1. **Part 1** — Move `"damage.fnt"` loading into `Skin.kt` under `Fonts.DAMAGE`; remove font field from `LifeSystem`
2. **Part 2** — Add `FloatingTextEvent` to `Events.kt`; strip `FloatingTextComponent` creation from `LifeSystem`; fire `FloatingTextEvent` at point of life change instead
3. **Part 3** — Rework `FloatingTextSystem` to implement `EventListener`; handle `FloatingTextEvent` to create `FloatingTextComponent` entities; preserve existing tick logic unchanged
4. **Part 4** — Fire `FloatingTextEvent` from `BattleSystem` at hit time, concurrent with white flash
5. **Part 5** — Fire `FloatingTextEvent` from `StatSystem` after consumable stat delta is applied; build display text from item config; add TODO comments for future font types
6. **Part 6** — Verification pass: font location, decoupling, sole creator pattern, damage and consumable text correctness, existing animation unchanged

---

## Key Files Reference

| File | Path |
|------|------|
| GameScreen | `core/src/main/kotlin/.../screens/GameScreen.kt` |
| LifeSystem | `core/src/main/kotlin/.../systems/LifeSystem.kt` |
| FloatingTextSystem | `core/src/main/kotlin/.../systems/FloatingTextSystem.kt` |
| BattleSystem | `core/src/main/kotlin/.../systems/BattleSystem.kt` |
| StatSystem | `core/src/main/kotlin/.../systems/StatSystem.kt` |
| Skin | `core/src/main/kotlin/.../ui/Skin.kt` |
| Events | `core/src/main/kotlin/.../events/Events.kt` |

## Verification

1. `./gradlew :core:compileKotlin` — must pass after each part
