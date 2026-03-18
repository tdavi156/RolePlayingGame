package com.github.jacks.roleplayinggame.components

import com.github.jacks.roleplayinggame.components.StatType.*

enum class StatType {
    UNDEFINED, CURRENT_HEALTH, MAX_HEALTH, ATTACK_DAMAGE, DEFENSE, MOVE_SPEED, ATTACK_SPEED;
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
    var skillPoints : Int = 0,         // available unspent skill points
    var abilityPoints : Int = 0,       // available unspent ability points
    var skillPointsInvestedAttack : Int = 0,   // cumulative points spent on attack
    var skillPointsInvestedDefense : Int = 0,  // cumulative points spent on defense
) {
    val isDead : Boolean
        get() = currentHealth <= 0

    /**
     * XP required to level up from the current level.
     * Formula: req(n) = n * 50 * (1.15 ^ n)
     *
     * Reference thresholds (first 10 levels):
     * L1→2:  58     L2→3:  132    L3→4:  263    L4→5:  439    L5→6:  669
     * L6→7:  967    L7→8:  1342   L8→9:  1811   L9→10: 2394   L10→11: 3107
     */
    val experienceToNextLevel : Int
        get() = (level * 50 * Math.pow(1.15, level.toDouble())).toInt()

    /**
     * Add XP and process any level-ups. Returns the number of levels gained.
     * Each level grants +10 maxHP, +5 maxMana only — no other passive stat boosts.
     * Callers are responsible for firing LevelUpEvent/GainSkillPointEvent/GainAbilityPointEvent.
     */
    fun gainExperience(amount: Int): Int {
        experience += amount
        var levelsGained = 0
        while (experience >= experienceToNextLevel) {
            experience -= experienceToNextLevel
            level++
            levelsGained++
            maxHealth += 10f
            currentHealth = (currentHealth + 10f).coerceAtMost(maxHealth)
            maxMana += 5f
            currentMana = (currentMana + 5f).coerceAtMost(maxMana)
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
            ATTACK_SPEED -> {
                attackSpeed += statValue
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
            ATTACK_SPEED -> {
                attackSpeed -= statValue
            }
        }
    }
}
