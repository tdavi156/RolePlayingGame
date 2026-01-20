package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.jacks.roleplayinggame.RolePlayingGame.Companion.UNIT_SCALE
import com.github.jacks.roleplayinggame.components.EntityCreationComponent
import com.github.jacks.roleplayinggame.components.SpawnerComponent
import com.github.jacks.roleplayinggame.configurations.Configurations.Companion.getConfiguration
import com.github.jacks.roleplayinggame.configurations.Configurations.Companion.getConfigurationType
import com.github.jacks.roleplayinggame.events.MapChangeEvent
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.IteratingSystem
import ktx.app.gdxError
import ktx.preferences.flush
import ktx.preferences.get
import ktx.preferences.set
import ktx.tiled.layer
import ktx.tiled.propertyOrNull
import ktx.tiled.x
import ktx.tiled.y

@AllOf([SpawnerComponent::class])
class SpawnerSystem(
    private val spawnerComponents : ComponentMapper<SpawnerComponent>
) : IteratingSystem(interval = Fixed(1f)), EventListener {

    private val preferences : Preferences by lazy { Gdx.app.getPreferences("rolePlayingGamePrefs") }

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
                }
            }
            spawnerComp.currentTime = 0f
            spawnerComp.isSpawned = true
            preferences.flush { this["spawner_${spawnerComp.spawnerId}_map_${spawnerComp.mapId}_is_Spawned"] = spawnerComp.isSpawned }
        }
    }

    override fun handle(event: Event): Boolean {
        when(event) {
            is MapChangeEvent -> {
                val spawnerLayer = event.map.layer("spawners")
                world.family(allOf = arrayOf(SpawnerComponent::class)).forEach { world.remove(it) }
                spawnerLayer.objects.forEach { spawner ->
                    world.entity {
                        add<SpawnerComponent> {
                            this.spawnerId = spawner.propertyOrNull<Int>("id") ?: gdxError("Map Object $spawner has no ID")
                            this.mapId = spawner.propertyOrNull<Int>("mapId") ?: gdxError("Map Object $spawner has no Map ID")
                            this.entityToSpawn = spawner.propertyOrNull<String>("entityToSpawn") ?: gdxError("Map Object $spawner has no Entity To Spawn")
                            this.spawnTimer = spawner.propertyOrNull<Float>("spawnTimer") ?: gdxError("Map Object $spawner has no Spawn Timer")
                            this.location.set(spawner.x, spawner.y)
                            this.currentTime = preferences["spawner_${this.spawnerId}_map_${this.mapId}_current_time", 0f]
                            this.isSpawned = preferences["spawner_${this.spawnerId}_map_${this.mapId}_is_Spawned", false]
                        }
                    }
                }
                world.family(allOf = arrayOf(SpawnerComponent::class)).forEach { spawnerEntity ->
                    val spawnerComp = spawnerComponents[spawnerEntity]
                    if (event.map.propertyOrNull<Int>("mapId") == spawnerComp.mapId
                        && spawnerComp.entityToSpawn != "player"
                        && spawnerComp.isSpawned) {
                        world.entity {
                            add<EntityCreationComponent> {
                                this.configurationType = getConfigurationType(spawnerComp.entityToSpawn)
                                this.configuration = getConfiguration(spawnerComp.entityToSpawn)
                                this.entityName = spawnerComp.entityToSpawn
                                this.location.set(spawnerComp.location.x * UNIT_SCALE, spawnerComp.location.y * UNIT_SCALE)
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
