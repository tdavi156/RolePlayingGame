package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.jacks.roleplayinggame.components.InitializeGameComponent
import com.github.jacks.roleplayinggame.configurations.CHARACTER_CONFIGS
import com.github.jacks.roleplayinggame.configurations.CombatAnimationSpeed
import com.github.jacks.roleplayinggame.configurations.ItemCategory
import com.github.jacks.roleplayinggame.configurations.Settings
import com.github.jacks.roleplayinggame.configurations.battleEnchantmentItemById
import com.github.jacks.roleplayinggame.configurations.consumableItemById
import com.github.jacks.roleplayinggame.configurations.equipmentItemById
import com.github.jacks.roleplayinggame.configurations.questItemById
import com.github.jacks.roleplayinggame.events.InitializeGameEvent
import com.github.jacks.roleplayinggame.saveManager.CharacterData
import com.github.jacks.roleplayinggame.saveManager.PartySaveData
import com.github.jacks.roleplayinggame.saveManager.QuestEntrySaveData
import com.github.jacks.roleplayinggame.saveManager.SaveManager
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World

@AllOf([InitializeGameComponent::class])
class InitializeGameSystem(
    private val entityWorld: World,
    private val saveManager: SaveManager,
    private val initializeGameComponents: ComponentMapper<InitializeGameComponent>,
) : IteratingSystem(), EventListener {

    /** Set during handle() so onTickEntity knows which map to load. */
    private var mapToLoad: String = "map_1"

    override fun onTickEntity(entity: Entity) {
        val initializeGameComponent = initializeGameComponents[entity]
        if (!initializeGameComponent.gameInitialized) {
            initializeGameComponent.gameInitialized = true
            entityWorld.system<MapSystem>().setMap(mapToLoad)
        }
        world.family(allOf = arrayOf(InitializeGameComponent::class)).forEach { world.remove(it) }
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is InitializeGameEvent -> {
                val saveData = saveManager.load()
                if (saveData != null) {
                    // Existing save found — restore all game state from disk
                    loadCharacterDataFromSave(saveData.party)
                    loadResourcesFromSave(saveData.resources.gold)
                    loadQuestStateFromSave(saveData.quests)
                    entityWorld.system<InventorySystem>().restoreInventory(saveData.inventory)
                    mapToLoad = saveData.map.currentMap
                } else {
                    // New game — seed defaults and write initial save to disk
                    seedCharacterDefaults()
                    seedStartingInventory()
                    mapToLoad = "map_1"
                    saveManager.gatherAndSave(entityWorld)
                }
                // Settings are loaded independently — they survive a new game
                loadSettingsFromSave()
                world.entity {
                    // eventually there may be a step before this that just loads the main menu
                    // and this doesn't trigger until game start or game loaded
                    add<InitializeGameComponent>()
                }
                return true
            }
            else -> return false
        }
    }

    // ── New game seeding ───────────────────────────────────────────────────────

    private fun seedCharacterDefaults() {
        val partySystem = entityWorld.system<PartySystem>()
        CHARACTER_CONFIGS.forEach { (id, config) ->
            val bs = config.baseStats
            partySystem.characterDataMap[id] = CharacterData(
                characterId             = id,
                characterName           = config.characterName,
                baseMaxHealth           = bs.baseMaxHealth,
                baseMaxMana             = bs.baseMaxMana,
                baseAttackSpeed         = bs.baseAttackSpeed,
                baseAccuracy            = bs.baseAccuracy,
                baseEvasion             = bs.baseEvasion,
                baseAttackDamage        = bs.baseAttackDamage,
                baseAttackDamagePercent = bs.baseAttackDamagePercent,
                baseSpellDamage         = bs.baseSpellDamage,
                baseSpellDamagePercent  = bs.baseSpellDamagePercent,
                baseDefense             = bs.baseDefense,
                baseDefensePercent      = bs.baseDefensePercent,
                baseResistance          = bs.baseResistance,
                baseResistancePercent   = bs.baseResistancePercent,
                moveSpeed               = bs.moveSpeed,
                currentAbilityPoints    = 0,
                isUnlocked              = (id == 1),
            )
        }
        partySystem.combatSlots.clear()
        partySystem.combatSlots.add(1)
        partySystem.activeOverworldCharacterId = 1
    }

    private fun seedStartingInventory() {
        val inv = entityWorld.system<InventorySystem>()
        // 5 equipment items
        inv.addItem(equipmentItemById(1001)!!) // Helmet
        inv.addItem(equipmentItemById(1002)!!) // Sword
        inv.addItem(equipmentItemById(1003)!!) // Big Sword
        inv.addItem(equipmentItemById(1004)!!) // Boots
        inv.addItem(equipmentItemById(1005)!!) // Armor
        // 2 consumables
        inv.addItem(consumableItemById(2001)!!) // Health Potion
        inv.addItem(consumableItemById(2002)!!) // Mega Potion
        // 2 quest items
        inv.addItem(questItemById(3001)!!) // Ancient Scroll
        inv.addItem(questItemById(3002)!!) // Broken Amulet
        // 2 battle enchantments
        inv.addItem(battleEnchantmentItemById(4001)!!) // War Sigil
        inv.addItem(battleEnchantmentItemById(4002)!!) // Iron Ward
    }

    // ── Save restore helpers ───────────────────────────────────────────────────

    private fun loadCharacterDataFromSave(partySave: PartySaveData) {
        val partySystem = entityWorld.system<PartySystem>()
        // Seed defaults first — provides a fallback for any character missing from the save
        // (e.g. a new character config added after the save was written)
        seedCharacterDefaults()
        // Overlay each saved character on top of the defaults
        partySave.characters.forEach { saved ->
            val config = CHARACTER_CONFIGS[saved.characterId] ?: return@forEach
            val bs     = config.baseStats
            val abilities: MutableSet<Int> =
                if (saved.unlockedAbilityIdsRaw.isBlank()) mutableSetOf()
                else saved.unlockedAbilityIdsRaw.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .toMutableSet()
            val data = CharacterData(
                characterId             = saved.characterId,
                characterName           = config.characterName,
                currentLevel            = saved.currentLevel,
                currentEXP              = saved.currentEXP,
                totalEXP                = saved.totalEXP,
                moveSpeed               = saved.moveSpeed,
                currentSkillPoints      = saved.currentSkillPoints,
                totalSkillPoints        = saved.totalSkillPoints,
                currentAbilityPoints    = saved.currentAbilityPoints,
                totalAbilityPoints      = saved.totalAbilityPoints,
                stamina                 = saved.stamina,
                strength                = saved.strength,
                agility                 = saved.agility,
                intelligence            = saved.intelligence,
                wisdom                  = saved.wisdom,
                baseMaxHealth           = bs.baseMaxHealth,
                baseMaxMana             = bs.baseMaxMana,
                baseAttackSpeed         = bs.baseAttackSpeed,
                baseAccuracy            = bs.baseAccuracy,
                baseEvasion             = bs.baseEvasion,
                baseAttackDamage        = bs.baseAttackDamage,
                baseAttackDamagePercent = bs.baseAttackDamagePercent,
                baseSpellDamage         = bs.baseSpellDamage,
                baseSpellDamagePercent  = bs.baseSpellDamagePercent,
                baseDefense             = bs.baseDefense,
                baseDefensePercent      = bs.baseDefensePercent,
                baseResistance          = bs.baseResistance,
                baseResistancePercent   = bs.baseResistancePercent,
                unlockedAbilityIds      = abilities,
                equippedItems           = mutableMapOf(
                    ItemCategory.HELMET to saved.equippedHelmet.takeIf { it >= 0 },
                    ItemCategory.WEAPON to saved.equippedWeapon.takeIf { it >= 0 },
                    ItemCategory.ARMOR  to saved.equippedArmor.takeIf  { it >= 0 },
                    ItemCategory.BOOTS  to saved.equippedBoots.takeIf  { it >= 0 },
                ),
                isUnlocked              = saved.isUnlocked,
            ).also { charData ->
                charData.recalculateDerivedStats()
                charData.currentHealth = saved.currentHealth.coerceIn(0f, charData.maxHealth)
                charData.currentMana   = saved.currentMana.coerceIn(0f, charData.maxMana)
            }
            partySystem.characterDataMap[saved.characterId] = data
        }
        // Restore active character and combat slot order
        partySystem.activeOverworldCharacterId = partySave.activeCharacterId
        partySystem.combatSlots.clear()
        partySave.combatSlotsRaw.split(",").mapNotNull { it.trim().toIntOrNull() }
            .forEach { partySystem.combatSlots.add(it) }
        if (partySystem.combatSlots.isEmpty()) partySystem.combatSlots.add(1)
    }

    private fun loadResourcesFromSave(gold: Int) {
        entityWorld.system<ResourceSystem>().resources.gold = gold
    }

    private fun loadQuestStateFromSave(quests: ArrayList<QuestEntrySaveData>) {
        val questSystem = entityWorld.system<QuestSystem>()
        quests.forEach { entry ->
            val status = QuestStatus.entries[entry.statusOrdinal]
            questSystem.questStates[entry.questId] = QuestState(entry.questId, entry.progress, status)
        }
    }

    private fun loadSettingsFromSave() {
        val settingsData = saveManager.loadSettings() ?: return
        entityWorld.system<SettingsSystem>().settings = Settings(
            masterVolume         = settingsData.masterVolume,
            musicVolume          = settingsData.musicVolume,
            effectsVolume        = settingsData.effectsVolume,
            combatAnimationSpeed = CombatAnimationSpeed.entries[settingsData.combatAnimationSpeedOrdinal],
            autoClearText        = settingsData.autoClearText,
        )
    }
}
