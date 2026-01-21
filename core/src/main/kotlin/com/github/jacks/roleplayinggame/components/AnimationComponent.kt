package com.github.jacks.roleplayinggame.components

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable

enum class AnimationModel {
    PLAYER, OLD_MAN, SLIME_GREEN, SLIME_BLUE, SLIME_RED, CHEST, SIGN, UNDEFINED;

    val atlasKey : String = this.toString().replace("_", "").lowercase()
}

enum class AnimationType {
    IDLE, MOVE, ATTACK, DEATH, OPEN;

    val atlasKey : String = this.toString().lowercase()
}

enum class AnimationDirection {
    TO, SIDE, AWAY;

    val atlasKey : String = this.toString().lowercase()
}

data class AnimationComponent(
    var model : AnimationModel = AnimationModel.UNDEFINED,
    var stateTime : Float = 0f,
    var playMode : Animation.PlayMode = Animation.PlayMode.LOOP
) {
    lateinit var animation : Animation<TextureRegionDrawable>
    var nextAnimation : String = NO_ANIMATION

    val isAnimationDone : Boolean
        get() = animation.isAnimationFinished(stateTime)

    fun nextAnimation(type : AnimationType) {
        nextAnimation = "${model.atlasKey}/${type.atlasKey}"
    }

    fun nextAnimation(type : AnimationType, direction : AnimationDirection) {
        nextAnimation = if (needsDirection(type)) {
            "${model.atlasKey}/${type.atlasKey}${direction.atlasKey}"
        } else {
            "${model.atlasKey}/${type.atlasKey}"
        }
    }

    fun nextAnimation(model : AnimationModel, type : AnimationType) {
        this.model = model
        nextAnimation = if (needsDirection(type)) {
            "${model.atlasKey}/${type.atlasKey}to"
        } else {
            "${model.atlasKey}/${type.atlasKey}"
        }
    }

    fun nextAnimation(model : AnimationModel, type : AnimationType, direction : AnimationDirection) {
        this.model = model
        nextAnimation = "${model.atlasKey}/${type.atlasKey}${direction.atlasKey}"
    }

    fun needsDirection(type: AnimationType) : Boolean {
        return type == AnimationType.IDLE || type == AnimationType.MOVE || type == AnimationType.ATTACK
    }

    companion object {
        val NO_ANIMATION = ""
    }
}
