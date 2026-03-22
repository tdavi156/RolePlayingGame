package com.github.jacks.roleplayinggame

/**
 * Development-only configuration flags.
 * Set these before running to control dev/test behavior.
 * All flags should be false before shipping.
 */
object DevConfig {
    /**
     * When true, deletes game_save.json on startup so every run starts from a clean slate.
     * Useful for testing new-game flows, spawn behavior, and initial state.
     * Settings (settings.json) are preserved so audio/display prefs survive restarts.
     */
    const val CLEAR_SAVE_ON_START: Boolean = true
}
