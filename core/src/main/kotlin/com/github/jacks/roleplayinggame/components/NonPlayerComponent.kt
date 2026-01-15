package com.github.jacks.roleplayinggame.components

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType
import ktx.math.vec2

data class NonPlayerConfiguration(
    val model : AnimationModel = AnimationModel.SLIME,
    val stats : StatComponent = StatComponent(),
    val speedScaling : Float = 1f,
    val canAttack : Boolean = true,
    val attackScaling : Float = 1f,
    val attackDelay : Float = 0.2f,
    val attackRange : Float = 0f,
    val lifeScaling : Float = 0f,
    val aiTreePath : String = "",
    val physicsScaling : Vector2 = vec2(1f, 1f),
    val physicsOffset : Vector2 = vec2(0f, 0f),
    val bodyType : BodyType = BodyType.DynamicBody,
)

class NonPlayerComponent {
}
