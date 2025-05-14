package com.github.jacks.roleplayinggame

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.github.jacks.roleplayinggame.screens.GameScreen
import ktx.app.KtxGame
import ktx.app.KtxScreen

class RolePlayingGame : KtxGame<KtxScreen>(){

    override fun create() {
        Gdx.app.logLevel = Application.LOG_DEBUG
        addScreen(GameScreen())
        setScreen<GameScreen>()
    }

    companion object {
        const val UNIT_SCALE = 1/16f
    }
}

