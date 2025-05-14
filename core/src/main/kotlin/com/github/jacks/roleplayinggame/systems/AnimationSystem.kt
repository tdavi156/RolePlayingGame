package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.github.jacks.roleplayinggame.components.AnimationComponent
import com.github.jacks.roleplayinggame.components.AnimationComponent.Companion.NO_ANIMATION
import com.github.jacks.roleplayinggame.components.ImageComponent
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import ktx.app.gdxError
import ktx.collections.map
import ktx.log.logger

@AllOf([AnimationComponent::class, ImageComponent::class])
class AnimationSystem(
    private val textureAtlas : TextureAtlas,
    private val animationComponents : ComponentMapper<AnimationComponent>,
    private val imageComponents : ComponentMapper<ImageComponent>
) : IteratingSystem(){

    private val cachedAnimations = mutableMapOf<String, Animation<TextureRegionDrawable>>()

    override fun onTickEntity(entity: Entity) {
        val animationComponent = animationComponents[entity]

        if (animationComponent.nextAnimation == NO_ANIMATION) {
            animationComponent.stateTime += deltaTime
        } else {
            animationComponent.animation = animation(animationComponent.nextAnimation)
            animationComponent.stateTime = 0f
            animationComponent.nextAnimation = NO_ANIMATION
        }

        animationComponent.animation.playMode = animationComponent.playMode
        imageComponents[entity].image.drawable = animationComponent.animation.getKeyFrame(animationComponent.stateTime)
    }

    private fun animation(animationKeyPath : String) : Animation<TextureRegionDrawable> {
        return cachedAnimations.getOrPut(animationKeyPath) {
            log.debug { "New Animation is created for '$animationKeyPath'" }
            val regions = textureAtlas.findRegions(animationKeyPath)
            if (regions.isEmpty) {
                gdxError("There are no texture regions for $animationKeyPath")
            }
            Animation(DEFAULT_FRAME_DURATION, regions.map { TextureRegionDrawable(it) })
        }
    }

    companion object {
        private val log = logger<AnimationSystem>()
        private const val DEFAULT_FRAME_DURATION = 1/8f
    }
}
