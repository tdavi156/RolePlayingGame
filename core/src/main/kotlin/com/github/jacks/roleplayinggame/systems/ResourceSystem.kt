package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.jacks.roleplayinggame.configurations.resources.Resources
import com.github.jacks.roleplayinggame.saveManager.SaveManager
import com.github.quillraven.fleks.IntervalSystem

class ResourceSystem(
    private val saveManager: SaveManager,
) : IntervalSystem(), EventListener {
    var resources = Resources()

    override fun onTick() {
        // Resources are read/written on demand — no tick logic needed.
    }

    override fun handle(event: Event): Boolean = false
}
