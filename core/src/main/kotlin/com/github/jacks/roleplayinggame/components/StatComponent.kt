package com.github.jacks.roleplayinggame.components

enum class StatType {
    UNDEFINED, CURRENT_HEALTH, MAX_HEALTH, ATTACK_DAMAGE, DEFENSE, MOVE_SPEED, ATTACK_SPEED;
}

/**
 * Pure reference holder — points to a [StatsProvider] subtype.
 * Systems must never store individual stat fields here; always dereference through [stats].
 * Cast [stats] to [com.github.jacks.roleplayinggame.systems.CharacterData] or [EnemyStats]
 * only where type-specific behaviour is required.
 */
class StatComponent {
    lateinit var stats: StatsProvider

    val isDead: Boolean get() = stats.currentHealth <= 0f
}
