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
| `screens/GameScreen.kt` | Main screen. Owns `gameStage`, `uiStage`, `entityWorld`. Character switch transition handled here. |
| `systems/StatSystem.kt` | Writes stat changes back to `CharacterData` in real time via `PartySystem`. Must avoid save loops. |
| `systems/InitializeGameSystem.kt` | Extended to load all `CharacterData` from prefs on startup. |
| `systems/BattleSystem.kt` | Spawns all `combatSlots` characters on combat start; writes HP/mana back to `CharacterData` in real time; handles defeat floor restore. |
| `systems/EntityCreationSystem.kt` | Extended to spawn player entities from `CharacterData` on game load and character switch. |
| `components/StatComponent.kt` | Remains the runtime stat container for spawned entities. Populated from `CharacterData` on spawn. |
| `components/AbilityComponent.kt` | Populated from `CharacterData.unlockedAbilityIds` on entity spawn. |
| `components/ItemComponent.kt` | `InventoryComponent` equipped slots populated from `CharacterData.equippedItems` on spawn. |
| `ui/views/CharacterInfoView.kt` | Fully reworked — left panel character list, right panel detail. |
| `ui/views/BattleView.kt` | Spells button re-evaluated per active turn entity. |
| `input/PlayerKeyboardInputProcessor.kt` | Alt+1–6 hotkeys fire `SwitchActiveCharacterEvent`. |
| `ui/views/MainGameView.kt` | Character switcher accessible from HUD if needed. |
| `events/Events.kt` | All new party and character events added here. |
| `maps/map_1.tmx` | 2–3 recruitable NPC entities added here. |
| `maps/battle.tmx` | 2 additional player spawn points added (slots 2 and 3). |

---

## Next Feature

# Multiple Player Characters

## Context
- Character data is decoupled from entity lifecycle — `CharacterData` lives in `PartySystem` singleton, always in memory, always saveable
- `StatComponent` remains the runtime container for a spawned entity — populated from `CharacterData` on spawn, written back in real time as changes occur
- Only one character entity exists in the overworld at a time — switching despawns the current and spawns the new one with a brief fade transition
- Combat spawns all characters in `combatSlots` (up to 3) simultaneously — each from their own `CharacterData`
- `combatSlots` is auto-populated in join order for now — player-assigned combat slot UI is deferred to a future feature
- All UIs read character data from `PartySystem.characterDataMap` — never from a live entity — so non-active characters can be equipped, leveled, and modified at any time
- HP/mana written back to `CharacterData` in real time during combat; all other stats written back after recalc
- On combat defeat: all party members restored to a minimum floor of 1 HP before writing back and saving
- `StatSystem` write-back must avoid save loops — a stat change must not trigger a recalc that triggers another save; use a dirty flag or guard to prevent cascading writes
- NPC recruits are permanently removed from the map on joining — they never reappear
- `AbilityView` character switcher becomes functional in this feature now that multiple characters exist

---

## Part 1 — Create `CharacterConfigs.kt`

Create `configurations/CharacterConfigs.kt`:
- Data class `BaseStats`: `maxHp: Int`, `maxMana: Int`, `attack: Int`, `defense: Int`, `speed: Int`
- Data class `CharacterConfig`: `characterId: Int`, `characterName: String`, `abilityTreeId: Int`, `baseStats: BaseStats`, `atlasKey: String`, `portraitKey: String`
- Define 3 characters:
  - `CHARACTER_1` (existing player) — populated with current player's base values; `characterId = 1`
  - `CHARACTER_2` — distinct base stats (e.g. higher mana/speed, lower HP); `characterId = 2`
  - `CHARACTER_3` — distinct base stats (e.g. high HP/defense, lower speed); `characterId = 3`
- Each references a distinct `abilityTreeId` from `AbilityTrees.kt` — add stub trees for characters 2 and 3 if not present
- Top-level registry: `val CHARACTER_CONFIGS: Map<Int, CharacterConfig>` keyed by `characterId`
- Include commented template for adding new characters:
  ```
  // CharacterConfig(
  //     characterId = 4,
  //     characterName = "Character Name",
  //     abilityTreeId = 4,
  //     baseStats = BaseStats(maxHp=80, maxMana=30, attack=12, defense=8, speed=9),
  //     atlasKey = "character_4",
  //     portraitKey = "portrait_4"
  // ),
  ```

---

## Part 2 — Create `CharacterData` and `PartySystem`

Create `systems/PartySystem.kt`:
- Data class `CharacterData`:
  - Identity: `characterId: Int`, `characterName: String`
  - Stats: `currentHp: Int`, `maxHp: Int`, `currentMana: Int`, `maxMana: Int`, `attack: Int`, `defense: Int`, `speed: Int`
  - Progression: `level: Int`, `exp: Int`, `skillPoints: Int`, `abilityPoints: Int`, `skillPointsInvestedAttack: Int`, `skillPointsInvestedDefense: Int`
  - Abilities: `unlockedAbilityIds: Set<Int>`
  - Equipment: `equippedItems: Map<ItemCategory, Int?>`
  - Party state: `isUnlocked: Boolean = false`
- `PartySystem` extends `IntervalSystem()`, implements `EventListener`; registered in `GameScreen.kt`
- Holds:
  - `characterDataMap: MutableMap<Int, CharacterData>`
  - `activeOverworldCharacterId: Int = 1`
  - `combatSlots: MutableList<Int>` — ordered list of up to 3 character IDs; auto-populated in join order
- Exposes:
  - `getCharacterData(id): CharacterData`
  - `updateCharacterData(id, block: CharacterData.() -> Unit)` — applies mutation and calls `saveCharacterData(id)`
  - `unlockCharacter(id)` — sets `isUnlocked = true`, adds to `combatSlots` if fewer than 3, saves
  - `saveCharacterData(id)` — writes single character's data to prefs
  - `getUnlockedCharacters(): List<CharacterData>` — returns all unlocked entries in ID order

---

## Part 3 — Load and Save `CharacterData` via Prefs

Modify `systems/InitializeGameSystem.kt`:
- Inject `PartySystem` via world
- On startup: for each entry in `CHARACTER_CONFIGS`:
  - Check prefs for `"char_data_{id}"` key
  - If absent (first launch): initialize `CharacterData` from `CharacterConfig.baseStats`; `CHARACTER_1` starts with `isUnlocked = true`, all others `false`; write defaults to prefs
  - If present: deserialize and populate `PartySystem.characterDataMap`
- Load `activeOverworldCharacterId` and `combatSlots` from prefs — defaults to `characterId = 1` and `combatSlots = [1]`
- Pref key constants in `PartySystem` companion object:
  ```
  KEY_CHARACTER_DATA_PREFIX = "char_data_"  // e.g. "char_data_1"
  KEY_ACTIVE_CHARACTER = "active_character"
  KEY_COMBAT_SLOTS = "combat_slots"
  ```

---

## Part 4 — Refactor `StatComponent` to Populate from `CharacterData`

Modify `systems/StatSystem.kt`:
- Add a **dirty flag guard** to prevent save loops: `private var isSyncing = false`
  - Set `isSyncing = true` before writing back to `CharacterData`; set `false` after
  - Skip write-back and save if `isSyncing == true` when a stat change is detected
- On player entity spawn: read all `StatComponent` values from `PartySystem.getCharacterData(id)` — HP, mana, attack, defense, speed, level, exp, skill points, invested points
- Write-back rules (all guarded by dirty flag):
  - HP/mana: written back to `CharacterData` immediately on every change via `PartySystem.updateCharacterData()`
  - Stat recalc (equipment change, skill point save): written back after full recalc completes
  - EXP/level: written back on level up after all level-up grants are applied
- `AbilityComponent` populated from `CharacterData.unlockedAbilityIds` on entity spawn
- `InventoryComponent` equipped slots populated from `CharacterData.equippedItems` on entity spawn
- All saves route through `PartySystem.saveCharacterData(id)` — no direct prefs access in `StatSystem`

---

## Part 5 — Overworld Character Spawning and Switching

Modify `systems/EntityCreationSystem.kt`:
- On game load: spawn only the `PartySystem.activeOverworldCharacterId` entity — all values from `CharacterData`
- No other character entities exist in the overworld at any time

Add to `events/Events.kt`:
- `class SwitchActiveCharacterEvent(val newCharacterId: Int) : Event()`
- `class PartyUpdatedEvent : Event()`
- `class AddCharacterToPartyEvent(val characterId: Int, val npcEntity: Entity) : Event()`

Handle `SwitchActiveCharacterEvent` in `GameScreen.kt` or `PartySystem`:
1. If `newCharacterId == activeOverworldCharacterId` or character not unlocked or `characterId > CHARACTER_CONFIGS.size` → do nothing
2. Trigger fade-out transition (reuse existing `GameScreen` fade pattern)
3. Remove current player entity from world
4. Update `PartySystem.activeOverworldCharacterId = newCharacterId`; save to prefs
5. Spawn new player entity from `PartySystem.getCharacterData(newCharacterId)`
6. Trigger fade-in transition

Modify `input/PlayerKeyboardInputProcessor.kt`:
- Alt+1 through Alt+6: fire `SwitchActiveCharacterEvent(id)` on `gameStage`
- No-op conditions handled in the event handler — input processor always fires the event

---

## Part 6 — Add Recruitable NPCs to Map and Dialog Flows

Modify `maps/map_1.tmx`:
- Add 2 NPC entities with properties: `entityToSpawn = NON_PLAYER`, `dialogId = RECRUIT_CHARACTER_2` / `RECRUIT_CHARACTER_3`, `characterId = 2` / `3`

Add to `DialogConfigurations.kt` — one flow per recruit NPC:
```
// RECRUIT_CHARACTER_2
dialog(DialogId.RECRUIT_CHARACTER_2.name) {
    node(0, "[Character 2 name]. Do you want me to join your party?") {
        option("Yes") {
            action = {
                addCharacterToParty(2)
                endDialog()
            }
        }
        option("No") {
            action = { endDialog() }
        }
    }
}
```
- Add `addCharacterToParty(characterId: Int)` to `Dialog.kt` — fires `AddCharacterToPartyEvent(characterId, npcEntity)` on `gameStage`
- Add `DialogId.RECRUIT_CHARACTER_2` and `DialogId.RECRUIT_CHARACTER_3` to the `DialogId` enum in `DialogConfigurations.kt`

Handle `AddCharacterToPartyEvent` in `PartySystem`:
- Call `unlockCharacter(characterId)` — sets `isUnlocked = true`, adds to `combatSlots` if < 3 filled, saves
- Remove `npcEntity` from the world permanently
- Fire `PartyUpdatedEvent` to trigger UI refresh across all relevant views

---

## Part 7 — Rework `CharacterInfoView`

Rework `ui/views/CharacterInfoView.kt` and its ViewModel:
- New layout mirrors inventory UI split:
  - **Left panel**: scrollable list of all unlocked party members
    - Each row: portrait (`portraitKey`), character name, level, HP bar
    - Up/Down navigates rows; focused row highlighted
  - **Right panel**: detail view for the focused character
    - Existing stats display moved here: current HP / max HP, current mana / max mana, attack, defense, speed, level, EXP to next level, gold (from `ResourceSystem`)
- All data sourced from `PartySystem.characterDataMap` — never from a live entity
- Reacts to `PartyUpdatedEvent` — new unlocked characters appear in the list immediately
- Active overworld character indicated visually in the list (e.g. a marker icon)
- Bind to updated ViewModel via `model.onPropertyChange()`
- `CharacterInfoViewOpenEvent` / `CharacterInfoViewClosedEvent` added to `Events.kt` if not already present

---

## Part 8 — Update All UIs to Read from `PartySystem`

Audit and update all views that currently read character data from a live entity or `StatComponent`:

**`InventoryView` / `InventoryLeftPanel`**:
- Character list reads from `PartySystem.getUnlockedCharacters()`
- Equip action: updates `CharacterData.equippedItems` via `PartySystem.updateCharacterData()`; if the target character is the active overworld entity, also update their live `InventoryComponent` and trigger `StatSystem` recalc
- Use consumable action: applies stat delta to `CharacterData` directly; if active entity, also update live `StatComponent`

**`SkillView` / `SkillViewModel`**:
- Reads `skillPoints`, `investedAttack`, `investedDefense` from `CharacterData` for the focused character (not necessarily the active entity)
- Save routes through `PartySystem.updateCharacterData()` and `saveCharacterData()`

**`AbilityView` / `AbilityViewModel`**:
- Character switcher (`activeCharacterIndex`) now maps to real `CharacterData` entries from `PartySystem.getUnlockedCharacters()`
- Reads `abilityPoints`, `unlockedAbilityIds` from focused character's `CharacterData`
- Save routes through `PartySystem` and `AbilitySystem`

**`ShopView` sell guard**:
- Equipped item counts checked across all unlocked characters' `CharacterData.equippedItems` — not just the active entity's `InventoryComponent`

---

## Part 9 — Update `BattleSystem` for Multi-Character Party

Modify `systems/BattleSystem.kt`:
- On combat trigger: spawn player entities for all IDs in `PartySystem.combatSlots`
  - Each entity populated from its `CharacterData` (stats, equipped items, abilities)
  - Map to correct spawn point by slot index (slot 1 → spawner 1, slot 2 → spawner 2, etc.)
  - If fewer than 3 characters in `combatSlots`, only use the first N spawners
- HP/mana write-back to `CharacterData` already handled in real time by `StatSystem` (Part 4) — no additional wiring needed
- On **full victory**:
  - Final `StatComponent` values already synced to `CharacterData` in real time
  - Call `PartySystem.saveCharacterData(id)` for all combat party members
- On **player defeat**:
  - For each party member entity: set `currentHp = max(1, currentHp)` on both `StatComponent` and `CharacterData`
  - Call `PartySystem.saveCharacterData(id)` for all combat party members
- On return to overworld: despawn all combat player entities; respawn only `activeOverworldCharacterId` entity

---

## Part 10 — Update Spells Button for Active Combat Turn

Modify `ui/views/BattleView.kt` and BattleViewModel:
- Spells button enabled/disabled re-evaluated at the start of each player turn — not once at combat start
- On each new player entity turn: check `AbilityComponent.unlockedAbilityIds` of the currently acting entity
  - If non-empty → `spellsButtonEnabled = true`
  - If empty → `spellsButtonEnabled = false`
- Spell list in the Spells panel refreshes per turn — shows only the current turn entity's skilled abilities
- A character with no skilled abilities will always see the Spells button disabled when it is their turn, even if other party members have abilities

---

## Part 11 — Update `battle.tmx` Spawners

Modify `maps/battle.tmx`:
- Add 2 additional player spawn points alongside the existing slot 1 spawner
- Each spawner has a `slotIndex` property (`1`, `2`, `3`) so `BattleSystem` can map `combatSlots[index]` to the correct world position
- Positions: slot 1 leftmost, slot 2 center, slot 3 rightmost (or appropriate spacing for the battle map layout)

---

## Part 12 — Verification Pass

- Confirm `CharacterData` loads from prefs for all 3 characters on startup; defaults written on first launch
- Confirm `CHARACTER_1` starts unlocked, characters 2 and 3 start locked
- Confirm recruit dialog: `"Yes"` → character unlocked, NPC removed permanently, `combatSlots` updated, `CharacterInfoView` list updates; `"No"` → dialog ends, NPC unchanged
- Confirm overworld switch: Alt+1/2/3 fires switch; invalid ID, already-active ID, and unlocked-only guard all no-op correctly; fade transition plays; new entity spawned from correct `CharacterData`
- Confirm `CharacterInfoView` left panel shows all unlocked characters; right panel updates on focus change; active character visually indicated
- Confirm all UIs (inventory equip/use, skills, abilities, shop sell guard) read from `PartySystem` — non-active characters can be modified correctly
- Confirm `StatSystem` dirty flag prevents save loops — rapid stat changes do not cascade into repeated recalcs or saves
- Confirm combat spawns correct entities per `combatSlots` using correct spawn points; fewer than 3 characters uses correct subset of spawners
- Confirm HP/mana written back in real time during combat
- Confirm combat defeat: all party members restored to minimum 1 HP before save
- Confirm Spells button re-evaluated per active turn entity — character with no abilities shows disabled button on their turn
- `./gradlew :core:compileKotlin` — must pass after each part

---

## Implementation Order

1. **Part 1** — Create `CharacterConfigs.kt` with 3 unique character definitions and registry
2. **Part 2** — Create `CharacterData` data class and `PartySystem` singleton with all state management methods
3. **Part 3** — Load and save all `CharacterData` from prefs in `InitializeGameSystem`; handle first-launch defaults
4. **Part 4** — Refactor `StatSystem` to populate `StatComponent` from `CharacterData` on spawn and write back in real time; add dirty flag guard to prevent save loops
5. **Part 5** — Implement overworld character spawning from `CharacterData`; wire Alt+1–6 hotkeys; implement `SwitchActiveCharacterEvent` with fade transition
6. **Part 6** — Add 2 recruitable NPCs to `map_1.tmx`; add recruit dialog flows to `DialogConfigurations.kt`; handle `AddCharacterToPartyEvent` in `PartySystem`
7. **Part 7** — Rework `CharacterInfoView` — left panel character list, right panel detail, sourced from `PartySystem`
8. **Part 8** — Audit and update all UIs (inventory, skills, abilities, shop) to read and write via `PartySystem.characterDataMap`
9. **Part 9** — Update `BattleSystem` to spawn all `combatSlots` entities; handle defeat floor restore; despawn and respawn on return to overworld
10. **Part 10** — Re-evaluate Spells button and spell list per active turn entity in `BattleView`
11. **Part 11** — Add slot 2 and slot 3 player spawners to `battle.tmx` with `slotIndex` properties
12. **Part 12** — Verification pass: data loading, recruit flow, character switching, UI reads, combat spawning, defeat restore, Spells button, dirty flag guard

---

## Key Files Reference

| File | Path |
|------|------|
| GameScreen | `core/src/main/kotlin/.../screens/GameScreen.kt` |
| StatSystem | `core/src/main/kotlin/.../systems/StatSystem.kt` |
| BattleSystem | `core/src/main/kotlin/.../systems/BattleSystem.kt` |
| InitializeGameSystem | `core/src/main/kotlin/.../systems/InitializeGameSystem.kt` |
| EntityCreationSystem | `core/src/main/kotlin/.../systems/EntityCreationSystem.kt` |
| AbilitySystem | `core/src/main/kotlin/.../systems/AbilitySystem.kt` |
| StatComponent | `core/src/main/kotlin/.../components/StatComponent.kt` |
| AbilityComponent | `core/src/main/kotlin/.../components/AbilityComponent.kt` |
| ItemComponent / InventoryComponent | `core/src/main/kotlin/.../components/ItemComponent.kt` |
| PlayerKeyboardInputProcessor | `core/src/main/kotlin/.../input/PlayerKeyboardInputProcessor.kt` |
| DialogConfigurations | `core/src/main/kotlin/.../configurations/DialogConfigurations.kt` |
| CharacterInfoView | `core/src/main/kotlin/.../ui/views/CharacterInfoView.kt` |
| BattleView | `core/src/main/kotlin/.../ui/views/BattleView.kt` |
| InventoryView / InventoryLeftPanel | `core/src/main/kotlin/.../ui/views/InventoryView.kt` |
| SkillView / SkillViewModel | `core/src/main/kotlin/.../ui/views/SkillView.kt` |
| AbilityView / AbilityViewModel | `core/src/main/kotlin/.../ui/views/AbilityView.kt` |
| Events | `core/src/main/kotlin/.../events/Events.kt` |
| map_1.tmx | `assets/maps/map_1.tmx` |
| battle.tmx | `assets/maps/battle.tmx` |
| **[NEW] CharacterConfigs** | `core/src/main/kotlin/.../configurations/CharacterConfigs.kt` |
| **[NEW] PartySystem** | `core/src/main/kotlin/.../systems/PartySystem.kt` |

## Verification

1. `./gradlew :core:compileKotlin` — must pass after each part
