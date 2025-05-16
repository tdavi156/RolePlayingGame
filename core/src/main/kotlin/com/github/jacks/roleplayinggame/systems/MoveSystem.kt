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

    /*
    when (keycode) {
                UP -> playerSin = if (Gdx.input.isKeyPressed(DOWN)) -1f else 0f
                W -> playerSin = if (Gdx.input.isKeyPressed(S)) -1f else 0f
                DOWN -> playerSin = if (Gdx.input.isKeyPressed(UP)) 1f else 0f
                S -> playerSin = if (Gdx.input.isKeyPressed(W)) 1f else 0f
                LEFT -> playerCos = if (Gdx.input.isKeyPressed(RIGHT)) 1f else 0f
                A -> playerCos = if (Gdx.input.isKeyPressed(D)) 1f else 0f
                RIGHT -> playerCos = if (Gdx.input.isKeyPressed(LEFT)) -1f else 0f
                D -> playerCos = if (Gdx.input.isKeyPressed(A)) -1f else 0f
            }
     */
    private fun getEntityDirection(cos : Float, sin : Float) : String {
        when (sin) {
            0f -> return "side"
            1f -> return "away"
            -1f -> return "to"
        }
        return "to"
        /*
        return if (sin == 0f && (cos == -1f || cos == 1f)) {
            "side"
        } else if (sin == 1f) {
            "away"
        } else if (sin == -1f) {
            "to"
        } else {
            //previousDirection.toString()
            "to"
        }

         */
    }
}
