package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.graphics.g2d.Animation
import com.github.jacks.roleplayinggame.components.AnimationComponent
import com.github.jacks.roleplayinggame.components.AnimationType
import com.github.jacks.roleplayinggame.components.LootComponent
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem

@AllOf([LootComponent::class])
class LootSystem(
    private val lootComponents : ComponentMapper<LootComponent>,
    private val animationComponents : ComponentMapper<AnimationComponent>
) : IteratingSystem() {

    override fun onTickEntity(entity: Entity) {
        with(lootComponents[entity]) {
            if (interactEntity == null) {
                return
            }

            configureEntity(entity) { lootComponents.remove(it) }
            animationComponents.getOrNull(entity)?.let { animationComponent ->
                animationComponent.nextAnimation(AnimationType.OPEN)
                animationComponent.playMode = Animation.PlayMode.NORMAL
            }
        }
    }
}
