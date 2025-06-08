package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.github.jacks.roleplayinggame.ui.Drawables
import com.github.jacks.roleplayinggame.ui.Labels
import com.github.jacks.roleplayinggame.ui.get
import com.github.jacks.roleplayinggame.ui.viewmodels.GameViewModel
import com.github.jacks.roleplayinggame.ui.widgets.CharacterInfo
import com.github.jacks.roleplayinggame.ui.widgets.characterInfo
import ktx.actors.alpha
import ktx.actors.plusAssign
import ktx.actors.txt
import ktx.scene2d.KTable
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor
import ktx.scene2d.label
import ktx.scene2d.table

class GameView(
    model : GameViewModel,
    skin : Skin
) : Table(skin), KTable {

    private val playerInfo : CharacterInfo
    //private val enemyInfo : CharacterInfo
    private val popupLabel : Label

    init {
        // UI elements
        setFillParent(true)

        /*
        enemyInfo = characterInfo(Drawables.PLAYER) {
            this.alpha = 0f
            it.row()
        }
         */

        table {
            background = skin[Drawables.FRAME_BGD]
            this@GameView.popupLabel = label(text = "", style = Labels.FRAME.skinKey) { labelCell ->
                this.setAlignment(Align.topLeft)
                this.wrap = true
                labelCell.expand().fill().pad(14f)
            }

            this.alpha = 0f
            it.expand().width(130f).height(40f).top().padTop(8f).row()
        }

        playerInfo = characterInfo(Drawables.PLAYER)


        // data binding
        model.onPropertyChange(GameViewModel::playerLife) { playerLife ->
            playerLife(playerLife)
        }
        /*
        model.onPropertyChange(GameViewModel::enemyLife) { enemyLife ->
            enemyLife(enemyLife)
        }
         */
        model.onPropertyChange(GameViewModel::lootText) { lootText ->
            popup(lootText)
        }
    }

    fun playerLife(percentage : Float) {
        playerInfo.life(percentage)
    }

    /*
    fun enemyLife(percentage : Float) {
        enemyInfo.life(percentage)
    }
     */

    private fun Actor.resetFadeOutDelay() {
        this.actions.filterIsInstance<SequenceAction>().lastOrNull()?.let { sequence ->
            val delay = sequence.actions.last() as DelayAction
            delay.time = 0f
        }
    }

    /*
    fun showEnemyInfo(charDrawable : Drawables, lifePercentage : Float) {
        enemyInfo.character(charDrawable)
        enemyInfo.life(lifePercentage, 0f)

        if (enemyInfo.alpha == 0f) {
            enemyInfo.clearActions()
            enemyInfo += sequence(fadeIn(0.5f), delay(5f, fadeOut(0.5f)))
        } else {
            enemyInfo.resetFadeOutDelay()
        }
    }
     */

    fun popup(infoText : String) {
        popupLabel.txt = infoText

        if (popupLabel.parent.alpha == 0f) {
            popupLabel.parent.clearActions()
            popupLabel.parent += sequence(fadeIn(0.2f), delay(1.5f, fadeOut(0.3f)))
        } else {
            popupLabel.parent.resetFadeOutDelay()
        }
    }
}

@Scene2dDsl
fun <S> KWidget<S>.gameView(
    model : GameViewModel,
    skin : Skin = Scene2DSkin.defaultSkin,
    init: GameView.(S) -> Unit = {}
) : GameView = actor(GameView(model, skin), init)
