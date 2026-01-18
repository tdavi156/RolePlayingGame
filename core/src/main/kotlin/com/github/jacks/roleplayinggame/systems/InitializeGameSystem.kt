package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.jacks.roleplayinggame.components.InitializeGameComponent
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
    private val entityWorld : World,
    private val initializeGameComponents : ComponentMapper<InitializeGameComponent>
) : IteratingSystem(), EventListener {

    private val preferences : Preferences by lazy { Gdx.app.getPreferences("rolePlayingGamePrefs") }

    override fun onTickEntity(entity: Entity) {
        val initializeGameComponent = initializeGameComponents[entity]
        if (!initializeGameComponent.gameInitialized) {
            initializeGameComponent.gameInitialized = true
            entityWorld.system<MapSystem>().setMap(preferences["current_map", "map_1"])
        }
        world.family(allOf = arrayOf(InitializeGameComponent::class)).forEach { world.remove(it) }
    }

    override fun handle(event: Event): Boolean {
        when(event) {
            is InitializeGameEvent -> {
                if (!preferences["is_game_initialized", false]) {
                    preferences.clear()
                    setupPreferences()
                }
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

    private fun setupPreferences() {
        preferences.flush {
            this["is_game_initialized"] = true
            this["current_map"] = "map_1"
        }
    }
}
