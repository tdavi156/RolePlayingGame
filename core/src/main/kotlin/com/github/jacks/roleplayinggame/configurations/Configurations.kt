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
                "questman" -> { ConfigurationType.NON_PLAYER }
                "cleric" -> { ConfigurationType.NON_PLAYER }
                "ranger" -> { ConfigurationType.NON_PLAYER }
                "sorcerer" -> { ConfigurationType.NON_PLAYER }
                "slimeGreen" -> { ConfigurationType.NON_PLAYER }
                "slimeBlue" -> { ConfigurationType.NON_PLAYER }
                "slimeRed" -> { ConfigurationType.NON_PLAYER }
                else -> { return ConfigurationType.UNDEFINED }
            }
        }

        fun getNonPlayerConfig(model: AnimationModel): NonPlayerConfiguration? = when (model) {
            AnimationModel.SLIME_GREEN -> SLIME_GREEN_CONFIGURATION
            AnimationModel.SLIME_BLUE  -> SLIME_BLUE_CONFIGURATION
            AnimationModel.SLIME_RED   -> SLIME_RED_CONFIGURATION
            else -> null
        }

        fun getConfiguration(entityName : String) : Any? {
            return when(entityName) {
                "player" -> { PLAYER_CONFIGURATION }
                "oldman" -> { OLDMAN_CONFIGURATION }
                "cleric" -> { CLERIC_CONFIGURATION }
                "ranger" -> { RANGER_CONFIGURATION }
                "sorcerer" -> { SORCERER_CONFIGURATION }
                "questman" -> { QUESTMAN_CONFIGURATION }
                "slimeGreen" -> { SLIME_GREEN_CONFIGURATION }
                "slimeBlue" -> { SLIME_BLUE_CONFIGURATION }
                "slimeRed" -> { SLIME_RED_CONFIGURATION }
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
        val QUESTMAN_CONFIGURATION = NonPlayerConfiguration(
            AnimationModel.OLD_MAN,
            hasStats = false,
            speedScaling = 0f,
            canAttack = false,
            canBattle = false,
            attackScaling = 0f,
            lifeScaling = 0f,
            hasAiBehavior = false,
            physicsScaling = vec2(0.3f, 0.3f),
            physicsOffset = vec2(0f, -10f * UNIT_SCALE),
            bodyType = BodyDef.BodyType.StaticBody,
            color = Color.WHITE,
            dialogId = DialogId.QUEST_MAN
        )
        val OLDMAN_CONFIGURATION = NonPlayerConfiguration(
            AnimationModel.OLD_MAN,
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
        val CLERIC_CONFIGURATION = NonPlayerConfiguration(
            AnimationModel.OLD_MAN,
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
            dialogId = DialogId.RECRUIT_CHARACTER_2
        )
        val RANGER_CONFIGURATION = NonPlayerConfiguration(
            AnimationModel.OLD_MAN,
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
            dialogId = DialogId.RECRUIT_CHARACTER_3
        )
        val SORCERER_CONFIGURATION = NonPlayerConfiguration(
            AnimationModel.OLD_MAN,
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
            dialogId = DialogId.NO_DIALOG
        )

        // enemy configurations
        val SLIME_GREEN_CONFIGURATION = NonPlayerConfiguration(
            AnimationModel.SLIME_GREEN,
            stats = StatComponent(
                currentHealth = 10f,
                maxHealth = 10f,
                attackDamage = 3f,
                defense = 0f,
                moveSpeed = 1f
            ),
            speedScaling = 0.5f,
            lifeScaling = 1f,
            canAttack = false, // Step 11: battle-capable enemies use turn-based combat, not real-time hitbox attacks
            attackRange = 1f,
            attackScaling = 1f,
            battleMap = "map_1_battle_1",
            xpReward = 10,
            goldReward = 5,
            lootPool = TIER_1_ITEMS,
            lootChance = 25,
            physicsScaling = vec2(0.3f, 0.3f),
            physicsOffset = vec2(0f, -2f * UNIT_SCALE),
            aiTreePath = "slimeBehavior.tree",
            color = Color.WHITE,
        )
        val SLIME_BLUE_CONFIGURATION = NonPlayerConfiguration(
            AnimationModel.SLIME_BLUE,
            stats = StatComponent(
                currentHealth = 20f,
                maxHealth = 20f,
                attackDamage = 5f,
                defense = 1f,
                moveSpeed = 1f
            ),
            speedScaling = 0.5f,
            lifeScaling = 1f,
            canAttack = false, // Step 11: battle-capable enemies use turn-based combat, not real-time hitbox attacks
            attackRange = 1f,
            attackScaling = 1f,
            battleMap = "map_1_battle_1",
            xpReward = 25,
            goldReward = 10,
            lootPool = TIER_1_ITEMS,
            lootChance = 25,
            physicsScaling = vec2(0.3f, 0.3f),
            physicsOffset = vec2(0f, -2f * UNIT_SCALE),
            aiTreePath = "slimeBehavior.tree",
            color = Color.WHITE,
        )
        val SLIME_RED_CONFIGURATION = NonPlayerConfiguration(
            AnimationModel.SLIME_RED,
            stats = StatComponent(
                currentHealth = 30f,
                maxHealth = 30f,
                attackDamage = 10f,
                defense = 3f,
                moveSpeed = 1f
            ),
            speedScaling = 0.5f,
            lifeScaling = 1f,
            canAttack = false, // Step 11: battle-capable enemies use turn-based combat, not real-time hitbox attacks
            attackRange = 1f,
            attackScaling = 1f,
            battleMap = "map_1_battle_1",
            xpReward = 50,
            goldReward = 20,
            lootPool = TIER_2_ITEMS,
            lootChance = 50,
            physicsScaling = vec2(0.3f, 0.3f),
            physicsOffset = vec2(0f, -2f * UNIT_SCALE),
            aiTreePath = "slimeBehavior.tree",
            color = Color.WHITE,
        )
    }
}
