package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.graphics.Color
import com.github.jacks.roleplayinggame.RolePlayingGame.Companion.UNIT_SCALE
import com.github.jacks.roleplayinggame.components.SpawnComponent
import com.github.jacks.roleplayinggame.components.SpawnerComponent
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.IteratingSystem
import ktx.tiled.property
import ktx.tiled.x
import ktx.tiled.y

@AllOf([SpawnerComponent::class])
class SpawnerSystem(
    private val spawnerComponents : ComponentMapper<SpawnerComponent>
) : IteratingSystem(interval = Fixed(1f)) {

    override fun onTickEntity(entity: Entity) {
        val spawnerComp = spawnerComponents[entity]
        if (spawnerComp.isSpawned) { return }
        if (spawnerComp.currentTime < spawnerComp.spawnTimer) { spawnerComp.currentTime ++ }
        if (spawnerComp.currentTime >= spawnerComp.spawnTimer) {
            spawnerComp.currentTime = 0f
            world.entity {
                // refactor spawncomponents to include data referencing the spawner id
                add<SpawnComponent> {
                    this.name = "slime"
                    this.location.set(spawnerComp.location.x * UNIT_SCALE, spawnerComp.location.y * UNIT_SCALE)
                    this.color = Color.WHITE
                }
            }
        }
    }
}
