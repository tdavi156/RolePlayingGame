package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.AnimationComponent
import com.github.jacks.roleplayinggame.components.DeathComponent
import com.github.jacks.roleplayinggame.components.SpawnComponent
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.events.EntityRespawnEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import ktx.log.logger
import ktx.preferences.flush
import ktx.preferences.set

@AllOf([DeathComponent::class])
class DeathSystem(
    private val deathComponents : ComponentMapper<DeathComponent>,
    private val statComponents : ComponentMapper<StatComponent>,
    private val animationComponents : ComponentMapper<AnimationComponent>,
    private val stage : Stage,
) : IteratingSystem() {

    private val preferences : Preferences by lazy { Gdx.app.getPreferences("rolePlayingGamePrefs") }

    override fun onTickEntity(entity: Entity) {
        if (entity !in animationComponents) {
            log.debug { "Entity $entity has no death animation. Removing from the world." }
            preferences.flush {
                this["${statComponents[entity].prefsName}_shouldSpawn"] = false
            }
            world.remove(entity)
            return
        }

        if (animationComponents[entity].isAnimationDone) {
            val deathComponent = deathComponents[entity]
            if (deathComponent.respawnTime == 0f) {
                log.debug { "Entity $entity has a death animation, and the animation is done. Removing from the world." }
                preferences.flush {
                    this["${statComponents[entity].prefsName}_shouldSpawn"] = false
                }
                world.remove(entity)
                return
            }

            deathComponent.respawnTime -= deltaTime
            if (deathComponent.respawnTime <= 0f) {
                log.debug { "Entity $entity has respawned." }
                with(statComponents[entity]) { currentHealth = maxHealth }
                configureEntity(entity) { deathComponents.remove(entity) }
                stage.fire(EntityRespawnEvent(entity))
            }
        }
    }

    companion object {
        private val log = logger<DeathSystem>()
    }
}
