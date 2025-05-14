package com.github.jacks.roleplayinggame.components

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable

enum class AnimationModel {
    PLAYER, SLIME, CHEST, UNDEFINED;

    val atlasKey : String = this.toString().lowercase()
}

enum class AnimationType {
    IDLETO, IDLESIDE, IDLEAWAY, MOVETO, MOVESIDE, MOVEAWAY, ATTACKTO, ATTACKSIDE, ATTACKAWAY, DEATH, OPEN;

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

    fun nextAnimation(model : AnimationModel, type : AnimationType) {
        this.model = model
        nextAnimation = "${model.atlasKey}/${type.atlasKey}"
    }

    companion object {
        val NO_ANIMATION = ""
    }
}
