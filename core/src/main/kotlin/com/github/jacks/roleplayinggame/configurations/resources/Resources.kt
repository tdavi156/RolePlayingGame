package com.github.jacks.roleplayinggame.configurations.resources

data class Resources(
    var gold: Int = 0
) {
    companion object {
        const val KEY_GOLD = "gold"
    }
}
