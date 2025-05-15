package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.physics.box2d.World
import com.github.jacks.roleplayinggame.components.AnimationComponent
import com.github.jacks.roleplayinggame.components.AttackComponent
import com.github.jacks.roleplayinggame.components.AttackState
import com.github.jacks.roleplayinggame.components.ImageComponent
import com.github.jacks.roleplayinggame.components.LifeComponent
import com.github.jacks.roleplayinggame.components.LootComponent
import com.github.jacks.roleplayinggame.components.PhysicsComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.systems.EntitySpawnSystem.Companion.HIT_BOX_SENSOR
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import ktx.box2d.query
import ktx.math.component1
import ktx.math.component2

@AllOf([AttackComponent::class, PhysicsComponent::class, ImageComponent::class])
class AttackSystem(
    private val attackComponents : ComponentMapper<AttackComponent>,
    private val physicsComponents : ComponentMapper<PhysicsComponent>,
    private val imageComponents : ComponentMapper<ImageComponent>,
    private val lifeComponents : ComponentMapper<LifeComponent>,
    private val lootComponents : ComponentMapper<LootComponent>,
    private val playerComponents : ComponentMapper<PlayerComponent>,
    private val animationComponents : ComponentMapper<AnimationComponent>,
    private val physicsWorld : World
) : IteratingSystem() {

    override fun onTickEntity(entity: Entity) {
        val attackComponent = attackComponents[entity]

        // entity is ready and does not want to attack -> do nothing
        if (attackComponent.isReady && !attackComponent.doAttack) {
            return
        }

        // entity is ready and wants to attack -> start attack
        if (attackComponent.isPrepared && attackComponent.doAttack) {
            attackComponent.doAttack = false
            attackComponent.state = AttackState.ATTACKING
            attackComponent.delay = attackComponent.maxDelay
            return
        }

        // entity is attacking and delay timer is started -> deal damage to nearby enemies
        attackComponent.delay -= deltaTime
        if (attackComponent.delay <= 0f && attackComponent.isAttacking) {
            attackComponent.state = AttackState.DEAL_DAMAGE

            val image = imageComponents[entity].image
            val physicsComponent = physicsComponents[entity]
            val attackLeft = image.flipX
            val (x, y) = physicsComponent.body.position
            val (offX, offY) = physicsComponent.offset
            val (width, height) = physicsComponent.size
            val halfWidth = width * 0.5f
            val halfHeight = height * 0.5f


            /*


            ___
            | |
            ---


             */
            if (attackLeft) {
                AABB_RECT_1.set(
                    x + offX - halfWidth - attackComponent.extraRange,
                    y + offY - halfHeight,
                    x + offX + halfWidth,
                    y + offY + halfHeight
                )
                AABB_RECT_2.set(
                    x + offX - halfWidth - attackComponent.extraRange * 2,
                    y + offY - halfHeight - attackComponent.extraRange,
                    x + offX + halfWidth,
                    y + offY + halfHeight
                )
            } else {
                AABB_RECT_1.set(
                    x + offX - halfWidth,
                    y + offY - halfHeight,
                    x + offX + halfWidth + attackComponent.extraRange,
                    y + offY + halfHeight
                )
                AABB_RECT_2.set(
                    x + offX + halfWidth,
                    y + offY - halfHeight * 1.5f,
                    x + offX + halfWidth + attackComponent.extraRange,
                    y + offY + halfHeight * 1.5f
                )
            }

            physicsWorld.query(AABB_RECT_1.x, AABB_RECT_1.y, AABB_RECT_1.width, AABB_RECT_1.height) { fixture ->
                val fixtureEntity = fixture.entity

                if (fixture.userData != HIT_BOX_SENSOR) {
                    return@query true
                }

                if (fixtureEntity == entity) {
                    return@query true
                }

                // add logic for non-player entities to not damage each other

                configureEntity(fixtureEntity) {
                    lifeComponents.getOrNull(it)?.let { lifeComponent ->
                        lifeComponent.takeDamage += attackComponent.damage
                    }

                    if (entity in playerComponents) {
                        lootComponents.getOrNull(it)?.let { lootComponent ->
                            lootComponent.interactEntity = entity
                        }
                    }
                }
                return@query true
            }

            physicsWorld.query(AABB_RECT_2.x, AABB_RECT_2.y, AABB_RECT_2.width, AABB_RECT_2.height) { fixture ->
                val fixtureEntity = fixture.entity

                if (fixture.userData != HIT_BOX_SENSOR) {
                    return@query true
                }

                if (fixtureEntity == entity) {
                    return@query true
                }

                // add logic for non-player entities to not damage each other

                configureEntity(fixtureEntity) {
                    lifeComponents.getOrNull(it)?.let { lifeComponent ->
                        lifeComponent.takeDamage += attackComponent.damage
                    }

                    if (entity in playerComponents) {
                        lootComponents.getOrNull(it)?.let { lootComponent ->
                            lootComponent.interactEntity = entity
                        }
                    }
                }
                return@query true
            }
        }

        val isDone = animationComponents.getOrNull(entity)?.isAnimationDone ?: true
        if (isDone) {
            attackComponent.state = AttackState.READY
        }
    }

    companion object {
        val AABB_RECT_1 = Rectangle()
        val AABB_RECT_2 = Rectangle()
    }
}
