package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.jacks.roleplayinggame.RolePlayingGame.Companion.UNIT_SCALE
import com.github.jacks.roleplayinggame.components.AnimationModel
import com.github.jacks.roleplayinggame.components.ConfigurationType
import com.github.jacks.roleplayinggame.components.EntityCreationComponent
import com.github.jacks.roleplayinggame.components.NonPlayerConfiguration
import com.github.jacks.roleplayinggame.components.PlayerConfiguration
import com.github.jacks.roleplayinggame.components.SpawnerComponent
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.events.MapChangeEvent
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.IteratingSystem
import ktx.app.gdxError
import ktx.log.logger
import ktx.math.vec2
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

    private fun getConfigurationType(entityName : String) : ConfigurationType {
        return when(entityName) {
            "player" -> { ConfigurationType.PLAYER }
            "slime" -> { ConfigurationType.NON_PLAYER }
            else -> { return ConfigurationType.UNDEFINED }
        }
    }

    private fun getConfiguration(entityName : String) : Any? {
        return when(entityName) {
            "player" -> { PLAYER_CONFIGURATION }
            "slime" -> { SLIME_CONFIGURATION }
            else -> { return null }
        }
    }

    companion object {
        private val log = logger<SpawnerSystem>()
        val PLAYER_CONFIGURATION = PlayerConfiguration(
            AnimationModel.PLAYER,
            stats = StatComponent(
                currentHealth = 30f,
                maxHealth = 30f,
                attackDamage = 5f,
                defense = 1f,
                moveSpeed = 1f
            ),
            speedScaling = 1.5f,
            lifeScaling = 1f,
            attackRange = 0.75f,
            attackScaling = 1f,
            physicsScaling = vec2(0.3f, 0.3f,),
            physicsOffset = vec2(0f, -10f * UNIT_SCALE),
            color = Color.WHITE,
        )
        val SLIME_CONFIGURATION = NonPlayerConfiguration(
            AnimationModel.SLIME,
            stats = StatComponent(
                currentHealth = 10f,
                maxHealth = 10f,
                attackDamage = 3f,
                defense = 0f,
                moveSpeed = 1f
            ),
            speedScaling = 0.5f,
            lifeScaling = 1f,
            attackRange = 1f,
            attackScaling = 1f,
            physicsScaling = vec2(0.3f, 0.3f),
            physicsOffset = vec2(0f, -2f * UNIT_SCALE),
            aiTreePath = "slimeBehavior.tree",
            color = Color.WHITE,
        )
    }
}
