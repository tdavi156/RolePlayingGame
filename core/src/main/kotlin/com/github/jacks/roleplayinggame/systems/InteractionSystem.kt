package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.DialogComponent
import com.github.jacks.roleplayinggame.components.LootComponent
import com.github.jacks.roleplayinggame.components.PhysicsComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.ShopComponent
import com.github.jacks.roleplayinggame.events.InteractionEvent
import com.github.jacks.roleplayinggame.events.ShopInteractionEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.systems.EntityCreationSystem.Companion.HIT_BOX_SENSOR
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import ktx.box2d.query
import ktx.math.component1
import ktx.math.component2

@AllOf([PlayerComponent::class, PhysicsComponent::class])
class InteractionSystem(
    private val physicsComponents: ComponentMapper<PhysicsComponent>,
    private val dialogComponents: ComponentMapper<DialogComponent>,
    private val lootComponents: ComponentMapper<LootComponent>,
    private val shopComponents: ComponentMapper<ShopComponent>,
    private val physicsWorld: World,
    private val gameStage: Stage,
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
        val (playerX, playerY) = physicsComponent.body.position
        val (offX, offY) = physicsComponent.offset
        val centerX = playerX + offX
        val centerY = playerY + offY

        // Build a square AABB for the broad-phase query, then filter by
        // actual circular distance inside the callback.
        AABB_RECT.set(
            centerX - INTERACTION_RANGE,
            centerY - INTERACTION_RANGE,
            centerX + INTERACTION_RANGE,
            centerY + INTERACTION_RANGE
        )

        physicsWorld.query(AABB_RECT.x, AABB_RECT.y, AABB_RECT.width, AABB_RECT.height) { fixture ->
            if (fixture.userData != HIT_BOX_SENSOR) return@query true
            val fixtureEntity = fixture.body.userData as? Entity ?: return@query true
            if (fixtureEntity == entity) return@query true

            // Circular distance check between player centre and NPC body centre
            val npcPos = fixture.body.position
            val dx = npcPos.x - centerX
            val dy = npcPos.y - centerY
            if (dx * dx + dy * dy > INTERACTION_RANGE * INTERACTION_RANGE) return@query true

            // Typed dispatch — priority: Shop > Dialog > Loot
            val shopComponent = shopComponents.getOrNull(fixtureEntity)
            if (shopComponent != null) {
                gameStage.fire(ShopInteractionEvent(shopComponent.shopId))
                return@query false // stop querying once a shop is found
            }

            dialogComponents.getOrNull(fixtureEntity)?.let { dialogComponent ->
                dialogComponent.interactingEntity = entity
            }
            lootComponents.getOrNull(fixtureEntity)?.let { lootComponent ->
                lootComponent.interactingEntity = entity
            }
            return@query true
        }
    }

    companion object {
        private val AABB_RECT = Rectangle()
        private const val INTERACTION_RANGE = 1.5f
    }
}
