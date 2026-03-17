package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.jacks.roleplayinggame.components.InitializeGameComponent
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
                loadSettings()
                loadResources()
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
