package com.github.jacks.roleplayinggame.systems

import com.github.jacks.roleplayinggame.components.ImageComponent
import com.github.jacks.roleplayinggame.components.MoveComponent
import com.github.jacks.roleplayinggame.components.PhysicsComponent
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import ktx.math.component1
import ktx.math.component2

@AllOf([MoveComponent::class, PhysicsComponent::class])
class MoveSystem (
    private val moveComponents : ComponentMapper<MoveComponent>,
    private val physicsComponents : ComponentMapper<PhysicsComponent>,
    private val imageComponents : ComponentMapper<ImageComponent>
) : IteratingSystem() {

    override fun onTickEntity(entity: Entity) {
        val moveComponent = moveComponents[entity]
        val physicsComponent = physicsComponents[entity]
        val mass = physicsComponent.body.mass
        val (velocityX, velocityY) = physicsComponent.body.linearVelocity
        moveComponent.direction = getEntityDirection(moveComponent.cos, moveComponent.sin)

        if ((moveComponent.cos == 0f && moveComponent.sin == 0f) || moveComponent.isRooted) {
            physicsComponent.impulse.set(
                mass * (0f - velocityX),
                mass * (0f - velocityY)
            )
            return
        }

        physicsComponent.impulse.set(
            mass * (moveComponent.speed * moveComponent.cos - velocityX),
            mass * (moveComponent.speed * moveComponent.sin - velocityY)
        )

        imageComponents.getOrNull(entity)?.let { imageComponent ->
            if (moveComponent.cos != 0f) {
                imageComponent.image.flipX = moveComponent.cos < 0
            }
        }
    }

    private fun getEntityDirection(cos : Float, sin : Float) : String {
        return if (cos == 0f && sin == 1f) {
            "side"
        } else if (cos == 1f && sin == 0f) {
            "away"
        } else {
            "to"
        }
    }
}
