package com.github.jacks.roleplayinggame.components

class AbilityComponent {
    val unlockedAbilityIds: MutableSet<Int> = mutableSetOf()

    fun isSkilled(abilityId: Int) = abilityId in unlockedAbilityIds

    companion object {
        const val KEY_UNLOCKED_ABILITY_IDS = "player_unlocked_ability_ids"
    }
}
