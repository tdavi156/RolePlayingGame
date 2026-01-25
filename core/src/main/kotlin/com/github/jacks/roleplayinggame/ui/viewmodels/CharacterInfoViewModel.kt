package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.World

class CharacterInfoViewModel(
    world : World,
    stage : Stage
) : PropertyChangeSource(), EventListener {

    init {
        stage.addListener(this)
    }

    override fun handle(event: Event): Boolean {
        return true
    }
}
