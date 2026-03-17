package com.github.jacks.roleplayinggame.configurations

enum class CombatAnimationSpeed { NORMAL, FAST, OFF }

data class Settings(
    var masterVolume: Int = 100,
    var musicVolume: Int = 100,
    var effectsVolume: Int = 100,
    var combatAnimationSpeed: CombatAnimationSpeed = CombatAnimationSpeed.NORMAL,
    var autoClearText: Boolean = false
) {
    companion object {
        const val KEY_MASTER_VOLUME = "master_volume"
        const val KEY_MUSIC_VOLUME = "music_volume"
        const val KEY_EFFECTS_VOLUME = "effects_volume"
        const val KEY_COMBAT_ANIMATION_SPEED = "combat_animation_speed"
        const val KEY_AUTO_CLEAR_TEXT = "auto_clear_text"
    }
}
