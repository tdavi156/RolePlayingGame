package com.github.jacks.roleplayinggame.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.github.jacks.roleplayinggame.components.LifeComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.events.EntityTakeDamageEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.ui.views.gameView
import com.github.jacks.roleplayinggame.ui.viewmodels.MainGameViewModel
import com.github.jacks.roleplayinggame.ui.views.GameView
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.world
import ktx.app.KtxScreen
import ktx.scene2d.actors

class UIScreen : KtxScreen {
    private val stage : Stage = Stage(ExtendViewport(320f, 180f))
    private val entityWorld = world {  }
    private val playerEntity : Entity
    private val model = MainGameViewModel(entityWorld, stage)
    private lateinit var gameView : GameView

    init {
        playerEntity = entityWorld.entity {
            add<PlayerComponent>()
            add<LifeComponent> {
                maxHealth = 10f
                health = 7f
            }
        }
    }

    override fun show() {
        stage.clear()
        stage.addListener(model)
        stage.actors {
            gameView = gameView(model)
        }
        //stage.isDebugAll = true
    }

    override fun render(delta: Float) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            hide()
            show()
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            stage.fire(EntityTakeDamageEvent(playerEntity))
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            gameView.playerLife(0.5f)
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            gameView.playerLife(1f)
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
            //gameView.showEnemyInfo(Drawables.SLIME, 0.75f)
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) {
            gameView.popup("this is a [#ff0000]test[]!")
        }

        stage.act()
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
    }
}
