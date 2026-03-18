package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.ui.Fonts
import com.github.jacks.roleplayinggame.components.AnimationComponent
import com.github.jacks.roleplayinggame.components.AnimationType
import com.github.jacks.roleplayinggame.components.LifeComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.DeathComponent
import com.github.jacks.roleplayinggame.components.PhysicsComponent
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.events.EntityDeathEvent
import com.github.jacks.roleplayinggame.events.EntityTakeDamageEvent
import com.github.jacks.roleplayinggame.events.FloatingTextEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.NoneOf
import kotlin.math.roundToInt

@AllOf([LifeComponent::class, StatComponent::class])
@NoneOf([DeathComponent::class])
class LifeSystem(
    private val lifeComponents : ComponentMapper<LifeComponent>,
    private val deathComponents : ComponentMapper<DeathComponent>,
    private val playerComponents : ComponentMapper<PlayerComponent>,
    private val physicsComponents : ComponentMapper<PhysicsComponent>,
    private val animationComponents : ComponentMapper<AnimationComponent>,
    private val statComponents : ComponentMapper<StatComponent>,
    private val gameStage : Stage
) : IteratingSystem() {

    override fun onTickEntity(entity: Entity) {
        val lifeComponent = lifeComponents[entity]
        val statComponent = statComponents[entity]
        statComponent.currentHealth = statComponent.currentHealth.coerceAtMost(statComponent.maxHealth)

        if (lifeComponent.takeDamage > 0f) {
            val damageDealt = (lifeComponent.takeDamage - statComponent.defense).coerceAtLeast(1f)
            statComponent.currentHealth -= damageDealt.coerceAtLeast(0f)
            gameStage.fire(EntityTakeDamageEvent(entity))
            physicsComponents.getOrNull(entity)?.body?.position?.let { pos ->
                gameStage.fire(FloatingTextEvent(pos, damageDealt.roundToInt().toString(), Fonts.DAMAGE))
            }
            lifeComponent.takeDamage = 0f
        }

        if (statComponent.isDead) {
            animationComponents.getOrNull(entity)?.let { animationComponent ->
                gameStage.fire(EntityDeathEvent(animationComponent.model))
                animationComponent.nextAnimation(AnimationType.DEATH)
                animationComponent.playMode = Animation.PlayMode.NORMAL
            }

            configureEntity(entity) {
                deathComponents.add(it) {
                    if (it in playerComponents) {
                        respawnTime = 5f
                    }
                }
            }
        }
    }
}
