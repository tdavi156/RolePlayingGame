package com.github.jacks.roleplayinggame.components

import com.github.jacks.roleplayinggame.components.StatType.*

enum class StatType {
    UNDEFINED, CURRENT_HEALTH, MAX_HEALTH, ATTACK_DAMAGE, DEFENSE, MOVE_SPEED;
}

data class StatComponent(
    var prefsName : String = "",
    var currentHealth : Float = 0f,
    var maxHealth : Float = 0f,
    var currentMana : Float = 0f,
    var maxMana : Float = 0f,
    var attackDamage : Float = 0f,
    var attackPercent : Float = 1f,
    var attackSpeed : Float = 1f,
    var defense : Float = 0f,
    var defensePercent : Float = 1f,
    var moveSpeed : Float = 1f,
    var level : Int = 1,
    var experience : Int = 0,
    var xpReward : Int = 0,           // XP awarded when this entity is defeated
) {
    val isDead : Boolean
        get() = currentHealth <= 0

    /** XP required to reach the next level: 50 * current level. */
    val experienceToNextLevel : Int
        get() = level * 50

    /**
     * Add XP and process any level-ups. Returns the number of levels gained.
     * Each level grants +5 max HP (and heals that amount), +1 attack, +1 defense.
     */
    fun gainExperience(amount: Int): Int {
        experience += amount
        var levelsGained = 0
        while (experience >= experienceToNextLevel) {
            experience -= experienceToNextLevel
            level++
            levelsGained++
            // Stat boosts per level
            maxHealth += 5f
            currentHealth += 5f
            attackDamage += 1f
            defense += 1f
        }
        return levelsGained
    }

    fun increaseStat(statType: StatType, statValue : Float) {
        when (statType) {
            UNDEFINED -> { }
            CURRENT_HEALTH -> {
                currentHealth += statValue
            }
            MAX_HEALTH -> {
                maxHealth += statValue
                currentHealth += statValue
            }
            ATTACK_DAMAGE -> {
                attackDamage += statValue
            }
            DEFENSE -> {
                defense += statValue
            }
            MOVE_SPEED -> {
                moveSpeed += statValue
            }
        }
    }

    fun decreaseStat(statType: StatType, statValue : Float) {
        when (statType) {
            UNDEFINED -> { }
            CURRENT_HEALTH -> {
                currentHealth -= statValue
            }
            MAX_HEALTH -> {
                maxHealth -= statValue
                currentHealth -= statValue
            }
            ATTACK_DAMAGE -> {
                attackDamage -= statValue
            }
            DEFENSE -> {
                defense -= statValue
            }
            MOVE_SPEED -> {
                moveSpeed -= statValue
            }
        }
    }
}
