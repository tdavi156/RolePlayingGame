package com.github.jacks.roleplayinggame.configurations

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.physics.box2d.BodyDef
import com.github.jacks.roleplayinggame.RolePlayingGame.Companion.UNIT_SCALE
import com.github.jacks.roleplayinggame.components.AnimationModel
import com.github.jacks.roleplayinggame.components.NonPlayerConfiguration
import com.github.jacks.roleplayinggame.components.PlayerConfiguration
import ktx.math.vec2

enum class ConfigurationType {
    UNDEFINED, PLAYER, NON_PLAYER, ENEMY
}

class Configurations {
    companion object {
        fun getConfigurationType(entityName : String) : ConfigurationType {
            return when(entityName) {
                "player"     -> ConfigurationType.PLAYER
                "oldman"     -> ConfigurationType.NON_PLAYER
                "questman"   -> ConfigurationType.NON_PLAYER
                "cleric"     -> ConfigurationType.NON_PLAYER
                "ranger"     -> ConfigurationType.NON_PLAYER
                "sorcerer"   -> ConfigurationType.NON_PLAYER
                "slimeGreen" -> ConfigurationType.ENEMY
                "slimeBlue"  -> ConfigurationType.ENEMY
                "slimeRed"   -> ConfigurationType.ENEMY
                else         -> ConfigurationType.UNDEFINED
            }
        }

        /** Returns the [EnemyConfiguration] for enemy [model], or null if not an enemy. */
        fun getEnemyConfig(model: AnimationModel): EnemyConfiguration? = ENEMY_CONFIGS[model]

        fun getConfiguration(entityName : String) : Any? {
            return when(entityName) {
                "player"     -> PLAYER_CONFIGURATION
                "oldman"     -> OLDMAN_CONFIGURATION
                "cleric"     -> CLERIC_CONFIGURATION
                "ranger"     -> RANGER_CONFIGURATION
                "sorcerer"   -> SORCERER_CONFIGURATION
                "questman"   -> QUESTMAN_CONFIGURATION
                "slimeGreen" -> SLIME_GREEN_ENEMY_CONFIG
                "slimeBlue"  -> SLIME_BLUE_ENEMY_CONFIG
                "slimeRed"   -> SLIME_RED_ENEMY_CONFIG
                else         -> null
            }
        }

        // ── Player ───────────────────────────────────────────────────────────────
        val PLAYER_CONFIGURATION = PlayerConfiguration(
            model        = AnimationModel.PLAYER,
            speedScaling = 1.5f,
            lifeScaling  = 1f,
            attackRange  = 0.75f,
            attackScaling = 1f,
            physicsScaling = vec2(0.3f, 0.3f),
            physicsOffset  = vec2(0f, -10f * UNIT_SCALE),
            color = Color.WHITE,
        )

        // ── Non-player NPCs (no combat stats) ───────────────────────────────────
        val QUESTMAN_CONFIGURATION = NonPlayerConfiguration(
            model          = AnimationModel.OLD_MAN,
            speedScaling   = 0f,
            canAttack      = false,
            canBattle      = false,
            attackScaling  = 0f,
            lifeScaling    = 0f,
            hasAiBehavior  = false,
            physicsScaling = vec2(0.3f, 0.3f),
            physicsOffset  = vec2(0f, -10f * UNIT_SCALE),
            bodyType       = BodyDef.BodyType.StaticBody,
            color          = Color.WHITE,
            dialogId       = DialogId.QUEST_MAN
        )
        val OLDMAN_CONFIGURATION = NonPlayerConfiguration(
            model          = AnimationModel.OLD_MAN,
            speedScaling   = 0f,
            canAttack      = false,
            canBattle      = false,
            attackScaling  = 0f,
            lifeScaling    = 0f,
            hasAiBehavior  = false,
            physicsScaling = vec2(0.3f, 0.3f),
            physicsOffset  = vec2(0f, -10f * UNIT_SCALE),
            bodyType       = BodyDef.BodyType.StaticBody,
            color          = Color.WHITE,
        )
        val CLERIC_CONFIGURATION = NonPlayerConfiguration(
            model          = AnimationModel.OLD_MAN,
            speedScaling   = 0f,
            canAttack      = false,
            canBattle      = false,
            attackScaling  = 0f,
            lifeScaling    = 0f,
            hasAiBehavior  = false,
            physicsScaling = vec2(0.3f, 0.3f),
            physicsOffset  = vec2(0f, -10f * UNIT_SCALE),
            bodyType       = BodyDef.BodyType.StaticBody,
            color          = Color.WHITE,
            dialogId       = DialogId.RECRUIT_CHARACTER_2
        )
        val RANGER_CONFIGURATION = NonPlayerConfiguration(
            model          = AnimationModel.OLD_MAN,
            speedScaling   = 0f,
            canAttack      = false,
            canBattle      = false,
            attackScaling  = 0f,
            lifeScaling    = 0f,
            hasAiBehavior  = false,
            physicsScaling = vec2(0.3f, 0.3f),
            physicsOffset  = vec2(0f, -10f * UNIT_SCALE),
            bodyType       = BodyDef.BodyType.StaticBody,
            color          = Color.WHITE,
            dialogId       = DialogId.RECRUIT_CHARACTER_3
        )
        val SORCERER_CONFIGURATION = NonPlayerConfiguration(
            model          = AnimationModel.OLD_MAN,
            speedScaling   = 0f,
            canAttack      = false,
            canBattle      = false,
            attackScaling  = 0f,
            lifeScaling    = 0f,
            hasAiBehavior  = false,
            physicsScaling = vec2(0.3f, 0.3f),
            physicsOffset  = vec2(0f, -10f * UNIT_SCALE),
            bodyType       = BodyDef.BodyType.StaticBody,
            color          = Color.WHITE,
            dialogId       = DialogId.NO_DIALOG
        )
    }
}
