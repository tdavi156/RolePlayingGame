package com.github.jacks.roleplayinggame.systems

import com.github.jacks.roleplayinggame.components.DeathComponent
import com.github.jacks.roleplayinggame.components.LifeComponent
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem

@AllOf([DeathComponent::class])
class DeathSystem(
    private val deathComponents : ComponentMapper<DeathComponent>,
    private val lifeComponents : ComponentMapper<LifeComponent>
) : IteratingSystem() {

    override fun onTickEntity(entity: Entity) {
        val deathComponent = deathComponents[entity]
        if (deathComponent.respawnTime == 0f) {
            world.remove(entity)
            return
        }

        deathComponent.respawnTime -= deltaTime
        if (deathComponent.respawnTime <= 0f) {
            with(lifeComponents[entity]) {
                health = maxHealth
            }
            configureEntity(entity) {
                deathComponents.remove(entity)
            }
        }
    }
}
