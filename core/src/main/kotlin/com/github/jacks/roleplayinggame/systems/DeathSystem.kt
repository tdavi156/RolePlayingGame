package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.ai.DefaultState
import com.github.jacks.roleplayinggame.components.AnimationComponent
import com.github.jacks.roleplayinggame.components.DeathComponent
import com.github.jacks.roleplayinggame.components.LifeComponent
import com.github.jacks.roleplayinggame.components.StateComponent
import com.github.jacks.roleplayinggame.events.EntityDeathEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem

@AllOf([DeathComponent::class])
class DeathSystem(
    private val deathComponents : ComponentMapper<DeathComponent>,
    private val lifeComponents : ComponentMapper<LifeComponent>,
    private val animationComponents : ComponentMapper<AnimationComponent>,
    private val stage : Stage
) : IteratingSystem() {

    override fun onTickEntity(entity: Entity) {
        val deathComponent = deathComponents[entity]
        if (deathComponent.respawnTime == 0f) {
            stage.fire(EntityDeathEvent(animationComponents[entity].model))
            world.remove(entity)
            return
        }

        deathComponent.respawnTime -= deltaTime
        if (deathComponent.respawnTime <= 0f) {
            with(lifeComponents[entity]) { health = maxHealth }
            configureEntity(entity) { deathComponents.remove(entity) }
        }
    }
}
