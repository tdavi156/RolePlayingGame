package com.github.jacks.roleplayinggame.components

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType
import ktx.math.vec2

const val DEFAULT_SPEED = 3f
const val DEFAULT_ATTACK_DAMAGE = 1
const val DEFAULT_LIFE = 10

data class SpawnConfiguration(
    val model : AnimationModel,
    val speedScaling : Float = 1f,
    val canAttack : Boolean = true,
    val attackScaling : Float = 1f,
    val attackDelay : Float = 0.2f,
    val attackRange : Float = 0f,
    val lifeScaling : Float = 0f,
    val lootable : Boolean = false,
    val physicsScaling : Vector2 = vec2(1f, 1f),
    val physicsOffset : Vector2 = vec2(0f, 0f),
    val bodyType : BodyType = BodyType.DynamicBody
)

data class SpawnComponent(
    var type : String = "",
    var location : Vector2 = vec2()
) {
}
