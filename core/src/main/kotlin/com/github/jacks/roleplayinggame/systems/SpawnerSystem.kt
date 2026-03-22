package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.jacks.roleplayinggame.RolePlayingGame.Companion.UNIT_SCALE
import com.github.jacks.roleplayinggame.components.EntityCreationComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.SpawnerComponent
import com.github.jacks.roleplayinggame.configurations.Configurations.Companion.getConfiguration
import com.github.jacks.roleplayinggame.configurations.Configurations.Companion.getConfigurationType
import com.github.jacks.roleplayinggame.events.MapChangeEvent
import com.github.jacks.roleplayinggame.saveManager.SaveManager
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.IteratingSystem
import ktx.app.gdxError
import ktx.tiled.layer
import ktx.tiled.propertyOrNull
import ktx.tiled.x
import ktx.tiled.y

@AllOf([SpawnerComponent::class])
class SpawnerSystem(
    private val spawnerComponents : ComponentMapper<SpawnerComponent>,
    private val saveManager: SaveManager,
) : IteratingSystem(interval = Fixed(1f)), EventListener {

    override fun onTickEntity(entity: Entity) {
        val spawnerComp = spawnerComponents[entity]
        if (spawnerComp.isSpawned) { return }
        if (spawnerComp.currentTime < spawnerComp.spawnTimer) { spawnerComp.currentTime++ }
        if (spawnerComp.currentTime >= spawnerComp.spawnTimer) {
            // the timer has completed and the entity should be spawned
            world.entity {
                add<EntityCreationComponent> {
                    this.configurationType = getConfigurationType(spawnerComp.entityToSpawn)
                    this.configuration = getConfiguration(spawnerComp.entityToSpawn)
                    this.entityName = spawnerComp.entityToSpawn
                    this.location.set(spawnerComp.location.x * UNIT_SCALE, spawnerComp.location.y * UNIT_SCALE)
                    this.spawnerId = spawnerComp.spawnerId
                    this.spawnerMapId = spawnerComp.mapId
                }
            }
            spawnerComp.currentTime = 0f
            spawnerComp.isSpawned = true
            // Spawn state will be captured on the next gatherAndSave call — no immediate write needed
        }
    }

    override fun handle(event: Event): Boolean {
        when(event) {
            is MapChangeEvent -> {
                val spawnerLayer = event.map.layer("spawners")
                world.family(allOf = arrayOf(SpawnerComponent::class)).forEach { world.remove(it) }
                spawnerLayer.objects.forEach { spawner ->
                    val spawnerId = spawner.propertyOrNull<Int>("id") ?: gdxError("Map Object $spawner has no ID")
                    val mapId = spawner.propertyOrNull<Int>("mapId") ?: gdxError("Map Object $spawner has no Map ID")
                    val entityToSpawn = spawner.propertyOrNull<String>("entityToSpawn") ?: gdxError("Map Object $spawner has no Entity To Spawn")
                    val spawnTimer = spawner.propertyOrNull<Float>("spawnTimer") ?: gdxError("Map Object $spawner has no Spawn Timer")
                    val savedState = saveManager.findSpawnerState(spawnerId, mapId)
                    // For the player spawner, isSpawned tracks whether a player entity currently
                    // exists — NOT the save file value. This prevents the spawner from staying
                    // permanently dormant when loading from a save where the player was alive
                    // (the player entity doesn't exist yet at load time), while also preventing
                    // a duplicate player spawn during normal portal/map transitions where the
                    // player entity already exists in the world.
                    val isPlayerSpawner = entityToSpawn == "player"
                    val playerAlreadyExists = if (isPlayerSpawner) {
                        var exists = false
                        world.family(allOf = arrayOf(PlayerComponent::class)).forEach { exists = true }
                        exists
                    } else false
                    world.entity {
                        add<SpawnerComponent> {
                            this.spawnerId = spawnerId
                            this.mapId = mapId
                            this.entityToSpawn = entityToSpawn
                            this.spawnTimer = spawnTimer
                            this.location.set(spawner.x, spawner.y)
                            this.currentTime = savedState?.currentTime ?: 0f
                            this.isSpawned = if (isPlayerSpawner) playerAlreadyExists else savedState?.isSpawned ?: false
                        }
                    }
                    if ((savedState?.isSpawned ?: false) && !isPlayerSpawner) {
                        world.entity {
                            add<EntityCreationComponent> {
                                this.configurationType = getConfigurationType(entityToSpawn)
                                this.configuration = getConfiguration(entityToSpawn)
                                this.entityName = entityToSpawn
                                this.location.set(spawner.x * UNIT_SCALE, spawner.y * UNIT_SCALE)
                                this.spawnerId = spawnerId
                                this.spawnerMapId = mapId
                            }
                        }
                    }
                }
                return true
            }
            else -> return false
        }
    }
}
