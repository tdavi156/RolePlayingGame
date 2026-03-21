package com.github.jacks.roleplayinggame.configurations

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.github.jacks.roleplayinggame.RolePlayingGame.Companion.UNIT_SCALE
import com.github.jacks.roleplayinggame.components.AnimationModel
import com.github.jacks.roleplayinggame.components.EnemyStats
import ktx.math.vec2

/**
 * Configuration for a battling enemy type.
 * [stats] is the template — each spawned entity gets a fresh copy.
 * Reward fields (xpReward, goldReward, lootPool, lootChance) are config concerns, not runtime stat concerns.
 */
data class EnemyConfiguration(
    val animationModel: AnimationModel,
    val stats: EnemyStats,
    // ── Overworld ──
    val speedScaling: Float = 0.5f,
    val lifeScaling: Float = 1f,
    val canAttack: Boolean = false,
    val attackDelay: Float = 0.2f,
    val attackRange: Float = 1f,
    val attackScaling: Float = 1f,
    val hasAiBehavior: Boolean = true,
    val aiTreePath: String = "",
    val physicsScaling: Vector2 = vec2(0.3f, 0.3f),
    val physicsOffset: Vector2 = vec2(0f, 0f),
    val color: Color = Color.WHITE,
    // ── Battle ──
    val battleMap: String = "",
    val battleCompId: Int? = null,
    // ── Rewards ──
    val xpReward: Int = 0,
    val goldReward: Int = 0,
    val lootPool: List<String>? = null,
    val lootChance: Int = 0,
)

// ── Enemy configs ─────────────────────────────────────────────────────────────

val SLIME_GREEN_ENEMY_CONFIG = EnemyConfiguration(
    animationModel = AnimationModel.SLIME_GREEN,
    stats = EnemyStats(
        maxHealth = 10f,
        currentHealth = 10f,
        attackDamage = 3f,
        defense = 0f,
        accuracy = 0.9f,
    ),
    speedScaling = 0.5f,
    lifeScaling = 1f,
    canAttack = false,
    attackRange = 1f,
    attackScaling = 1f,
    hasAiBehavior = true,
    aiTreePath = "slimeBehavior.tree",
    physicsScaling = vec2(0.3f, 0.3f),
    physicsOffset = vec2(0f, -2f * UNIT_SCALE),
    color = Color.WHITE,
    battleMap = "map_1_battle_1",
    xpReward = 10,
    goldReward = 5,
    lootPool = TIER_1_ITEMS,
    lootChance = 25,
)

val SLIME_BLUE_ENEMY_CONFIG = EnemyConfiguration(
    animationModel = AnimationModel.SLIME_BLUE,
    stats = EnemyStats(
        maxHealth = 20f,
        currentHealth = 20f,
        attackDamage = 5f,
        defense = 1f,
        accuracy = 0.9f,
    ),
    speedScaling = 0.5f,
    lifeScaling = 1f,
    canAttack = false,
    attackRange = 1f,
    attackScaling = 1f,
    hasAiBehavior = true,
    aiTreePath = "slimeBehavior.tree",
    physicsScaling = vec2(0.3f, 0.3f),
    physicsOffset = vec2(0f, -2f * UNIT_SCALE),
    color = Color.WHITE,
    battleMap = "map_1_battle_1",
    xpReward = 25,
    goldReward = 10,
    lootPool = TIER_1_ITEMS,
    lootChance = 25,
)

val SLIME_RED_ENEMY_CONFIG = EnemyConfiguration(
    animationModel = AnimationModel.SLIME_RED,
    stats = EnemyStats(
        maxHealth = 30f,
        currentHealth = 30f,
        attackDamage = 10f,
        defense = 3f,
        accuracy = 0.9f,
    ),
    speedScaling = 0.5f,
    lifeScaling = 1f,
    canAttack = false,
    attackRange = 1f,
    attackScaling = 1f,
    hasAiBehavior = true,
    aiTreePath = "slimeBehavior.tree",
    physicsScaling = vec2(0.3f, 0.3f),
    physicsOffset = vec2(0f, -2f * UNIT_SCALE),
    color = Color.WHITE,
    battleMap = "map_1_battle_1",
    xpReward = 50,
    goldReward = 20,
    lootPool = TIER_2_ITEMS,
    lootChance = 50,
)

val ENEMY_CONFIGS: Map<AnimationModel, EnemyConfiguration> = mapOf(
    AnimationModel.SLIME_GREEN to SLIME_GREEN_ENEMY_CONFIG,
    AnimationModel.SLIME_BLUE  to SLIME_BLUE_ENEMY_CONFIG,
    AnimationModel.SLIME_RED   to SLIME_RED_ENEMY_CONFIG,
)
