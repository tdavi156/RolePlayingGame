package com.github.jacks.roleplayinggame.configurations

import com.github.jacks.roleplayinggame.components.StatType

enum class EquipmentStatType {
    UNDEFINED, MAX_HEALTH, ATTACK_DAMAGE, DEFENSE, MOVE_SPEED;

    fun toStatType(): StatType = when (this) {
        UNDEFINED     -> StatType.UNDEFINED
        MAX_HEALTH    -> StatType.MAX_HEALTH
        ATTACK_DAMAGE -> StatType.ATTACK_DAMAGE
        DEFENSE       -> StatType.DEFENSE
        MOVE_SPEED    -> StatType.MOVE_SPEED
    }
}
