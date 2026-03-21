package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.github.jacks.roleplayinggame.RolePlayingGame.Companion.UNIT_SCALE
import com.github.jacks.roleplayinggame.saveManager.SpawnerEntrySaveData
import com.github.jacks.roleplayinggame.components.BattleComponent
import com.github.jacks.roleplayinggame.components.ImageComponent
import com.github.jacks.roleplayinggame.components.PhysicsComponent
import com.github.jacks.roleplayinggame.components.PhysicsComponent.Companion.bodyFromImageAndConfiguration
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.SpawnerComponent
import com.github.jacks.roleplayinggame.configurations.Configurations.Companion.PLAYER_CONFIGURATION
import com.github.jacks.roleplayinggame.events.BattleEndEvent
import com.github.jacks.roleplayinggame.events.BattleEvent
import com.github.jacks.roleplayinggame.events.BattleMapChangeEvent
import com.github.jacks.roleplayinggame.events.MapChangeEvent
import com.github.jacks.roleplayinggame.events.PortalEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.ui.views.FadeInOutView
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.IntervalSystem
import com.github.quillraven.fleks.Qualifier
import com.github.jacks.roleplayinggame.saveManager.SaveManager
import ktx.app.gdxError
import ktx.assets.disposeSafely
import ktx.tiled.height
import ktx.tiled.id
import ktx.tiled.layer
import ktx.tiled.width
import ktx.tiled.x
import ktx.tiled.y

class MapSystem(
    private val physicsWorld : World,
    private val gameStage : Stage,
    private val physicsComponents : ComponentMapper<PhysicsComponent>,
    private val imageComponents : ComponentMapper<ImageComponent>,
    private val spawnerComponents : ComponentMapper<SpawnerComponent>,
    private val battleComponents : ComponentMapper<BattleComponent>,
    private val playerComponents : ComponentMapper<PlayerComponent>,
    private val saveManager: SaveManager,
) : IntervalSystem(), EventListener {

    private var currentMap : TiledMap? = null
    // Overworld state saved when entering a battle map
    private var preBattleMapName: String? = null
    private var preBattlePlayerX: Float = 0f
    private var preBattlePlayerY: Float = 0f

    /** Name of the map currently loaded. Updated on every map transition. Used by SaveManager. */
    var currentMapName: String = "map_1"
        private set

    /**
     * Collects the current spawner state from all active [SpawnerComponent] entities.
     * Called by [SaveManager.gatherAndSave] to capture spawner persistence data.
     */
    fun collectSpawnerSaveData(): ArrayList<SpawnerEntrySaveData> {
        val list = arrayListOf<SpawnerEntrySaveData>()
        world.family(allOf = arrayOf(SpawnerComponent::class)).forEach { spawnerEntity ->
            val sc = spawnerComponents[spawnerEntity]
            list.add(SpawnerEntrySaveData(
                spawnerId   = sc.spawnerId,
                mapId       = sc.mapId,
                isSpawned   = sc.isSpawned,
                currentTime = sc.currentTime,
            ))
        }
        return list
    }

    override fun onTick() = Unit

    override fun handle(event: Event): Boolean {
        when(event) {
            is PortalEvent -> {
                setMap(event.toMap, event.toPortal)
                return true
            }
            is BattleEvent -> {
                // Save overworld map name and player position before switching
                preBattleMapName = currentMapName
                world.family(allOf = arrayOf(PlayerComponent::class)).forEach { playerEntity ->
                    val playerImage = imageComponents[playerEntity].image
                    preBattlePlayerX = playerImage.x
                    preBattlePlayerY = playerImage.y
                }
                setBattleMap(battleComponents[event.enemy].toMap)
                return true
            }
            is BattleEndEvent -> {
                returnToOverworld()
                return true
            }
            else -> return false
        }
    }

    fun setMap(mapName : String, targetPortalId : Int = -1) {
        if (currentMap != null) { saveManager.gatherAndSave(world) }
        val newMap = TmxMapLoader().load("maps/$mapName.tmx")
        currentMap?.disposeSafely()
        currentMap = newMap
        currentMapName = mapName
        world.family(noneOf = arrayOf(PlayerComponent::class)).forEach { world.remove(it) }

        val mapObject = if (targetPortalId != -1) {
            targetPortalById(newMap, targetPortalId)
        } else {
            newMap.layer("spawners").objects.get("player_spawner")
        }

        world.family(allOf = arrayOf(PlayerComponent::class)).forEach { playerEntity ->
            val playerImage = imageComponents[playerEntity].image
            playerImage.setPosition(
                mapObject.x * UNIT_SCALE - playerImage.width * 0.5f + mapObject.width * 0.5f * UNIT_SCALE,
                mapObject.y * UNIT_SCALE - mapObject.height * 0.5f * UNIT_SCALE
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

    fun setBattleMap(mapName : String) {
        if (currentMap != null) { saveManager.gatherAndSave(world) }
        currentMap?.disposeSafely()
        world.family(noneOf = arrayOf(PlayerComponent::class)).forEach { world.remove(it) }
        val newMap = TmxMapLoader().load("maps/$mapName.tmx")
        currentMap = newMap
        currentMapName = mapName

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
        gameStage.fire(BattleMapChangeEvent(newMap))
    }

    private fun returnToOverworld() {
        val mapName = preBattleMapName ?: "map_1"
        currentMap?.disposeSafely()
        world.family(noneOf = arrayOf(PlayerComponent::class)).forEach { world.remove(it) }
        val newMap = TmxMapLoader().load("maps/$mapName.tmx")
        currentMap = newMap
        currentMapName = mapName

        world.family(allOf = arrayOf(PlayerComponent::class)).forEach { playerEntity ->
            val playerImage = imageComponents[playerEntity].image
            playerImage.setPosition(preBattlePlayerX, preBattlePlayerY)
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
        preBattleMapName = null
        gameStage.fire(MapChangeEvent(newMap))
    }

    private fun targetPortalById(map : TiledMap, portalId : Int) : MapObject {
        return map.layer("portals").objects.first { it.id == portalId }
            ?: gdxError("There is no portal with id: $portalId")
    }

    override fun onDispose() {
        currentMap?.disposeSafely()
    }
}
