package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.saveManager.SaveManager
import com.github.quillraven.fleks.World
import ktx.log.logger

class MenuViewModel(
    stage: Stage,
    private val saveManager: SaveManager,
    private val entityWorld: World,
) : PropertyChangeSource(), EventListener {

    init {
        stage.addListener(this)
    }

    fun saveGame() {
        saveManager.gatherAndSave(entityWorld)
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
