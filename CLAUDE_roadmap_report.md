# Android RPG Game — Feature Roadmap Report

A summary of the 10 features scoped out for the project. Each entry covers the major implementations, any systems that were reworked or refactored, and what new systems were introduced.

---

## Feature 1 — Items, Loot, and Gold Reward System

The first feature lays the economic and item foundation for the game. It introduces a gold currency tracked independently of any character (account-scoped), a flexible item configuration system, and a post-battle reward screen.

**Major implementations:**
- Gold tracked in a new `ResourceSystem` singleton, persisted to prefs
- Items refactored from a rigid `ItemType` enum into a flexible `ItemData` data class config (`Items.kt`), with human-readable IDs and a `Map<StatType, Int>` for stats
- Item loot pools (`ItemPools.kt`) with `rollForDrop()` and `rollRandomItem()` utility functions
- Enemy configurations extended with `expReward`, `goldReward`, `lootPool`, and `lootChance` fields
- Post-battle reward screen built as an overlay inside `BattleView`, showing EXP, gold, and any item drop — battle does not conclude until the player dismisses it

**Reworked:** `ItemComponent` and `ItemModel` refactored to reference items by name key rather than enum.

**Added:** `ResourceSystem`, `RewardViewModel`, `RewardView`, `Items.kt`, `ItemPools.kt`

---

## Feature 2 — Inventory Rework

A full replacement of the existing drag-and-drop inventory with a tabbed, text-based system inspired by the Pokémon Bag layout. This feature also formally separates item types into their own config files and introduces a standalone inventory data structure.

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

## Feature 3 — Shops, Buying and Selling Items

Introduces NPC shop interactions, a shop UI, and full buy/sell functionality built on the item and gold systems from Features 1 and 2.

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

## Feature 4 — Consumable Items in Combat

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

## Feature 5 — Floating Text Refactor and Expansion

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

## Feature 6 — EXP, Level Ups, Skill Points, and Ability Points

Reworks the placeholder EXP/level system into a meaningful progression system and introduces two new point currencies for future character customization.

**Major implementations:**
- EXP formula replaced with soft exponential scaling: `req(n) = n × 50 × 1.15^n` — first 10 thresholds documented in a comment block
- Level up grants: `+10 maxHP`, `+5 maxMana`, `+1 skillPoints`, `+1 abilityPoints` only — all other passive boosts removed
- Skill points and ability points tracked in `StatComponent`, saved as part of character data
- `GainSkillPointEvent` and `GainAbilityPointEvent` are decoupled from level-up — any future source (quests, items) can fire them independently
- `SkillView` fully implemented: stat rows with `[-][pts][+]` controls, resulting value preview, save/cancel confirm flows. Invested points tracked separately from raw stats to preserve investment history for a future respec feature
- `AbilityView` and `AbilityViewModel` created as stubs (ability spending deferred to Feature 7)
- `"LEVEL UP!"` floating text fires on level up following the established pattern from Feature 5

**Reworked:** `StatSystem` extended with EXP formula, level-up logic, and skill point save handling. Existing passive stat boosts on level up removed.

**Added:** `SkillView` (reworked from placeholder), `SkillViewModel` (reworked), `AbilityView` (stub), `AbilityViewModel` (stub)

---

## Feature 7 — Ability Points and Spells in Battle

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

## Feature 8 — Dialog System Refactor and Quest System

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

## Feature 9 — Multiple Enemy Battles

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

## Feature 10 — Multiple Player Characters

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

*End of roadmap report. 10 features scoped, covering economic systems, inventory, shops, combat abilities, dialog, quests, multi-enemy encounters, and a full multi-character party system.*
