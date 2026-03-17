package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.jacks.roleplayinggame.configurations.resources.Resources
import com.github.quillraven.fleks.IntervalSystem
import ktx.preferences.flush
import ktx.preferences.set

class ResourceSystem : IntervalSystem(), EventListener {
    var resources = Resources()

    private val preferences by lazy { Gdx.app.getPreferences("rolePlayingGamePrefs") }

    fun saveResources() {
        preferences.flush {
            this[Resources.KEY_GOLD] = resources.gold
        }
    }

    override fun onTick() {
        // Resources are read/written on demand — no tick logic needed.
    }

    override fun handle(event: Event): Boolean = false
}
