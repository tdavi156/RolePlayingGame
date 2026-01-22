package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import ktx.log.logger

class MenuViewModel(
    stage : Stage
) : PropertyChangeSource(), EventListener {

    private val preferences : Preferences by lazy { Gdx.app.getPreferences("planetaryIdlePrefs") }

    init {
        stage.addListener(this)
    }

    override fun handle(event: Event): Boolean {
        when (event) {
        }
        return true
    }

    companion object {
        private val log = logger<MenuViewModel>()
    }
}
