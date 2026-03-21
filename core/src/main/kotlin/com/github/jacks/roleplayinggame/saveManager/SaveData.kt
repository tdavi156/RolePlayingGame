package com.github.jacks.roleplayinggame.saveManager

/**
 * All serializable data classes for the game save system.
 *
 * Design rules:
 *  - Every field MUST have a default value so LibGDX Json can invoke the no-arg constructor.
 *  - ArrayList<T> is used for lists of complex objects — LibGDX Json reliably deserializes
 *    concrete ArrayList types.
 *  - Int lists (ability IDs, combat slots) are stored as comma-separated Strings to avoid
 *    generic type-erasure issues with LibGDX Json's reflection-based deserializer.
 */

// ── Character ─────────────────────────────────────────────────────────────────

data class CharacterSaveData(
    var characterId:             Int     = 0,
    var currentLevel:            Int     = 1,
    var currentEXP:              Int     = 0,
    var totalEXP:                Int     = 0,
    var moveSpeed:               Float   = 1f,
    var currentSkillPoints:      Int     = 0,
    var totalSkillPoints:        Int     = 0,
    var currentAbilityPoints:    Int     = 0,
    var totalAbilityPoints:      Int     = 0,
    var stamina:                 Int     = 0,
    var strength:                Int     = 0,
    var agility:                 Int     = 0,
    var intelligence:            Int     = 0,
    var wisdom:                  Int     = 0,
    var currentHealth:           Float   = 0f,
    var currentMana:             Float   = 0f,
    /** Comma-separated ability IDs, e.g. "1,3,7". Empty string = none unlocked. */
    var unlockedAbilityIdsRaw:   String  = "",
    var equippedHelmet:          Int     = -1,
    var equippedWeapon:          Int     = -1,
    var equippedArmor:           Int     = -1,
    var equippedBoots:           Int     = -1,
    var isUnlocked:              Boolean = false,
)

// ── Party ─────────────────────────────────────────────────────────────────────

data class PartySaveData(
    var activeCharacterId: Int                          = 1,
    /** Comma-separated character IDs in combat slot order, e.g. "1,2". */
    var combatSlotsRaw:    String                       = "1",
    var characters:        ArrayList<CharacterSaveData> = arrayListOf(),
)

// ── Resources ─────────────────────────────────────────────────────────────────

data class ResourceSaveData(
    var gold: Int = 0,
)

// ── Inventory ─────────────────────────────────────────────────────────────────

data class ItemEntrySaveData(
    var id:       Int = 0,
    var quantity: Int = 0,
)

data class InventorySaveData(
    var equipment:    ArrayList<ItemEntrySaveData> = arrayListOf(),
    var consumables:  ArrayList<ItemEntrySaveData> = arrayListOf(),
    var questItems:   ArrayList<ItemEntrySaveData> = arrayListOf(),
    var enchantments: ArrayList<ItemEntrySaveData> = arrayListOf(),
)

// ── Quests ────────────────────────────────────────────────────────────────────

data class QuestEntrySaveData(
    var questId:       Int = 0,
    var statusOrdinal: Int = 0,
    var progress:      Int = 0,
)

// ── Map / Spawners ────────────────────────────────────────────────────────────

data class SpawnerEntrySaveData(
    var spawnerId:   Int     = 0,
    var mapId:       Int     = 0,
    var isSpawned:   Boolean = false,
    var currentTime: Float   = 0f,
)

data class MapSaveData(
    var currentMap: String                          = "map_1",
    var spawners:   ArrayList<SpawnerEntrySaveData> = arrayListOf(),
)

// ── Root game save ────────────────────────────────────────────────────────────

data class GameSaveData(
    var party:     PartySaveData                 = PartySaveData(),
    var resources: ResourceSaveData              = ResourceSaveData(),
    var inventory: InventorySaveData             = InventorySaveData(),
    var quests:    ArrayList<QuestEntrySaveData> = arrayListOf(),
    var map:       MapSaveData                   = MapSaveData(),
)

// ── Settings (separate file — preserved on new game) ─────────────────────────

data class SettingsSaveData(
    var masterVolume:                Int     = 100,
    var musicVolume:                 Int     = 100,
    var effectsVolume:               Int     = 100,
    var combatAnimationSpeedOrdinal: Int     = 0,
    var autoClearText:               Boolean = false,
)
