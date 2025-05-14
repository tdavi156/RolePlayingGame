package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.github.jacks.roleplayinggame.components.LifeComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.DeathComponent
import com.github.jacks.roleplayinggame.components.FloatingTextComponent
import com.github.jacks.roleplayinggame.components.PhysicsComponent
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.NoneOf
import ktx.assets.disposeSafely
import kotlin.math.roundToInt

@AllOf([LifeComponent::class])
@NoneOf([DeathComponent::class])
class LifeSystem(
    private val lifeComponents : ComponentMapper<LifeComponent>,
    private val deathComponents : ComponentMapper<DeathComponent>,
    private val playerComponents : ComponentMapper<PlayerComponent>,
    private val physicsComponents : ComponentMapper<PhysicsComponent>
) : IteratingSystem() {

    private val damageFont = BitmapFont(Gdx.files.internal("assets/fonts/damage.fnt"))
    private val floatingTextStyle = LabelStyle(damageFont, Color.WHITE)

    override fun onTickEntity(entity: Entity) {
        val lifeComponent = lifeComponents[entity]
        lifeComponent.health = (lifeComponent.health + lifeComponent.healthRegeneration * deltaTime).coerceAtMost(lifeComponent.maxHealth)

        if (lifeComponent.takeDamage > 0f) {
            val physicsComponent = physicsComponents[entity]
            lifeComponent.health -= lifeComponent.takeDamage
            damageText(lifeComponent.takeDamage.roundToInt().toString(), physicsComponent.body.position, physicsComponent.size)
            lifeComponent.takeDamage = 0f
        }

        if (lifeComponent.isDead) {
            configureEntity(entity) {
                deathComponents.add(it) {
                    if (it in playerComponents) {
                        respawnTime = 5f
                    }
                }
            }
        }
    }

    private fun damageText(text : String, position : Vector2, size : Vector2) {
        world.entity {
            add<FloatingTextComponent> {
                textStartLocation.set(position.x, position.y - size.y * 0.5f)
                textDuration = 2f
                label = Label(text, floatingTextStyle)
                label.setFontScale(0.75f)
            }
        }
    }

    override fun onDispose() {
        damageFont.disposeSafely()
    }
}
