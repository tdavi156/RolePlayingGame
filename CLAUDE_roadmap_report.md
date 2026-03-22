# Android RPG Game — Feature Roadmap Report

A summary of all major features scoped out and implemented for the project. Each entry covers the major implementations, any systems that were reworked or refactored, and what new systems were introduced.

---

## Feature 0.1 — LibGDX Tutorial

Initial project setup and framework orientation. Establishes the technical foundation the entire project builds on.

**Major implementations:**
- LibGDX project scaffolded with Gradle, targeting Desktop (lwjgl3) and Android
- KTX extensions integrated for idiomatic Kotlin wrappers over LibGDX APIs
- Fleks ECS wired as the entity/component/system backbone
- Scene2D stage hierarchy established: `gameStage` (FitViewport, world units) and `uiStage` (ScreenViewport, pixels)
- Basic player entity with movement, sprite rendering, and a test map loaded via Tiled

**Added:** Core project structure, `GameScreen`, `RolePlayingGame`, initial system stubs

---

## Feature 0.2 — Map Redesign

Redesigns the game world layout and map infrastructure to support multiple connected zones.

**Major implementations:**
- Multi-map architecture: overworld map, interior maps (house), and a dedicated battle map
- Tiled `.tmx` files drive all map data — entity spawn properties, portal destinations, and collision shapes defined in map data
- `MapSystem` introduced to load and unload maps; portal triggers teleport the player between zones
- `SpawnerSystem` introduced to manage enemy spawn state per map, respecting per-spawner timers

**Added:** `MapSystem`, `SpawnerSystem`, `PortalSystem`, multi-zone `.tmx` map files

---

## Feature 0.3 — Recolor Slime for Enemy Variation

Adds a second enemy type using the existing slime sprite sheet recolored, establishing the pattern for adding new enemy variants.

**Major implementations:**
- Blue slime added as a distinct enemy type with higher stats than the green slime
- Enemy type enum extended; `EnemyConfigurations` defines per-type stat and reward values
- Spawners on existing maps updated to include blue slime spawn points

**Reworked:** `EnemyConfigurations` extended to be the canonical enemy stat registry.

---

## Features 1–7 — Refactor Battle System to Turn Based

A multi-feature effort refactoring the real-time collision-based combat into a full turn-based RPG battle system. This is the largest architectural investment in the project.

**Major implementations:**
- Battle triggered on enemy contact — player and enemy teleported to a dedicated `battle.tmx` arena via `BattleEvent`
- `BattleSystem` drives the complete turn loop: player action selection → animation → damage resolution → enemy AI turn → repeat
- Action buttons: Attack, Escape, Items (stub), Spells (stub)
- HP and mana bars rendered per entity in battle UI; floating hit numbers on damage
- Enemy AI selects attack actions automatically
- Escape attempt with configurable success chance
- Battle end: victory (enemy HP = 0) or defeat (player HP = 0) triggers return to overworld via `BattleEndEvent`
- `BattleView` and `BattleViewModel` built as the battle UI layer following the MVVM pattern
- `LifeSystem` tracks HP changes; `StatSystem` stub owns stat fields on `StatComponent`
- `CharacterInfoView` first introduced to display player stats during overworld

**Added:** `BattleSystem`, `BattleView`, `BattleViewModel`, `LifeSystem`, `StatSystem`, `StatComponent`, `BattleEvent`, `BattleEndEvent`, `BattleTransitionStartEvent`, `BattleEndTransitionStartEvent`

---

## Features 8–9 — Overhaul Character Info UI

Rebuilds the character info panel into a clean MVVM-pattern view and introduces map and menu view infrastructure.

**Major implementations:**
- `CharacterInfoView` reworked with a proper layout: HP bar, mana bar, stat labels, level/EXP display
- `CharacterInfoViewModel` introduced, binding live `StatComponent` values to the view via `propertyNotify`
- Map view added as a placeholder overlay
- Menu view skeleton introduced with a Save button stub
- Fade transition system established in `GameScreen` for smooth map/battle transitions

**Reworked:** `CharacterInfoView` fully rebuilt from placeholder. `GameScreen` extended with fade overlay logic.

**Added:** `CharacterInfoViewModel`, `MapView`, `MenuView`, `FadeInOutView`

---

## Feature 10 — Settings

Adds a fully functional settings panel for audio and gameplay preferences, persisted across sessions.

**Major implementations:**
- `SettingsSystem` singleton owns all runtime settings values: master volume, music volume, effects volume, combat animation speed, auto-clear text flag
- `SettingsViewModel` binds settings to the UI; `save()` persists values to SharedPreferences
- `SettingsView` built with sliders and toggles for each setting
- Settings loaded on startup in `InitializeGameSystem`; defaults written on first launch
- Audio system integration stubbed (volume sliders wired but audio system disabled)

**Added:** `SettingsSystem`, `SettingsViewModel`, `SettingsView`

---

## Feature 11 — Loot and Gold

The first feature laying the economic and item foundation for the game. Introduces a gold currency tracked independently of any character (account-scoped), a flexible item configuration system, and a post-battle reward screen.

**Major implementations:**
- Gold tracked in a new `ResourceSystem` singleton, persisted to prefs
- Items refactored from a rigid `ItemType` enum into a flexible `ItemData` data class config (`Items.kt`), with human-readable IDs and a `Map<StatType, Int>` for stats
- Item loot pools (`ItemPools.kt`) with `rollForDrop()` and `rollRandomItem()` utility functions
- Enemy configurations extended with `expReward`, `goldReward`, `lootPool`, and `lootChance` fields
- Post-battle reward screen built as an overlay inside `BattleView`, showing EXP, gold, and any item drop — battle does not conclude until the player dismisses it

**Reworked:** `ItemComponent` and `ItemModel` refactored to reference items by name key rather than enum.

**Added:** `ResourceSystem`, `RewardViewModel`, `RewardView`, `Items.kt`, `ItemPools.kt`

---

## Feature 12 — Inventory Redesign

A full replacement of the existing drag-and-drop inventory with a tabbed, text-based system inspired by the Pokémon Bag layout. Also formally separates item types into their own config files and introduces a standalone inventory data structure.

**Major implementations:**
- New tabbed inventory UI: Equipment, Consumables, Quest Items, and Battle Enchantments tabs
- Right panel: scrollable item list with quantity badges and a fixed item info panel at the bottom
- Left panel: context-sensitive display — character list for equipment/consumable actions, stubbed displays for quest and enchantment tabs
- Item actions: double-click or Enter selects an item, context switches to character list for equip/use confirmation
- Four item type config files introduced with distinct data structures and ID ranges (1000–4999)
- Item stacking by unique integer ID — same ID increments quantity
- `InventorySystem` singleton replaces `InventoryComponent` as the source of truth for all owned items
- `StatSystem` extended to support direct modification of transient current values (HP/mana) from consumable use

**Reworked:** `ItemComponent`/`InventoryComponent` reduced to equipped item IDs only. Drag-and-drop system formally decoupled from inventory and preserved as a generic UI utility. `Items.kt` renamed to `EquipmentItems.kt`.

**Added:** `InventorySystem`, `InventoryViewModel`, `InventoryRightPanel`, `InventoryLeftPanel`, `InventoryView` (replacement), `ConsumableItems.kt`, `QuestItems.kt`, `BattleEnchantmentItems.kt`, `EquipmentStatType.kt`, `ConsumableStatType.kt`, `BattleEnchantmentStatType.kt`

---

## Feature 13 — Shops and Items

Introduces NPC shop interactions, a shop UI, and full buy/sell functionality built on the item and gold systems from Features 11 and 12.

**Major implementations:**
- Shop interactions triggered via the existing `InteractionSystem` — a new `ShopComponent` on NPC entities routes to the new `ShopSystem`
- `InteractionSystem` fully reworked: directional hitbox detection replaced with circular radius check, clean player/NPC identification, typed component dispatch for all interaction types (Loot, Dialog, Shop)
- Shop UI is a non-fullscreen right-side overlay — overworld remains visible behind it
- Buy mode: item list with prices, unaffordable items faded, quantity selector, insufficient gold popup
- Sell mode: player inventory list with sell prices (`ceil(goldValue / 2)`), unsellable items faded, equipped item sell guard (available sell quantity = owned − equipped across all party members)
- ESC context rollback at every depth; Leave option always present in every tab
- Shop stock is infinite — purchasing never reduces available quantity

**Reworked:** `InteractionSystem` fully reworked (see above).

**Added:** `ShopSystem`, `ShopViewModel`, `ShopView`, `ShopConfigs.kt`, `ShopComponent`

---

## Feature 14 — Integrate Items into Battle

Enables the previously disabled Items button in battle, reusing the existing inventory UI locked to the Consumables tab, and integrates consumable use into the combat turn flow.

**Major implementations:**
- Items button enabled in `BattleView`; opens inventory overlay locked to Consumables tab via an `isCombatMode` flag
- Full stat restriction check: if a character is already at full HP/mana, a warning message is shown and the item is not consumed
- Result message shown after successful item use — turn does not advance until the player dismisses it
- Consumable stat application and turn gating handled via a shared `UseConsumableEvent` with an `isCombatItemUse` flag — stat logic is identical in both combat and overworld contexts
- Color flash animation on item use: `flashColor` field added to `ConsumableItemData` (green for health, blue for mana), reusing the existing hit-flash mechanism

**Reworked:** `UseConsumableEvent` extended with `isCombatItemUse` flag. `InventoryViewModel` extended with `isCombatMode` flag.

**Added:** `CombatInventoryOpenEvent`, `CombatInventoryClosedEvent`, `CombatItemUseDismissedEvent`, `ItemUseFlashEvent`

---

## Feature 15 — Update the FloatingTextSystem

Decouples floating text creation from `LifeSystem` and establishes a clean event-driven pattern for all future floating text triggers.

**Major implementations:**
- `FloatingTextEvent(sourceEntity, text, fontType)` introduced as the single trigger for all floating text — always fired by a system, never by the UI layer
- `FloatingTextSystem` becomes the sole creator of `FloatingTextComponent` entities — all other systems fire events only
- Font loading moved from `LifeSystem` into `Skin.kt` under the `Fonts` enum
- Damage floating text wired in `BattleSystem` concurrent with hit flash
- Consumable use floating text wired in `StatSystem` after stat delta is applied — `StatSystem` has direct entity reference and stat values, keeping the UI layer out of entity concerns
- TODO comments placed in `StatSystem` for future colored fonts (heal green, mana blue)

**Reworked:** `LifeSystem` stripped of all floating text logic and font storage. `FloatingTextSystem` extended to implement `EventListener`.

**Added:** `FloatingTextEvent`

---

## Feature 16 — EXP, Levels, and Skill Point System

Reworks the placeholder EXP/level system into a meaningful progression system and introduces two new point currencies for future character customization.

**Major implementations:**
- EXP formula replaced with soft exponential scaling: `req(n) = n × 50 × 1.15^n` — first 10 thresholds documented in a comment block
- Level up grants: `+10 maxHP`, `+5 maxMana`, `+1 skillPoints`, `+1 abilityPoints` only — all other passive boosts removed
- Skill points and ability points tracked in `StatComponent`, saved as part of character data
- `GainSkillPointEvent` and `GainAbilityPointEvent` are decoupled from level-up — any future source (quests, items) can fire them independently
- `SkillView` fully implemented: stat rows with `[-][pts][+]` controls, resulting value preview, save/cancel confirm flows. Invested points tracked separately from raw stats to preserve investment history for a future respec feature
- `AbilityView` and `AbilityViewModel` created as stubs (ability spending deferred to Feature 17)
- `"LEVEL UP!"` floating text fires on level up following the established pattern from Feature 15

**Reworked:** `StatSystem` extended with EXP formula, level-up logic, and skill point save handling. Existing passive stat boosts on level up removed.

**Added:** `SkillView` (reworked from placeholder), `SkillViewModel` (reworked), `AbilityView` (stub), `AbilityViewModel` (stub)

---

## Feature 17 — Ability Point System

Implements the ability tree system, the `AbilityView` UI for spending ability points, and wires unlocked abilities as usable spells in combat.

**Major implementations:**
- Battle action buttons renamed: `"Flee"` → `"Escape"`, `"Skills"` → `"Spells"`
- Spells button dynamically enabled/disabled based on whether the active character has any skilled abilities
- `AbilityTrees.kt` config introduced: sealed class `AbilityEffect` (`DamageEnemy`, `HealSelf`) — extensible for future effect types without restructuring the config
- `AbilityComponent` on player entity tracks per-character unlock state; persisted to prefs
- `AbilityView` fully implemented: node tree UI with circles and connecting lines, visual states (locked/unlockable/pending/skilled), save/cancel confirm flows matching `SkillView`
- `AbilitySystem` created as a dedicated singleton — owns all unlock logic and `AbilityComponent` mutation; `StatSystem` tracks point counts only
- Spell list panel in `BattleView`: scrollable rows with mana cost, description area, faded unaffordable spells
- Spell cast turn flow mirrors item use: result message shown, turn advances only after dismissal via `SpellCastDismissedEvent`
- 3 ability points seeded at game start for testing (marked with TODO for removal)

**Reworked:** `AbilityView` and `AbilityViewModel` fully implemented from stubs. `StatSystem` responsibility clarified — point counts only, no ability logic.

**Added:** `AbilitySystem`, `AbilityComponent`, `AbilityTrees.kt`, `CastSpellEvent`, `SpellCastDismissedEvent`, `AbilitySkillChangedEvent`

---

## Feature 18 — Update DialogSystem and QuestSystem

Cleans up the dialog architecture and builds a full quest system on top of it, including a recruitable NPC quest, quest tracking, and a `QuestView` UI.

**Major implementations:**
- `DialogId` enum and all dialog flow definitions extracted from `DialogSystem` and `DialogComponent` into a new `DialogConfigurations.kt` — `DialogSystem` becomes a pure state machine with no embedded data
- `Dialog.kt` action functions refactored to fire events only — all side effects delegated to the appropriate systems (no logic in `Dialog.kt`)
- `dialogId` field added to `NonPlayerConfiguration`; `EntityCreationSystem` restored to assign `DialogComponent` on entity creation when `dialogId != NO_DIALOG`
- Dialog UI updated to use new drawables: layered boxes, dynamic option buttons generated per node, text scaling
- `QuestSystem` singleton introduced: tracks quest state (`NOT_STARTED`, `ACTIVE`, `CONDITIONS_MET`, `COMPLETED`), listens for `EnemyKilledEvent` to auto-update progress, handles reward delivery on `CompleteQuestEvent`
- `QuestCondition` as a sealed class — extensible for future condition types (`CollectItem`, `ReachLocation`, etc.)
- `questman` NPC added to `map_1.tmx` with a full 5-node dialog flow covering all quest outcomes
- `QuestView` implemented: two-panel layout (Active / Completed), info panel, reactive updates via `QuestStateChangedEvent`

**Reworked:** `DialogSystem` stripped of all data — pure state machine only. `DialogComponent` simplified to `dialogId` field only. `Dialog.kt` action functions become thin event wrappers.

**Added:** `QuestSystem`, `QuestViewModel` (reworked from stub), `QuestView` (reworked from stub), `DialogConfigurations.kt`, `QuestConfigurations.kt`, `AcceptQuestEvent`, `CompleteQuestEvent`, `QuestStateChangedEvent`

---

## Feature 19 — Multiple Enemy in Battles

Extends the battle system to support multi-enemy encounters with flexible composition, per-kill rewards, dynamic turn order, and enemy targeting UI.

**Major implementations:**
- `BattleCompositions.kt` config: `BattleComp` data class with up to 3 enemy slots; fixed comp registry and per-enemy-type random roll tables
- Hybrid composition resolution: spawner with a non-null `battleCompId` uses a fixed comp; null triggers a random roll — supporting both scripted encounters (bosses, blockers) and varied random encounters
- Mixed-type random comps supported — a green slime encounter can roll a comp containing other enemy types
- **Reward escrow**: EXP awarded immediately per kill; gold and loot held in escrow until full victory — discarded on player defeat. Fully prevents boss item farming exploit
- `EnemyKilledEvent` fires per individual kill mid-combat — quest progress updates in real time
- **Speed-based dynamic turn order**: sorted by `StatComponent.speed` at combat start; rebuilt immediately whenever `CombatSpeedChangedEvent` fires (e.g. a speed buff mid-combat); currently acting entity never displaced
- **Enemy selection UI**: world-space arrow/highlight indicator positioned over the target entity using `PhysicsComponent` coordinates; last selected enemy remembered between turns; auto-advances if selected enemy dies
- Reward screen extended to display combined multi-enemy rewards with dynamic item drop rows

**Reworked:** `BattleSystem` significantly extended — comp resolution, escrow, dynamic turn order, enemy selection. `StatSystem` extended to fire `CombatSpeedChangedEvent` on speed changes during combat.

**Added:** `BattleCompositions.kt`, `CombatSpeedChangedEvent`

---

## Feature 20 — Multiple Player Characters in Battles

The largest and most architecturally significant feature — introduces a full party system with up to 6 characters, overworld character switching, multi-character combat, and a complete decoupling of character data from entity lifecycle.

**Major implementations:**
- `CharacterData` class introduced as the persistent, always-in-memory source of truth for each character's stats, progression, equipment, and ability state — completely decoupled from any entity
- `PartySystem` singleton holds all `CharacterData`, manages party membership, `combatSlots`, and the active overworld character — persists to prefs
- `StatComponent` remains the runtime container for spawned entities — populated from `CharacterData` on spawn, written back in real time. A dirty flag guard prevents cascading save loops
- Only one character entity exists in the overworld at a time — Alt+1–6 hotkeys trigger a character switch with a brief fade transition
- 2–3 recruitable NPCs added to `map_1.tmx` — joining the party permanently removes them from the map
- `CharacterInfoView` fully reworked: left panel character list (all unlocked party members), right panel detail view
- All UIs (inventory, skills, abilities, shop sell guard) updated to read and write via `PartySystem.characterDataMap` — non-active characters can be equipped and modified at any time
- Combat spawns all `combatSlots` characters simultaneously from their `CharacterData`; correct spawn points used per slot count; defeat restores all party members to minimum 1 HP before saving
- Spells button re-evaluated per active turn entity in combat — a character with no abilities always sees it disabled on their turn
- `battle.tmx` extended with 2 additional player spawn points (slots 2 and 3)
- Combat slot assignment is fixed (join order) for now — player-assigned UI deferred to a future feature

**Reworked:** `StatSystem` refactored to write back to `CharacterData` in real time. `CharacterInfoView` fully reworked. `AbilityView` character switcher becomes functional. All inventory/skill/ability UIs updated to source data from `PartySystem`.

**Added:** `PartySystem`, `CharacterConfigs.kt`, `SwitchActiveCharacterEvent`, `AddCharacterToPartyEvent`, `PartyUpdatedEvent`

---

## Feature 21 — New Stat Types

A foundational architectural refactor and expansion. Redesigns how stats are stored and accessed across all entity types, introduces a richer set of stats organized into logical groups, wires skill point investment into derived battle stat effects, and implements proper combat calculations for accuracy/evasion, attack/spell damage, and defense/resistance.

**Major implementations:**
- `StatComponent` redesigned as a **pure reference holder**: holds a single `val stats: StatsProvider` — systems dereference through `stats`, never reading fields directly from `StatComponent`
- `StatsProvider` introduced as a sealed class with two subtypes: `CharacterData` (mutable, persistent, full stat set) and `EnemyStats` (mostly static, only `currentHealth` mutable mid-combat)
- `CharacterData` reorganized into clearly commented stat groups: Overworld Stats, Skill Stats (stamina/strength/agility/intelligence/wisdom), Base Battle Stats (from config, read-only), and Derived Battle Stats (computed at runtime)
- `EnemyConfigurations.kt` extended — each enemy now has a fully populated `EnemyStats` instance with real values for all battle stat fields
- `recalculateDerivedStats()` function added to `CharacterData`: computes all derived stats from base values plus skill investment (e.g. `maxHealth = baseMaxHealth + stamina * 10f`, `attackDamage = baseAttackDamage + strength * 3f`)
- `SkillView` redesigned with 5 investable skill stats (Stamina, Strength, Agility, Intelligence, Wisdom), each showing a derived effect description
- `BattleSystem` gains three combat calculation helpers: `resolveHitChance()` (accuracy − evasion roll for Attack), `resolvePhysicalDamage()` (raw damage × percent − defense × percent, floored), `resolveSpellDamage()` (raw spell damage − resistance, floored)
- Spells bypass accuracy/evasion check entirely; `"Missed!"` floating text shown on Attack misses
- All HP and damage values stored as `Float` internally — `toInt()` applied only at the display boundary

**Reworked:** `StatComponent` fully redesigned. `CharacterData` reorganized and extended. `SkillView` and `SkillViewModel` redesigned for 5 skill stats. `BattleSystem` combat calculations replaced with proper formula-based helpers.

**Added:** `StatsProvider.kt`, `EnemyStats`, `EnemyConfigurations.kt`, `recalculateDerivedStats()`

---

## Feature 22 — Save with Serialization

Removes all `SharedPreferences`-based saving scattered across 8+ systems and replaces it with a central `SaveManager` that serializes game state to JSON files using LibGDX's built-in `Json` serializer. Also fixes the existing gap where `InventorySystem` had no persistence at all.

**Major implementations:**
- New `saveManager/` package: `SaveData.kt` (all `*SaveData` data classes with LibGDX Json-compatible defaults), `SaveManager.kt` (central file I/O), `CharacterData.kt` (moved from `systems/`)
- Two save files: `save/game_save.json` (all game state: party, resources, inventory, quests, map/spawners) and `save/settings.json` (user preferences — preserved across new games)
- `SaveManager` API: `hasSave()`, `gatherAndSave(world)`, `saveFull(data)`, `load()`, `saveSettings(data)`, `loadSettings()`, `findSpawnerState(spawnerId, mapId)` for fast in-memory spawner lookups
- `InitializeGameSystem` fully rewritten: `hasSave()` branches to new-game seeding vs. full state restore; settings loaded on both paths
- `InventorySystem.restoreInventory()` added — resolves the longstanding gap where inventory always reseeded from hardcoded defaults on every boot
- `MapSystem` tracks `currentMapName` directly (updated on every `setMap()`/`setBattleMap()`/`returnToOverworld()`) and exposes `collectSpawnerSaveData()` for `gatherAndSave()`
- `SpawnerSystem` reads initial spawner state from `saveManager.findSpawnerState()` on `MapChangeEvent` instead of prefs
- `SettingsViewModel.save()` writes `settings.json` via `saveManager.saveSettings()`
- `MenuViewModel` save action wired to `saveManager.gatherAndSave(world)`
- All dead prefs code removed from `RolePlayingGame`, `GameScreen`, `EntityCreationSystem`, and `MainGameView`

**Reworked:** `InitializeGameSystem` completely rewritten. `PartySystem`, `ResourceSystem`, `QuestSystem`, `MapSystem`, `SpawnerSystem` all stripped of individual prefs fields and save methods. `StatSystem`, `ShopSystem`, `BattleSystem` call sites updated. `SettingsViewModel` and `MenuViewModel` migrated.

**Added:** `SaveManager`, `SaveData.kt` (all `*SaveData` data classes), `InventorySystem.restoreInventory()`

---

## Feature 23 — Remove Dead Life/Death/Attack Systems

Removed all overworld real-time combat infrastructure that became dead code when turn-based battle replaced direct overworld combat. All five systems/components were still registered and running each tick but processing nothing — `AttackSystem` was permanently disabled, `LifeSystem` ran but `takeDamage` was never set, and `DeathSystem` iterated zero entities. All meaningful death and damage logic already lived exclusively in `BattleSystem`.

**Deleted entirely:**
- `LifeSystem` — ran each tick but processed nothing (`takeDamage` always zero, `isDead` never true in overworld)
- `DeathSystem` — iterated zero entities (`DeathComponent` never added)
- `AttackSystem` — permanently disabled via `overworldDisabledSystems`, never ran
- `LifeComponent` — only `takeDamage` was ever relevant; all other fields (`health`, `maxHealth`, `attack`, `defense`, `healthRegeneration`) were superseded by `StatsProvider` and unused
- `DeathComponent` — never added to any entity

**Cleaned up across 7 files:**
- `Events.kt`: removed 4 dead events — `EntityAttackEvent`, `EntityDeathEvent`, `EntityTakeDamageEvent`, `EntityRespawnEvent`
- `GameScreen.kt`: removed system registrations, `attackMapper`, the post-battle attack state reset block, and `AttackSystem::class` from `overworldDisabledSystems`
- `EntityCreationSystem.kt`: removed all 3 `add<LifeComponent>` blocks and the unused `DEFAULT_LIFE` constant import
- `MainGameViewModel.kt`: removed `lifeComponents` mapper, dead event handlers, and the never-updated `playerLife`/`enemyLife` properties; corresponding dead bindings removed from `MainGameView`
- `AudioSystem.kt`: removed `EntityAttackEvent` and `EntityDeathEvent` handlers (attack/death audio will be re-wired to `BattleSystem` in a future audio feature)
- `AiSystem.kt` / `AiEntity.kt`: removed `@NoneOf([DeathComponent::class])` annotation and unused `DeathComponent`/`LifeComponent` mapper references; logic otherwise preserved intact for future complex battle AI use
- `DebugSystem.kt`: removed `AttackSystem.AABB_RECT` reference used to visualize the old attack hitbox

**Preserved:** `AttackComponent` and all `AiEntity`/`AiSystem` logic retained for future AI development.

---

## Feature 24 — PlayerKeyboardInputProcessor Overhaul

Complete rewrite of `PlayerKeyboardInputProcessor` to unify key mappings, fix longstanding bugs, and eliminate ~130 lines of duplicated open/close boilerplate that had accumulated across 23 incremental features.

**Key mapping changes:**

| Key | Action |
|-----|--------|
| I | Inventory |
| C | Character info |
| K | Skills (was L) |
| L | Abilities (was J) |
| J | Quest log (was Q) |
| M | Map |
| O | Settings (new) |
| ESCAPE | Contextual: close view / go back / open menu |
| E | Interaction |
| W/A/S/D + Arrows | Movement + menu navigation |
| Alt+1–6 | Switch active overworld character |

**Bugs fixed:**

- **CHARACTER view softlock** — The CHARACTER priority block consumed all keys including C and ESCAPE, making it impossible to close the view from the keyboard. Fixed by running view-toggle logic before priority blocks.
- **View switching** — Pressing a view key (K, L, I, etc.) while a different view was already open did nothing. Fixed with a `handleViewToggle()` helper that detects the current view and switches directly, bypassing priority blocks.
- **Settings background leak** — Pressing O to close settings hid the `SettingsView` but left `BackgroundView` visible. Root cause: `SettingsViewModel` fires `SettingsClosedEvent` on `uiStage` while `GameScreen` listens on `gameStage`, so the close event was never received by the background hide logic. Fixed by detecting the "opened from overworld" context (MenuView not visible) directly in the SETTINGS ESCAPE handler.
- **Movement direction tracking** — When two arrow/WASD keys were held simultaneously, releasing one key always snapped direction to the last-released key rather than the remaining held key. Fixed with cross-scheme partner checks on `keyUp` (UP checks if W is still held, W checks if UP is still held, etc.) plus a post-update direction correction pass.

**Architecture introduced:**

- `handleViewToggle(keycode)` — runs before all priority blocks; maps I/C/K/L/J/M/O to their ViewType and handles three cases: (1) no view open → pause and open target, (2) same view open → close gracefully, (3) different view open → force-hide current and open target
- `getViewActor(viewType)` — resolves the `Actor` instance from `uiStage` for any view type
- `openViewActor(viewType)` — shows the actor and fires open events (InventoryOpenEvent, SkillViewOpenEvent, AbilityViewOpenEvent) where required
- `closeViewGracefully(viewType)` — fires cancel/confirm dialogs for SKILL/ABILITY, handles the SETTINGS uiStage/gameStage split, and fires GameResumeEvent for all other views
- `forceHideCurrentView(viewType)` — used when switching views without close confirmation

**Reworked:** `PlayerKeyboardInputProcessor` completely rewritten. `GameScreen.kt` updated to construct and hold `skillViewModel` as a named field (same pattern as `abilityViewModel`) and pass both into the input processor constructor.

**Added:** `skillViewModel` field in `GameScreen`; `handleViewToggle`, `getViewActor`, `openViewActor`, `closeViewGracefully`, `forceHideCurrentView` helpers in `PlayerKeyboardInputProcessor`.

---

---

## Feature 25 — UI Cleanup: Remove 9-Patch Inflation, Flatten Row Highlights, Fix Layouts

A targeted pass across six views and widgets to eliminate a class of layout inflation bugs caused by using 9-patch button drawables in contexts that expect minimal-size backgrounds. Also fixes tab label overlapping in the inventory, description label overflow in the skill view, and full-screen inflation of the dialog box.

**Root cause of the 9-patch inflation class:** LibGDX `NinePatchDrawable.getMinWidth()` / `getMinHeight()` return the NinePatch's total pixel size. When assigned as a `Table.background`, the table's minimum preferred size is forced up to match — overriding explicit `width()` / `height()` constraints and making panels, dialogs, and row highlights balloon to unexpected sizes.

**Row highlight replacement:**
- `Skin.kt`: Added `ROW_HIGHLIGHT` entry to `Drawables` enum. At skin load time a 1×1 `Pixmap` (semi-transparent grey, `RGBA8888`) is turned into a `Texture` → `TextureRegion` and registered in the skin under the `row_highlight` key. `TextureRegion` must be used (not `TextureRegionDrawable`) because `skin.getDrawable()` only checks specific type buckets; it wraps the region in a fresh `TextureRegionDrawable` with zero minimum-size constraints on retrieval. `disposeSkin()` extended to dispose the backing texture.
- `SettingsView`, `InventoryLeftPanel`, `InventoryRightPanel`, `ShopView`: All `BACKGROUND_GREY` usages as row/focused-item backgrounds replaced with `ROW_HIGHLIGHT`.

**Tab and mode highlight replacement:**
- `InventoryRightPanel`: Tab labels (Equipment / Consumables / Quest Items / Enchants) and the action menu highlight (Use / Cancel) previously used `SMALL_WHITE_BGD` label styles whose 9-patch background inflated the right panel. Replaced with direct `label.color` tinting: `Colors.ORANGE` for the active/focused entry, `Color.WHITE` for inactive, `Color(1f,1f,1f,0.35f)` for faded (combat-mode non-consumable tabs).
- `ShopView`: Buy/Sell mode labels and tab labels previously used `SMALL_WHITE_BGD` / `SMALL_GREY_BGD` styles for highlighting. Replaced with `label.color` tinting matching the same pattern. Unaffordable buy items and unsellable sell items changed from `SMALL_GREY_BGD` label style to `Color(0.5f, 0.5f, 0.5f, 1f)` tinting on the name and price labels directly.

**DialogView — full-screen inflation fix:**
- `BROWN_BUTTON_SMALL` buttons in `buttonArea` inflated the inner table's minimum size beyond the explicit `width(200f).height(130f)` constraint, causing the dialog background to fill the entire screen.
- Fix: Added `defaults().minSize(0f)` to the outer `DialogView` table, the inner `table {}` block, and the `buttonArea`'s `defaults().expand()` chain.

**InventoryView — tab label overlap fix:**
- Both left and right panels used `expand().fill()`, giving each exactly half (320px) of the 640px inner table. Four tab labels in 320px (≈80px each) was too narrow for "Consumables" at the 16pt font.
- Fix: Changed left panel to `fill().width(160f)` (fixed width matching its portrait + name + bar content). Right panel keeps `expand().fill()` and now receives the remaining ≈478px, giving each tab ≈119px — ample for all four labels.

**SkillView — description label overflow fix:**
- The five stat description labels (e.g. "+3 Spell Dmg, +5 Mana per point") were constrained to `width(140f)`, far too narrow for the longest strings (~230px at the 16pt font). Labels rendered outside the frame boundary.
- Fix: All five description cell widths increased from `140f` → `200f` (via `replace_all`). The outer table width increased from `460f` → `520f` to absorb the additional width; the expandX stat-name column still receives a comfortable ~230px.

**SkillView — button 9-patch inflation fix:**
- `defaults().minSize(0f)` added to the inner `table { outerCell -> }` block so the `[-]` / `[+]` buttons' 9-patch minimums cannot inflate the stat rows.

**Bugs fixed (separate from Feature 25):**

*SaveManager — player not spawning on load from save:*
- When a save file existed, `SpawnerSystem` restored the player spawner's `isSpawned = true` from disk. `onTickEntity` skips any spawner with `isSpawned = true`, and the immediate-respawn block already excluded `entityToSpawn == "player"` — so no player entity was ever created on load. Camera had nothing to follow.
- Fix in `SpawnerSystem.handle(MapChangeEvent)`: For the player spawner, `isSpawned` is now set based on whether a `PlayerComponent` entity already exists in the world (runtime check) rather than the saved value. This ensures: (a) loading from save with no player entity → spawner fires normally; (b) portal/map transitions where the player entity already exists → spawner stays dormant to prevent duplicates.

*Battle crash on enter:*
- `MapSystem.setBattleMap()` looked up the battle map's player position with `objects.get("player_spawner")` but `map_1_battle_1.tmx` had the object named `player_spawner_1` (numbered for future multi-slot party support). The lookup returned null → NPE on `playerSpawner.x`.
- Fix: Renamed `player_spawner_1` → `player_spawner` in the TMX. `player_spawner_2` and `player_spawner_3` retained for future party member positioning.

**DevConfig — development quality-of-life flag:**
- `DevConfig.kt` (new top-level file): single `const val CLEAR_SAVE_ON_START: Boolean` flag. When `true`, `SaveManager.clearSave()` is called before `InitializeGameEvent` fires in `GameScreen`, deleting `game_save.json` and clearing the in-memory cache so the game always boots into a clean new-game state. Settings file is intentionally preserved. Flip to `false` when testing save/load functionality.

**Reworked:** `InventoryRightPanel`, `ShopView` (color tinting throughout). `InventoryView` (centered layout with fixed left panel). `SkillView` (wider cells). `DialogView` (minSize suppression). `SpawnerSystem` (player spawner runtime check).

**Added:** `Drawables.ROW_HIGHLIGHT` + backing texture in `Skin.kt`. `DevConfig.kt`. `SaveManager.clearSave()`. `.gitignore` entries for `/save/` and `.claude/settings.local.json`.

---

*End of roadmap report. 25 major features scoped and implemented, covering foundational framework setup, map design, turn-based combat, character progression, inventory, shops, quests, multi-enemy encounters, a full multi-character party system, a stat system redesign, a complete save system overhaul, a dead code cleanup of the overworld combat infrastructure, a complete input processor overhaul with unified key mappings and view-switching architecture, and a UI cleanup pass eliminating 9-patch inflation across all major views.*
