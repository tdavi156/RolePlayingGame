package com.github.jacks.roleplayinggame

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.github.jacks.roleplayinggame.screens.GameScreen
import com.github.jacks.roleplayinggame.screens.InventoryScreen
import com.github.jacks.roleplayinggame.screens.UIScreen
import com.github.jacks.roleplayinggame.ui.disposeSkin
import com.github.jacks.roleplayinggame.ui.loadSkin
import ktx.app.KtxGame
import ktx.app.KtxScreen

class RolePlayingGame : KtxGame<KtxScreen>(){

    override fun create() {
        Gdx.app.logLevel = Application.LOG_DEBUG

        loadSkin()
        addScreen(GameScreen())
        addScreen(UIScreen())
        addScreen(InventoryScreen())
        setScreen<GameScreen>()
        //setScreen<UIScreen>()
        //setScreen<InventoryScreen>()
    }

    override fun dispose() {
        super.dispose()
        disposeSkin()
    }

    companion object {
        const val UNIT_SCALE = 1/16f
    }
}

