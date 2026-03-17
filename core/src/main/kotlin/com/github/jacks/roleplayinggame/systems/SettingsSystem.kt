package com.github.jacks.roleplayinggame.systems

import com.github.jacks.roleplayinggame.configurations.Settings
import com.github.quillraven.fleks.IntervalSystem

class SettingsSystem : IntervalSystem() {
    var settings = Settings()

    override fun onTick() {
        // Settings are read by other systems at runtime — no tick logic needed.
    }
}
