package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.graphics.Color
import com.github.jacks.roleplayinggame.RolePlayingGame.Companion.UNIT_SCALE
import com.github.jacks.roleplayinggame.components.EntityCreationComponent
import com.github.jacks.roleplayinggame.components.NonPlayerEntity
import com.github.jacks.roleplayinggame.components.SpawnerComponent
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.IteratingSystem

@AllOf([SpawnerComponent::class])
class SpawnerSystem(
    private val spawnerComponents : ComponentMapper<SpawnerComponent>
) : IteratingSystem(interval = Fixed(1f)) {

    override fun onTickEntity(entity: Entity) {
        val spawnerComp = spawnerComponents[entity]
        if (spawnerComp.isSpawned) { return }
        if (spawnerComp.currentTime < spawnerComp.spawnTimer) { spawnerComp.currentTime++ }
        if (spawnerComp.currentTime >= spawnerComp.spawnTimer) {
            // the timer has completed and the entity should be spawned
            world.entity {
                add<EntityCreationComponent> {
                    this.entityName = spawnerComp.entityToSpawn
                    this.configurationName = NonPlayerEntity.SLIME.configurationName
                    this.configurationType = NonPlayerEntity.SLIME.configurationType
                    this.location.set(spawnerComp.location.x * UNIT_SCALE, spawnerComp.location.y * UNIT_SCALE)
                    this.color = Color.WHITE
                }
            }
            spawnerComp.currentTime = 0f
            spawnerComp.isSpawned = true
        }
    }
}

// add the configs here for different spawners, and send those in as config files to the entitycreation system
