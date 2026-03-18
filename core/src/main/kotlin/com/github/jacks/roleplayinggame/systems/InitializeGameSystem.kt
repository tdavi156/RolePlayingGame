package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.jacks.roleplayinggame.components.InitializeGameComponent
import com.github.jacks.roleplayinggame.configurations.CHARACTER_CONFIGS
import com.github.jacks.roleplayinggame.configurations.CombatAnimationSpeed
import com.github.jacks.roleplayinggame.configurations.Settings
import com.github.jacks.roleplayinggame.configurations.battleEnchantmentItemById
import com.github.jacks.roleplayinggame.configurations.consumableItemById
import com.github.jacks.roleplayinggame.configurations.equipmentItemById
import com.github.jacks.roleplayinggame.configurations.questItemById
import com.github.jacks.roleplayinggame.configurations.resources.Resources
import com.github.jacks.roleplayinggame.events.InitializeGameEvent
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World
import ktx.preferences.flush
import ktx.preferences.get
import ktx.preferences.set

@AllOf([InitializeGameComponent::class])
class InitializeGameSystem(
    private val entityWorld: World,
    private val initializeGameComponents: ComponentMapper<InitializeGameComponent>,
) : IteratingSystem(), EventListener {

    private val preferences: Preferences by lazy { Gdx.app.getPreferences("rolePlayingGamePrefs") }

    override fun onTickEntity(entity: Entity) {
        val initializeGameComponent = initializeGameComponents[entity]
        if (!initializeGameComponent.gameInitialized) {
            initializeGameComponent.gameInitialized = true
            entityWorld.system<MapSystem>().setMap(preferences["current_map", "map_1"])
        }
        world.family(allOf = arrayOf(InitializeGameComponent::class)).forEach { world.remove(it) }
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is InitializeGameEvent -> {
                if (!preferences["is_game_initialized", false]) {
                    preferences.clear()
                    setupPreferences()
                }
                loadCharacterData()
                loadSettings()
                loadResources()
                loadQuestState()
                seedStartingInventory()
                world.entity {
                    // eventually there may be a step before this that just loads the main menu and this doesn't
                    // trigger until game start or game loaded
                    add<InitializeGameComponent>()
                }
                return true
            }
            else -> return false
        }
    }

    private fun loadCharacterData() {
        val partySystem = world.system<PartySystem>()

        // Seed character names from configs so loadCharacterData can read them back
        CHARACTER_CONFIGS.forEach { (id, config) ->
            partySystem.characterDataMap[id] = CharacterData(
                characterId = id,
                characterName = config.characterName,
                currentHp = config.baseStats.maxHp.toFloat(),
                maxHp = config.baseStats.maxHp.toFloat(),
                currentMana = config.baseStats.maxMana.toFloat(),
                maxMana = config.baseStats.maxMana.toFloat(),
                attack = config.baseStats.attack.toFloat(),
                defense = config.baseStats.defense.toFloat(),
                attackSpeed = config.baseStats.attackSpeed,
                moveSpeed = config.baseStats.moveSpeed,
                level = 1,
                exp = 0,
                skillPoints = 0,
                abilityPoints = 3,
                skillPointsInvestedAttack = 0,
                skillPointsInvestedDefense = 0,
                isUnlocked = (id == 1),
            )
        }

        // Try to load saved data for each character; fall back to the default just seeded
        CHARACTER_CONFIGS.keys.forEach { id ->
            val saved = partySystem.loadCharacterData(id)
            if (saved != null) {
                partySystem.characterDataMap[id] = saved
            } else {
                // First launch: persist defaults
                partySystem.saveCharacterData(id)
            }
        }

        // Load active overworld character
        val rawActive = preferences.getInteger(PartySystem.KEY_ACTIVE_CHARACTER, 1)
        partySystem.activeOverworldCharacterId = rawActive

        // Load combat slots
        val rawSlots = preferences.getString(PartySystem.KEY_COMBAT_SLOTS, "1")
        partySystem.combatSlots.clear()
        rawSlots.split(",").mapNotNull { it.trim().toIntOrNull() }.forEach {
            partySystem.combatSlots.add(it)
        }
        if (partySystem.combatSlots.isEmpty()) partySystem.combatSlots.add(1)
    }

    private fun seedStartingInventory() {
        val inv = world.system<InventorySystem>()
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

    private fun setupPreferences() {
        preferences.flush {
            this["is_game_initialized"] = true
            this["current_map"] = "map_1"
        }
    }

    private fun loadQuestState() {
        world.system<QuestSystem>().loadState(preferences)
    }

    private fun loadResources() {
        if (!preferences.contains(Resources.KEY_GOLD)) {
            val defaults = Resources()
            preferences.flush {
                this[Resources.KEY_GOLD] = defaults.gold
            }
        } else {
            world.system<ResourceSystem>().resources = Resources(
                gold = preferences[Resources.KEY_GOLD, 0]
            )
        }
    }

    private fun loadSettings() {
        if (!preferences.contains(Settings.KEY_MASTER_VOLUME)) {
            val defaults = Settings()
            preferences.flush {
                this[Settings.KEY_MASTER_VOLUME] = defaults.masterVolume
                this[Settings.KEY_MUSIC_VOLUME] = defaults.musicVolume
                this[Settings.KEY_EFFECTS_VOLUME] = defaults.effectsVolume
                this[Settings.KEY_COMBAT_ANIMATION_SPEED] = defaults.combatAnimationSpeed.ordinal
                this[Settings.KEY_AUTO_CLEAR_TEXT] = defaults.autoClearText
            }
        } else {
            world.system<SettingsSystem>().settings = Settings(
                masterVolume = preferences[Settings.KEY_MASTER_VOLUME, 100],
                musicVolume = preferences[Settings.KEY_MUSIC_VOLUME, 100],
                effectsVolume = preferences[Settings.KEY_EFFECTS_VOLUME, 100],
                combatAnimationSpeed = CombatAnimationSpeed.entries[preferences[Settings.KEY_COMBAT_ANIMATION_SPEED, 0]],
                autoClearText = preferences[Settings.KEY_AUTO_CLEAR_TEXT, false]
            )
        }
    }
}
