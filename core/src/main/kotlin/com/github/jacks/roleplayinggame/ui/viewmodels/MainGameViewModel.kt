package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.events.EntityAddItemEvent
import com.github.quillraven.fleks.World

class MainGameViewModel(
    world : World,
    stage : Stage
) : PropertyChangeSource(), EventListener {

    var lootText by propertyNotify("")

    init {
        stage.addListener(this)
    }

    override fun handle(event: Event): Boolean {
        when(event) {
            is EntityAddItemEvent -> {
                lootText = "New Item found: [#4e557d]${event.itemName}[]"
            }
            else -> return false
        }
        return true
    }
}
