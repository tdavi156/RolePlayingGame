package com.github.jacks.roleplayinggame.components

/**
 * Sealed base class for all runtime stat holders.
 * Systems should read stats through this interface whenever possible.
 * Cast to [com.github.jacks.roleplayinggame.systems.CharacterData] or [EnemyStats]
 * only where type-specific behaviour is required.
 */
abstract class StatsProvider {
    abstract var currentHealth: Float
    abstract val maxHealth: Float
    abstract var currentMana: Float
    abstract val maxMana: Float
    abstract val moveSpeed: Float
    abstract val attackSpeed: Float
    abstract val accuracy: Float
    abstract val evasion: Float
    abstract val attackDamage: Float
    abstract val attackDamagePercent: Float
    abstract val spellDamage: Float
    abstract val spellDamagePercent: Float
    abstract val defense: Float
    abstract val defensePercent: Float
    abstract val resistance: Float
    abstract val resistancePercent: Float

    val isDead: Boolean get() = currentHealth <= 0f
}

// ── Enemy stats ──────────────────────────────────────────────────────────────

data class EnemyStats(
    // BattleStats
    override val maxHealth: Float,
    override var currentHealth: Float,
    override val maxMana: Float = 0f,
    override var currentMana: Float = 0f,
    override val moveSpeed: Float = 1f,
    override val attackSpeed: Float = 1f,
    override val accuracy: Float = 0.9f,          // default for all enemies per design decision
    override val evasion: Float = 0f,
    override val attackDamage: Float = 0f,
    override val attackDamagePercent: Float = 1f,
    override val spellDamage: Float = 0f,
    override val spellDamagePercent: Float = 1f,
    override val defense: Float = 0f,
    override val defensePercent: Float = 1f,
    override val resistance: Float = 0f,
    override val resistancePercent: Float = 1f,
) : StatsProvider()
