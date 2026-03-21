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
| `systems/BattleSystem.kt` | Owns all combat turn logic, animation sequencing, and damage application. Source of all combat calculations going forward. |
| `systems/StatSystem.kt` | Owns EXP, level-up logic, and skill point save handling. Extended here to trigger full stat recalculation on skill point investment. |
| `components/StatComponent.kt` | Redesigned — now a pure reference holder pointing to a `StatsProvider` sealed class instance. |
| `ui/views/SkillView.kt` | Redesigned — now shows all 5 investable skill stats with their derived battle stat effects. |
| `configurations/CharacterConfigs.kt` | Extended — each character config now defines base BattleStats values. |
| `configurations/EnemyConfigurations.kt` | Extended — each enemy config now defines an `EnemyStats` instance with real BattleStats values. |

---

## Next Feature

# Feature 11 — Stat System Redesign and Combat Calculation Overhaul

## Context

This feature is a foundational architectural refactor and expansion. It redesigns how stats are stored and accessed across all entity types, introduces a richer set of stats organized into logical groups, wires skill point investment into derived battle stat effects, and implements proper combat calculations for accuracy/evasion, attack/spell damage, and defense/resistance. It does not introduce new gameplay screens — it makes the existing combat and progression systems significantly more complete and consistent.

### Key Architectural Decisions
- `StatComponent` becomes a **pure reference holder**: it holds a single `val stats: StatsProvider` and nothing else stat-related. Systems never read raw fields from `StatComponent` directly — they always dereference through `stats`.
- `StatsProvider` is a **sealed class** with two subtypes: `CharacterData` (mutable, persistent, full stat set) and `EnemyStats` (mostly static, only `currentHealth` is mutable mid-combat).
- **Stat calculation approach**: Full recalculation from all sources when skill points are invested. Additive/subtractive deltas for temporary combat effects (buffs, debuffs). This keeps persistent progression clean while keeping transient combat effects lightweight.
- **HP and all damage values are stored as `Float` internally.** The UI always displays floored `Int` values. No rounding occurs mid-calculation — only at the display boundary.
- `expRewardValue` remains on `EnemyConfiguration`, not on `EnemyStats`.

---

## Part 1 — StatComponent Redesign and StatsProvider Sealed Class

Create `components/StatsProvider.kt` (or define inline in `StatComponent.kt`):
- Define `sealed class StatsProvider`
- `CharacterData` (existing class, moved/extended in Part 2) becomes a subtype: `data class CharacterData(...) : StatsProvider()`
- `EnemyStats` (new) becomes a subtype: `data class EnemyStats(...) : StatsProvider()`

`EnemyStats` fields (all `Float` unless noted, grouped with comments):
```
// BattleStats
maxHealth: Float,
var currentHealth: Float,         // mutable — changes during combat only
maxMana: Float = 0f,
var currentMana: Float = 0f,
attackSpeed: Float = 1f,
accuracy: Float = 0.9f,           // default for all enemies per design decision
evasion: Float = 0f,
attackDamage: Float = 0f,
attackDamagePercent: Float = 1f,
spellDamage: Float = 0f,
spellDamagePercent: Float = 1f,
defense: Float = 0f,
defensePercent: Float = 1f,
resistance: Float = 0f,
resistancePercent: Float = 1f
```

Redesign `StatComponent`:
- Remove all existing stat fields
- Add single field: `val stats: StatsProvider`
- No other fields or logic — this is a pure reference holder

Update all existing systems that read `StatComponent` fields directly (e.g. `statComponent.hp`, `statComponent.speed`, etc.) to dereference through `statComponent.stats`. Cast to `CharacterData` or `EnemyStats` only where type-specific behavior is required (e.g. prefs save — only `CharacterData` is ever persisted). For common reads shared by both types, use extension functions or properties defined on `StatsProvider`.

Update `EnemyConfigurations.kt`:
- Each enemy config now includes a fully populated `EnemyStats` instance with real values (not placeholder defaults)
- Remove any now-redundant individual stat fields that were previously on the config directly and are covered by `EnemyStats`
- `expRewardValue`, `goldReward`, `lootPool`, `lootChance` remain on `EnemyConfiguration` — they are config concerns, not runtime stat concerns

---

## Part 2 — CharacterData Stat Group Redesign

Redesign `CharacterData` (location: `systems/PartySystem.kt` or extracted to its own file if not already) to extend `StatsProvider` and organize all fields into clearly commented groups. Field order and grouping must match exactly:

```kotlin
data class CharacterData(
    val id: String,

    // ---- Overworld Stats ----
    var currentLevel: Int = 1,
    var currentEXP: Int = 0,
    var totalEXP: Int = 0,
    var moveSpeed: Float = 1f,
    var currentSkillPoints: Int = 0,
    var totalSkillPoints: Int = 0,
    var currentAbilityPoints: Int = 0,
    var totalAbilityPoints: Int = 0,

    // ---- Skill Stats (invested points — affect BattleStats via recalculation) ----
    var stamina: Int = 0,
    var strength: Int = 0,
    var agility: Int = 0,
    var intelligence: Int = 0,
    var wisdom: Int = 0,

    // ---- Base Battle Stats (sourced from CharacterConfigs.kt — do not modify directly) ----
    val baseMaxHealth: Float = 0f,
    val baseMaxMana: Float = 0f,
    val baseAttackSpeed: Float = 0f,
    val baseAccuracy: Float = 1f,
    val baseEvasion: Float = 0f,
    val baseAttackDamage: Float = 0f,
    val baseAttackDamagePercent: Float = 1f,
    val baseSpellDamage: Float = 0f,
    val baseSpellDamagePercent: Float = 1f,
    val baseDefense: Float = 0f,
    val baseDefensePercent: Float = 1f,
    val baseResistance: Float = 0f,
    val baseResistancePercent: Float = 1f,

    // ---- Derived Battle Stats (computed from base + skill investment — read these at runtime) ----
    var maxHealth: Float = baseMaxHealth,
    var currentHealth: Float = maxHealth,
    var maxMana: Float = baseMaxMana,
    var currentMana: Float = maxMana,
    var attackSpeed: Float = baseAttackSpeed,
    var accuracy: Float = baseAccuracy,
    var evasion: Float = baseEvasion,
    var attackDamage: Float = baseAttackDamage,
    var attackDamagePercent: Float = baseAttackDamagePercent,
    var spellDamage: Float = baseSpellDamage,
    var spellDamagePercent: Float = baseSpellDamagePercent,
    var defense: Float = baseDefense,
    var defensePercent: Float = baseDefensePercent,
    var resistance: Float = baseResistance,
    var resistancePercent: Float = baseResistancePercent,

    // ---- Ability / Equipment state (unchanged from Feature 10) ----
    // ... existing fields remain here
) : StatsProvider()
```

Update `CharacterConfigs.kt`:
- Each character config now passes all `base*` BattleStats values when constructing `CharacterData`
- All player characters default to `baseAccuracy = 1f`, `baseEvasion = 0f` per design decision
- Populate reasonable starting values per character (can be low — these will grow through skill investment and equipment)

---

## Part 3 — Skill Point Investment and Derived Stat Recalculation

Add a `recalculateDerivedStats()` function to `CharacterData` (or as an extension in `PartySystem`):
- Called whenever skill points are invested — never called for temporary combat deltas
- Computes each derived BattleStats field from its `base*` counterpart plus all applicable skill contributions:
  - `maxHealth = baseMaxHealth + (stamina * 10f)`
  - `attackDamage = baseAttackDamage + (strength * 3f)`
  - `accuracy = baseAccuracy + (agility * 0.05f)`
  - `evasion = baseEvasion + (agility * 0.05f)`
  - `attackSpeed = baseAttackSpeed + (agility * 1f)`
  - `spellDamage = baseSpellDamage + (intelligence * 3f)`
  - `maxMana = baseMaxMana + (intelligence * 5f) + (wisdom * 3f)`
  - `resistance = baseResistance + (wisdom * 2f)`
- After recalculating `maxHealth` and `maxMana`, clamp `currentHealth` and `currentMana` to not exceed the new maximums
- Add a comment noting that equipment stat bonuses will be added as additional terms in this same function in a future feature — the recalc pattern is intentionally designed to accommodate them

Modify `StatSystem.kt`:
- After skill point investment is saved (existing logic), call `recalculateDerivedStats()` on the affected `CharacterData`
- No other changes to `StatSystem` — EXP and level-up logic unchanged

---

## Part 4 — SkillView Redesign

Redesign `ui/views/SkillView.kt`:
- Replace the existing 2-skill stub layout with 5 rows: **Stamina**, **Strength**, **Agility**, **Intelligence**, **Wisdom**
- Each row displays:
  - Skill name
  - Current invested points (e.g. `3`)
  - `[-][pts][+]` investment controls (matching existing pattern)
  - A sub-label showing the derived effect preview (e.g. `"Stamina: +10 Max HP per point"`) — static description text, not a live calculated value
- Save/Cancel confirm flow matches existing `SkillView` pattern exactly
- On Save: committed skill point changes are applied to `CharacterData`, then `recalculateDerivedStats()` is called
- Remove all references to the old 2-skill fields

Update `SkillViewModel.kt`:
- Replace old skill observable properties with five new ones: `stamina`, `strength`, `agility`, `intelligence`, `wisdom` — each `by propertyNotify(0)`
- Pending investment tracking follows the same pattern as before (track deltas separately, apply on save)

---

## Part 5 — Combat Calculation: Accuracy and Evasion

Modify `BattleSystem.kt`:
- Add a private helper function `resolveHitChance(attacker: StatsProvider, defender: StatsProvider): Boolean`:
  - `hitChance = attacker.accuracy - defender.evasion` (clamped to 0f..1f)
  - Returns `true` if a random roll `[0f, 1f)` is less than `hitChance`
- Apply this check **only** for the basic Attack action — spells always bypass this check entirely and always land
- If the check returns `false` (miss):
  - Fire `FloatingTextEvent` on the defending entity with text `"Missed!"` using the existing floating text font
  - Skip all damage calculation and application for that attack
  - Battle log shows a miss message (e.g. `"[Attacker] missed!"`) instead of a damage message
  - Turn still advances normally after the miss

---

## Part 6 — Combat Calculation: Attack Damage and Defense

Modify `BattleSystem.kt`:
- Add a private helper function `resolvePhysicalDamage(attacker: StatsProvider, defender: StatsProvider): Float`:
  - Step 1 — raw damage: `rawDamage = attacker.attackDamage * attacker.attackDamagePercent`
  - Step 2 — apply defense: `finalDamage = rawDamage - (defender.defense * defender.defensePercent)`
  - Step 3 — clamp: `finalDamage = max(0f, finalDamage)`
  - Returns `finalDamage` as `Float` — never rounded here
- Apply this function for the basic Attack action after a successful hit check (Part 5)
- Apply damage to `currentHealth` as a `Float` — no rounding on the stat itself
- Display damage in floating text and battle log as `finalDamage.toInt()` (floor via Kotlin's `toInt()`)
- Add a comment: `// TODO: If finalDamage == 0f after defense reduction, consider showing "Blocked!" message instead of "0" in a future update`

---

## Part 7 — Combat Calculation: Spell Damage and Resistance

Modify `BattleSystem.kt`:
- Add a private helper function `resolveSpellDamage(caster: StatsProvider, defender: StatsProvider): Float`:
  - Step 1 — raw damage: `rawDamage = caster.spellDamage * caster.spellDamagePercent`
  - Step 2 — apply resistance: `finalDamage = rawDamage - (defender.resistance * defender.resistancePercent)`
  - Step 3 — clamp: `finalDamage = max(0f, finalDamage)`
  - Returns `finalDamage` as `Float`
- Apply this function for any spell action tagged as a damage spell (i.e. `AbilityEffect.DamageEnemy`) — healing spells (`AbilityEffect.HealSelf`) bypass this entirely and apply their heal value directly
- Accuracy/evasion check is **not** applied to spells — they always attempt damage resolution
- Display and float text follow the same floor-display pattern as Part 6

---

## Implementation Order

1. **Part 1** — Redesign `StatComponent` as a pure reference holder. Create `StatsProvider` sealed class with `EnemyStats` subtype. Migrate all systems reading `StatComponent` fields to dereference through `stats`. Update all enemy configs with real `EnemyStats` values. Compile must pass.
2. **Part 2** — Redesign `CharacterData` to extend `StatsProvider`, organized into Overworld / Skill / Base Battle / Derived Battle stat groups. Update `CharacterConfigs.kt` with per-character base values. Compile must pass.
3. **Part 3** — Add `recalculateDerivedStats()` to `CharacterData`. Wire it into `StatSystem` after skill point save. Compile must pass.
4. **Part 4** — Redesign `SkillView` and `SkillViewModel` with the 5 new investable skill stats. Wire Save to call `recalculateDerivedStats()`. Compile must pass.
5. **Part 5** — Add `resolveHitChance()` helper to `BattleSystem`. Wire accuracy/evasion check into the Attack action flow. Wire `"Missed!"` floating text and battle log message on miss. Compile must pass.
6. **Part 6** — Add `resolvePhysicalDamage()` helper to `BattleSystem`. Replace existing hardcoded attack damage logic with the new calculation. Wire floor-display for damage values. Compile must pass.
7. **Part 7** — Add `resolveSpellDamage()` helper to `BattleSystem`. Replace existing spell damage logic for `DamageEnemy` spells with the new calculation. Compile must pass.

---

## Key Files Reference

| File | Path |
|------|------|
| GameScreen | `core/src/main/kotlin/.../screens/GameScreen.kt` |
| StatComponent | `core/src/main/kotlin/.../components/StatComponent.kt` |
| StatsProvider | `core/src/main/kotlin/.../components/StatsProvider.kt` |
| CharacterData / PartySystem | `core/src/main/kotlin/.../systems/PartySystem.kt` |
| StatSystem | `core/src/main/kotlin/.../systems/StatSystem.kt` |
| BattleSystem | `core/src/main/kotlin/.../systems/BattleSystem.kt` |
| CharacterConfigs | `core/src/main/kotlin/.../configurations/CharacterConfigs.kt` |
| EnemyConfigurations | `core/src/main/kotlin/.../configurations/EnemyConfigurations.kt` |
| SkillView | `core/src/main/kotlin/.../ui/views/SkillView.kt` |
| SkillViewModel | `core/src/main/kotlin/.../ui/viewmodels/SkillViewModel.kt` |
| **[NEW] StatsProvider** | `core/src/main/kotlin/.../components/StatsProvider.kt` |

## Verification

1. `./gradlew :core:compileKotlin` — must pass after each part
2. After Part 3: invest a skill point in `stamina` in-game — verify `maxHealth` increases by `10f` on the character sheet
3. After Part 5: confirm Attack actions against a 0% evasion enemy always hit; confirm `"Missed!"` floating text appears when evasion exceeds accuracy
4. After Part 6: confirm attack damage is floored in display but stored as Float; confirm defense reduces damage correctly including the 0-floor clamp
5. After Part 7: confirm damage spells use `spellDamage` path and resistance reduction; confirm heal spells are unaffected
