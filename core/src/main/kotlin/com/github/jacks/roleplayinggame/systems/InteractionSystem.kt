package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.jacks.roleplayinggame.components.DialogComponent
import com.github.jacks.roleplayinggame.components.LootComponent
import com.github.jacks.roleplayinggame.components.MoveComponent
import com.github.jacks.roleplayinggame.components.PhysicsComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.events.InteractionEvent
import com.github.jacks.roleplayinggame.systems.EntityCreationSystem.Companion.HIT_BOX_SENSOR
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import ktx.box2d.query
import ktx.math.component1
import ktx.math.component2

@AllOf([PlayerComponent::class, PhysicsComponent::class, MoveComponent::class])
class InteractionSystem(
    private val physicsComponents: ComponentMapper<PhysicsComponent>,
    private val moveComponents: ComponentMapper<MoveComponent>,
    private val dialogComponents: ComponentMapper<DialogComponent>,
    private val lootComponents: ComponentMapper<LootComponent>,
    private val physicsWorld: World,
) : IteratingSystem(), EventListener {

    private var interactionRequested = false

    override fun handle(event: Event): Boolean {
        if (event is InteractionEvent) {
            interactionRequested = true
            return true
        }
        return false
    }

    override fun onTickEntity(entity: Entity) {
        if (!interactionRequested) return
        interactionRequested = false

        val physicsComponent = physicsComponents[entity]
        val moveComponent = moveComponents[entity]
        val (x, y) = physicsComponent.body.position
        val (offX, offY) = physicsComponent.offset
        val (width, height) = physicsComponent.size
        val halfWidth = width * 0.5f
        val halfHeight = height * 0.5f

        when {
            moveComponent.direction == "away" -> AABB_RECT.set(
                x + offX - halfWidth * 2f,
                y + offY,
                x + offX + width * 1.5f,
                y + offY + INTERACTION_RANGE
            )
            moveComponent.direction == "to" -> AABB_RECT.set(
                x + offX - halfWidth * 2f,
                y + offY - (halfHeight * 0.25f) - INTERACTION_RANGE,
                x + offX + width * 1.5f,
                y + offY
            )
            moveComponent.cos < 0f -> AABB_RECT.set(
                x + offX - halfWidth - INTERACTION_RANGE,
                y + offY - halfHeight * 2f,
                x + offX + halfWidth,
                y + offY + halfHeight * 0.75f
            )
            else -> AABB_RECT.set(
                x + offX - halfWidth,
                y + offY - halfHeight * 2f,
                x + offX + halfWidth + INTERACTION_RANGE,
                y + offY + halfHeight * 0.75f
            )
        }

        physicsWorld.query(AABB_RECT.x, AABB_RECT.y, AABB_RECT.width, AABB_RECT.height) { fixture ->
            if (fixture.userData != HIT_BOX_SENSOR) return@query true
            val fixtureEntity = fixture.entity
            if (fixtureEntity == entity) return@query true

            configureEntity(fixtureEntity) {
                dialogComponents.getOrNull(it)?.let { dialogComponent ->
                    dialogComponent.interactingEntity = entity
                }
                lootComponents.getOrNull(it)?.let { lootComponent ->
                    lootComponent.interactingEntity = entity
                }
            }
            return@query true
        }
    }

    companion object {
        private val AABB_RECT = Rectangle()
        private const val INTERACTION_RANGE = 1.5f
    }
}
