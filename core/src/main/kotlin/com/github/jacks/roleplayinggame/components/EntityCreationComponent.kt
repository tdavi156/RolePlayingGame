package com.github.jacks.roleplayinggame.components

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType
import ktx.math.vec2

enum class ConfigurationType {
    UNDEFINED, PLAYER, NON_PLAYER
}

enum class PlayerEntity(
    val configurationType : ConfigurationType = ConfigurationType.PLAYER,
    val configurationName : String = "player",
) {
    PLAYER(ConfigurationType.PLAYER, "player")
}

enum class NonPlayerEntity(
    val configurationType : ConfigurationType = ConfigurationType.NON_PLAYER,
    val configurationName : String = "nonPlayer",
) {
    SLIME(ConfigurationType.NON_PLAYER, "slime")
}

const val DEFAULT_SPEED = 3f
const val DEFAULT_ATTACK_DAMAGE = 1
const val DEFAULT_LIFE = 10

data class SpawnConfiguration(
    val model : AnimationModel,
    val stats : StatComponent = StatComponent(),
    val speedScaling : Float = 1f,
    val canAttack : Boolean = true,
    val attackScaling : Float = 1f,
    val attackDelay : Float = 0.2f,
    val attackRange : Float = 0f,
    val lifeScaling : Float = 0f,
    val lootable : Boolean = false,
    val aiTreePath : String = "",
    val physicsScaling : Vector2 = vec2(1f, 1f),
    val physicsOffset : Vector2 = vec2(0f, 0f),
    val bodyType : BodyType = BodyType.DynamicBody,
    val dialogId : DialogId = DialogId.NONE
)

data class EntityCreationComponent(
    var entityName : String = "",
    var configurationName : String = "",
    var configurationType : ConfigurationType = ConfigurationType.UNDEFINED,
    var prefsName : String = "",
    var location : Vector2 = vec2(),
    var color : Color = Color.WHITE
) {

    private fun getConfigurationTypeFromString(type : String) : ConfigurationType {
        return when(type) {
            "player" -> ConfigurationType.PLAYER
            "non_player" -> ConfigurationType.NON_PLAYER
            else -> ConfigurationType.UNDEFINED
        }
    }
}
