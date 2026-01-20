package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.github.jacks.roleplayinggame.RolePlayingGame.Companion.UNIT_SCALE
import com.github.jacks.roleplayinggame.components.ImageComponent
import com.github.jacks.roleplayinggame.components.ItemComponent
import com.github.jacks.roleplayinggame.components.PhysicsComponent
import com.github.jacks.roleplayinggame.components.PhysicsComponent.Companion.bodyFromImageAndConfiguration
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.SpawnerComponent
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.configurations.Configurations.Companion.PLAYER_CONFIGURATION
import com.github.jacks.roleplayinggame.events.BattleEvent
import com.github.jacks.roleplayinggame.events.BattleMapChangeEvent
import com.github.jacks.roleplayinggame.events.MapChangeEvent
import com.github.jacks.roleplayinggame.events.PortalEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.ui.views.BackgroundView
import com.github.jacks.roleplayinggame.ui.views.FadeInOutView
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.IntervalSystem
import ktx.app.gdxError
import ktx.assets.disposeSafely
import ktx.preferences.flush
import ktx.preferences.get
import ktx.preferences.set
import ktx.tiled.height
import ktx.tiled.id
import ktx.tiled.layer
import ktx.tiled.width
import ktx.tiled.x
import ktx.tiled.y

class MapSystem(
    private val physicsWorld : World,
    private val gameStage : Stage,
    private val statComponents : ComponentMapper<StatComponent>,
    private val physicsComponents : ComponentMapper<PhysicsComponent>,
    private val imageComponents : ComponentMapper<ImageComponent>,
    private val spawnerComponents : ComponentMapper<SpawnerComponent>,
) : IntervalSystem(), EventListener {

    private val preferences : Preferences by lazy { Gdx.app.getPreferences("rolePlayingGamePrefs") }
    private var currentMap : TiledMap? = null
    private val sequence = SequenceAction()
    val fadeInOutView = gameStage.actors.filterIsInstance<FadeInOutView>().first()

    override fun onTick() = Unit

    override fun handle(event: Event): Boolean {
        when(event) {
            is PortalEvent -> {
                sequence.addAction(Actions.fadeIn(1f))
                sequence.addAction(Actions.run {
                    setMap(event.toMap, event.toPortal)
                })
                sequence.addAction(Actions.fadeOut(1f))
                fadeInOutView.addAction(sequence)
                return true
            }
            is BattleEvent -> {
                setBattleMap(event.toMap)
                return true
            }
            else -> return false
        }
    }

    fun setMap(mapName : String) {
        if (currentMap != null) { saveCurrentMapData() }
        currentMap?.disposeSafely()
        world.family(noneOf = arrayOf(PlayerComponent::class, ItemComponent::class)).forEach { world.remove(it) }
        val newMap = TmxMapLoader().load("maps/$mapName.tmx")
        currentMap = newMap
        preferences.flush { this["current_map"] = mapName }

        world.family(allOf = arrayOf(PlayerComponent::class)).forEach { playerEntity ->
            val playerSpawner = newMap.layer("spawners").objects.get("player_spawner")
            val playerImage = imageComponents[playerEntity].image
            playerImage.setPosition(
                playerSpawner.x * UNIT_SCALE - playerImage.width * 0.5f + playerSpawner.width * 0.5f * UNIT_SCALE,
                playerSpawner.y * UNIT_SCALE - playerSpawner.height * 0.5f * UNIT_SCALE
            )
            configureEntity(playerEntity) {
                physicsComponents.remove(it)
                physicsComponents.add(it) {
                    body = bodyFromImageAndConfiguration(
                        physicsWorld,
                        playerImage,
                        PLAYER_CONFIGURATION.bodyType,
                        PLAYER_CONFIGURATION.physicsScaling,
                        PLAYER_CONFIGURATION.physicsOffset)
                }
            }
        }
        gameStage.fire(MapChangeEvent(newMap))
    }

    fun setMap(mapName : String, targetPortalId : Int = -1) {
        if (currentMap != null) { saveCurrentMapData() }
        currentMap?.disposeSafely()
        world.family(noneOf = arrayOf(PlayerComponent::class, ItemComponent::class)).forEach { world.remove(it) }
        val newMap = TmxMapLoader().load("maps/$mapName.tmx")
        currentMap = newMap
        preferences.flush { this["current_map"] = mapName }

        if (targetPortalId != -1) {
            world.family(allOf = arrayOf(PlayerComponent::class)).forEach { playerEntity ->
                val targetPortal = targetPortalById(newMap, targetPortalId)
                val playerImage = imageComponents[playerEntity].image
                playerImage.setPosition(
                    targetPortal.x * UNIT_SCALE - playerImage.width * 0.5f + targetPortal.width * 0.5f * UNIT_SCALE,
                    targetPortal.y * UNIT_SCALE - targetPortal.height * 0.5f * UNIT_SCALE
                )
                configureEntity(playerEntity) {
                    physicsComponents.remove(it)
                    physicsComponents.add(it) {
                        body = bodyFromImageAndConfiguration(
                            physicsWorld,
                            playerImage,
                            PLAYER_CONFIGURATION.bodyType,
                            PLAYER_CONFIGURATION.physicsScaling,
                            PLAYER_CONFIGURATION.physicsOffset)
                    }
                }
            }
        }
        gameStage.fire(MapChangeEvent(newMap))
    }

    fun setBattleMap(mapName : String) {
        if (currentMap != null) { saveCurrentMapData() }
        currentMap?.disposeSafely()
        world.family(noneOf = arrayOf(PlayerComponent::class, ItemComponent::class)).forEach { world.remove(it) }
        val newMap = TmxMapLoader().load("maps/$mapName.tmx")
        currentMap = newMap
        preferences.flush {
            this["previous_map"] = preferences["current_map", "map_1"]
            this["current_map"] = mapName
        }

        world.family(allOf = arrayOf(PlayerComponent::class)).forEach { playerEntity ->
            val playerSpawner = newMap.layer("spawners").objects.get("player_spawner")
            val playerImage = imageComponents[playerEntity].image
            playerImage.setPosition(
                playerSpawner.x * UNIT_SCALE - playerImage.width * 0.5f + playerSpawner.width * 0.5f * UNIT_SCALE,
                playerSpawner.y * UNIT_SCALE - playerSpawner.height * 0.5f * UNIT_SCALE
            )
            configureEntity(playerEntity) {
                physicsComponents.remove(it)
                physicsComponents.add(it) {
                    body = bodyFromImageAndConfiguration(
                        physicsWorld,
                        playerImage,
                        PLAYER_CONFIGURATION.bodyType,
                        PLAYER_CONFIGURATION.physicsScaling,
                        PLAYER_CONFIGURATION.physicsOffset)
                }
            }
        }
        // this time we do need to store player location data so when the battle is over we go back to correct place

        gameStage.fire(BattleMapChangeEvent(newMap))
    }

    private fun targetPortalById(map : TiledMap, portalId : Int) : MapObject {
        return map.layer("portals").objects.first { it.id == portalId }
            ?: gdxError("There is no portal with id: $portalId")
    }

    private fun saveCurrentMapData() {
        world.family(allOf = arrayOf(PlayerComponent::class)).forEach { playerEntity ->
            val statComponent = statComponents[playerEntity]
            preferences.flush {
                // player data such as health, exp, mana, level
                // eventually we need to include the player id when there are multiple characters
                this["player_current_health"] = statComponent.currentHealth
                this["player_current_mana"] = statComponent.currentMana
            }
        }
        world.family(allOf = arrayOf(SpawnerComponent::class)).forEach { spawnerEntity ->
            val spawnerComponent = spawnerComponents[spawnerEntity]
            preferences.flush {
                this["spawner_${spawnerComponent.spawnerId}_map_${spawnerComponent.mapId}_current_time"] = spawnerComponent.currentTime
                this["spawner_${spawnerComponent.spawnerId}_map_${spawnerComponent.mapId}_is_Spawned"] = spawnerComponent.isSpawned
            }
        }
    }

    override fun onDispose() {
        currentMap?.disposeSafely()
    }
}
