package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.TiledComponent
import com.github.jacks.roleplayinggame.events.CollisionDespawnEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem

@AllOf([TiledComponent::class])
class CollisionDespawnSystem (
    private val tiledComponents : ComponentMapper<TiledComponent>,
    private val stage : Stage
) : IteratingSystem() {

    override fun onTickEntity(entity: Entity) {
        with (tiledComponents[entity]) {
            if (nearbyEntities.isEmpty()) {
                stage.fire(CollisionDespawnEvent(cell))
                world.remove(entity)
            }
        }
    }
}
