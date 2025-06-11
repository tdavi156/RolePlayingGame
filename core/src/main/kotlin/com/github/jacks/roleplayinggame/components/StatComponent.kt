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
) {
    val isDead : Boolean
        get() = currentHealth <= 0

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
