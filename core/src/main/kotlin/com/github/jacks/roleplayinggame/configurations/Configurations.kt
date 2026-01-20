package com.github.jacks.roleplayinggame.configurations

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.physics.box2d.BodyDef
import com.github.jacks.roleplayinggame.RolePlayingGame.Companion.UNIT_SCALE
import com.github.jacks.roleplayinggame.components.AnimationModel
import com.github.jacks.roleplayinggame.components.NonPlayerConfiguration
import com.github.jacks.roleplayinggame.components.PlayerConfiguration
import com.github.jacks.roleplayinggame.components.StatComponent
import ktx.math.vec2

enum class ConfigurationType {
    UNDEFINED, PLAYER, NON_PLAYER
}

class Configurations {
    companion object {
        fun getConfigurationType(entityName : String) : ConfigurationType {
            return when(entityName) {
                "player" -> { ConfigurationType.PLAYER }
                "oldman" -> { ConfigurationType.NON_PLAYER }
                "slime" -> { ConfigurationType.NON_PLAYER }
                else -> { return ConfigurationType.UNDEFINED }
            }
        }

        fun getConfiguration(entityName : String) : Any? {
            return when(entityName) {
                "player" -> { PLAYER_CONFIGURATION }
                "oldman" -> { NPC_CONFIGURATION }
                "slime" -> { SLIME_CONFIGURATION }
                else -> { return null }
            }
        }

        // player configurations
        val PLAYER_CONFIGURATION = PlayerConfiguration(
            AnimationModel.PLAYER,
            stats = StatComponent(
                currentHealth = 30f,
                maxHealth = 30f,
                attackDamage = 5f,
                defense = 1f,
                moveSpeed = 1f
            ),
            speedScaling = 1.5f,
            lifeScaling = 1f,
            attackRange = 0.75f,
            attackScaling = 1f,
            physicsScaling = vec2(0.3f, 0.3f,),
            physicsOffset = vec2(0f, -10f * UNIT_SCALE),
            color = Color.WHITE,
        )

        // non-player configurations
        val NPC_CONFIGURATION = NonPlayerConfiguration(
            AnimationModel.OLDMAN,
            hasStats = false,
            speedScaling = 0f,
            canAttack = false,
            canBattle = false,
            attackScaling = 0f,
            lifeScaling = 0f,
            hasAiBehavior = false,
            physicsScaling = vec2(0.3f, 0.3f,),
            physicsOffset = vec2(0f, -10f * UNIT_SCALE),
            bodyType = BodyDef.BodyType.StaticBody,
            color = Color.WHITE,
        )

        // enemy configurations
        val SLIME_CONFIGURATION = NonPlayerConfiguration(
            AnimationModel.SLIME,
            stats = StatComponent(
                currentHealth = 10f,
                maxHealth = 10f,
                attackDamage = 3f,
                defense = 0f,
                moveSpeed = 1f
            ),
            speedScaling = 0.5f,
            lifeScaling = 1f,
            attackRange = 1f,
            attackScaling = 1f,
            physicsScaling = vec2(0.3f, 0.3f),
            physicsOffset = vec2(0f, -2f * UNIT_SCALE),
            aiTreePath = "slimeBehavior.tree",
            color = Color.WHITE,
        )
    }
}
