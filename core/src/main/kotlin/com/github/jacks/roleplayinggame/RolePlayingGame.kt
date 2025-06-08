package com.github.jacks.roleplayinggame

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.FitViewport
import com.github.jacks.roleplayinggame.events.GamePauseEvent
import com.github.jacks.roleplayinggame.events.GameResumeEvent
import com.github.jacks.roleplayinggame.screens.GameScreen
import com.github.jacks.roleplayinggame.screens.InventoryScreen
import com.github.jacks.roleplayinggame.screens.UIScreen
import com.github.jacks.roleplayinggame.ui.disposeSkin
import com.github.jacks.roleplayinggame.ui.loadSkin
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.assets.disposeSafely

class RolePlayingGame : KtxGame<KtxScreen>(), EventListener{

    private val batch : Batch by lazy { SpriteBatch() }
    val gameStage : Stage by lazy { Stage(FitViewport(16f * CAMERA_ZOOM, 9f * CAMERA_ZOOM), batch) }
    val uiStage : Stage by lazy { Stage(FitViewport(320f, 180f), batch) }
    private var paused = false

    override fun create() {
        Gdx.app.logLevel = Application.LOG_DEBUG

        loadSkin()
        gameStage.addListener(this)
        addScreen(GameScreen(this))
        addScreen(UIScreen())
        addScreen(InventoryScreen())
        setScreen<GameScreen>()
    }

    override fun render() {
        clearScreen(0f, 0f, 0f, 1f)
        val deltaTime = if (paused) 0f else Gdx.graphics.deltaTime
        currentScreen.render(deltaTime)
    }

    override fun resize(width: Int, height: Int) {
        gameStage.viewport.update(width, height, true)
        uiStage.viewport.update(width, height, true)
    }

    override fun dispose() {
        super.dispose()
        gameStage.disposeSafely()
        uiStage.disposeSafely()
        batch.disposeSafely()
        disposeSkin()
    }

    companion object {
        const val UNIT_SCALE = 1/16f
        const val CAMERA_ZOOM = 1.5f
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is GamePauseEvent -> {
                paused = true
                currentScreen.pause()
            }
            is GameResumeEvent -> {
                paused = false
                currentScreen.resume()
            }
            else -> return false
        }
        return true
    }
}

