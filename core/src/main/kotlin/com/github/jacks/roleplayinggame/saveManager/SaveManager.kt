package com.github.jacks.roleplayinggame.saveManager

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter
import com.github.jacks.roleplayinggame.configurations.ItemCategory
import com.github.jacks.roleplayinggame.systems.InventorySystem
import com.github.jacks.roleplayinggame.systems.MapSystem
import com.github.jacks.roleplayinggame.systems.PartySystem
import com.github.jacks.roleplayinggame.systems.QuestSystem
import com.github.jacks.roleplayinggame.systems.ResourceSystem
import com.github.quillraven.fleks.World

/**
 * Central save/load manager for all game state.
 *
 * Responsibilities:
 *  - Write `save/game_save.json`  (full game state)
 *  - Write `save/settings.json`   (user preferences — preserved on new game)
 *  - Gather data from ECS systems on save via [gatherAndSave]
 *  - Distribute deserialized data back to systems on load (callers use the returned [GameSaveData])
 *  - Cache the last known save in memory so [systems.SpawnerSystem] can query spawner state
 *    without re-reading the file on every map transition.
 *
 * This class is a plain Kotlin object — NOT a Fleks system. It is constructed in
 * [screens.GameScreen] and injected into the ECS world so any system can receive it
 * as a constructor parameter.
 */
class SaveManager {

    private val json = Json().apply {
        setOutputType(JsonWriter.OutputType.json)  // standard JSON, no LibGDX quirks
        setTypeName(null)                          // don't write "class" fields for known types
        ignoreUnknownFields = true                // forward-compatible — unknown keys are silently ignored
    }

    /** In-memory cache of the most recently loaded or saved game data. */
    private var cachedSave: GameSaveData? = null

    private val gameSaveFile     get() = Gdx.files.local("save/game_save.json")
    private val settingsSaveFile get() = Gdx.files.local("save/settings.json")

    // ── Existence check ───────────────────────────────────────────────────────

    /** Returns true if a save file exists on disk. False means first-time / new game. */
    fun hasSave(): Boolean = gameSaveFile.exists()

    // ── Full game save ─────────────────────────────────────────────────────────

    /**
     * Collect current state from all relevant systems and write to disk.
     * Call this whenever game state changes that needs to be persisted.
     */
    fun gatherAndSave(world: World) {
        saveFull(buildGameSaveData(world))
    }

    /**
     * Write a pre-built [GameSaveData] to disk and update the in-memory cache.
     * Prefer [gatherAndSave] unless you have already assembled the data.
     */
    fun saveFull(data: GameSaveData) {
        gameSaveFile.parent().mkdirs()
        gameSaveFile.writeString(json.toJson(data, GameSaveData::class.java), false)
        cachedSave = data
    }

    /**
     * Load and deserialize the game save from disk.
     * Returns null if no save file exists — callers should treat null as "new game".
     * Updates the in-memory cache on success.
     */
    fun load(): GameSaveData? {
        if (!hasSave()) return null
        return try {
            json.fromJson(GameSaveData::class.java, gameSaveFile).also { cachedSave = it }
        } catch (e: Exception) {
            Gdx.app.error("SaveManager", "Failed to load game save: ${e.message}")
            null
        }
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    fun saveSettings(data: SettingsSaveData) {
        settingsSaveFile.parent().mkdirs()
        settingsSaveFile.writeString(json.toJson(data, SettingsSaveData::class.java), false)
    }

    fun loadSettings(): SettingsSaveData? {
        if (!settingsSaveFile.exists()) return null
        return try {
            json.fromJson(SettingsSaveData::class.java, settingsSaveFile)
        } catch (e: Exception) {
            Gdx.app.error("SaveManager", "Failed to load settings: ${e.message}")
            null
        }
    }

    // ── Spawner lookup ────────────────────────────────────────────────────────

    /**
     * Look up the saved state for a specific spawner from the in-memory cache.
     * Used by [systems.SpawnerSystem] on [events.MapChangeEvent] to restore `isSpawned` / `currentTime`
     * without re-reading the full save file.
     * Returns null if no cached save exists or if this spawner has no saved entry
     * (treat as default unspawned state in that case).
     */
    fun findSpawnerState(spawnerId: Int, mapId: Int): SpawnerEntrySaveData? =
        cachedSave?.map?.spawners?.find { it.spawnerId == spawnerId && it.mapId == mapId }

    // ── Data gathering ────────────────────────────────────────────────────────

    private fun buildGameSaveData(world: World): GameSaveData {
        val partySystem     = world.system<PartySystem>()
        val resourceSystem  = world.system<ResourceSystem>()
        val inventorySystem = world.system<InventorySystem>()
        val questSystem     = world.system<QuestSystem>()
        val mapSystem       = world.system<MapSystem>()

        return GameSaveData(
            party     = buildPartySaveData(partySystem),
            resources = ResourceSaveData(gold = resourceSystem.resources.gold),
            inventory = buildInventorySaveData(inventorySystem),
            quests    = buildQuestList(questSystem),
            map       = MapSaveData(
                currentMap = mapSystem.currentMapName,
                spawners   = mapSystem.collectSpawnerSaveData(),
            ),
        )
    }

    private fun buildPartySaveData(partySystem: PartySystem): PartySaveData {
        val characterList = arrayListOf<CharacterSaveData>()
        partySystem.characterDataMap.forEach { (id, data) ->
            characterList.add(
                CharacterSaveData(
                    characterId             = id,
                    currentLevel            = data.currentLevel,
                    currentEXP              = data.currentEXP,
                    totalEXP                = data.totalEXP,
                    moveSpeed               = data.moveSpeed,
                    currentSkillPoints      = data.currentSkillPoints,
                    totalSkillPoints        = data.totalSkillPoints,
                    currentAbilityPoints    = data.currentAbilityPoints,
                    totalAbilityPoints      = data.totalAbilityPoints,
                    stamina                 = data.stamina,
                    strength                = data.strength,
                    agility                 = data.agility,
                    intelligence            = data.intelligence,
                    wisdom                  = data.wisdom,
                    currentHealth           = data.currentHealth,
                    currentMana             = data.currentMana,
                    unlockedAbilityIdsRaw   = data.unlockedAbilityIds.joinToString(","),
                    equippedHelmet          = data.equippedItems[ItemCategory.HELMET] ?: -1,
                    equippedWeapon          = data.equippedItems[ItemCategory.WEAPON] ?: -1,
                    equippedArmor           = data.equippedItems[ItemCategory.ARMOR]  ?: -1,
                    equippedBoots           = data.equippedItems[ItemCategory.BOOTS]  ?: -1,
                    isUnlocked              = data.isUnlocked,
                )
            )
        }
        return PartySaveData(
            activeCharacterId = partySystem.activeOverworldCharacterId,
            combatSlotsRaw    = partySystem.combatSlots.joinToString(","),
            characters        = characterList,
        )
    }

    private fun buildInventorySaveData(inventorySystem: InventorySystem): InventorySaveData {
        return InventorySaveData(
            equipment    = inventorySystem.equipment.mapTo(arrayListOf())    { ItemEntrySaveData(it.item.id, it.quantity) },
            consumables  = inventorySystem.consumables.mapTo(arrayListOf())  { ItemEntrySaveData(it.item.id, it.quantity) },
            questItems   = inventorySystem.questItems.mapTo(arrayListOf())   { ItemEntrySaveData(it.item.id, it.quantity) },
            enchantments = inventorySystem.enchantments.mapTo(arrayListOf()) { ItemEntrySaveData(it.item.id, it.quantity) },
        )
    }

    private fun buildQuestList(questSystem: QuestSystem): ArrayList<QuestEntrySaveData> {
        val list = arrayListOf<QuestEntrySaveData>()
        questSystem.questStates.forEach { (id, state) ->
            list.add(QuestEntrySaveData(
                questId       = id,
                statusOrdinal = state.status.ordinal,
                progress      = state.progress,
            ))
        }
        return list
    }
}
