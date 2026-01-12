package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.github.jacks.roleplayinggame.ui.Drawables
import com.github.jacks.roleplayinggame.ui.Labels
import com.github.jacks.roleplayinggame.ui.get
import com.github.jacks.roleplayinggame.ui.viewmodels.MainGameViewModel
import com.github.jacks.roleplayinggame.ui.widgets.CharacterInfo
import com.github.jacks.roleplayinggame.ui.widgets.characterInfo
import ktx.actors.alpha
import ktx.actors.plusAssign
import ktx.actors.txt
import ktx.preferences.get
import ktx.scene2d.KTable
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor
import ktx.scene2d.label
import ktx.scene2d.table

class MainGameView(
    model : MainGameViewModel,
    skin : Skin
) : Table(skin), KTable {

    private lateinit var stage : Stage
    private val preferences : Preferences by lazy { Gdx.app.getPreferences("rolePlayingGamePrefs") }

    // initial values from preferences
    private var playerHealth = preferences["player_health", 1.0]
    private var playerMana = preferences["player_mana", 1.0]
    private var playerExperience = preferences["player_experience", 0]

    // buttons
    private var characterInfoButton : TextButton
    private var settingsButton : TextButton
    private var menuButton : TextButton



    // old stuff
    private val playerInfo : CharacterInfo
    //private val enemyInfo : CharacterInfo
    private val popupLabel : Label

    init {
        // UI elements
        setFillParent(true)
        table {
            background = skin[Drawables.FRAME_BGD]
            this@MainGameView.popupLabel = label(text = "", style = Labels.FRAME.skinKey) { labelCell ->
                this.setAlignment(Align.topLeft)
                this.wrap = true
                labelCell.expand().fill().pad(14f)
            }

            this.alpha = 0f
            it.expand().width(130f).height(40f).top().padTop(8f).row()
        }

        playerInfo = characterInfo(Drawables.PLAYER)

        // data binding
        model.onPropertyChange(MainGameViewModel::playerLife) { playerLife ->
            playerLife(playerLife)
        }
        model.onPropertyChange(MainGameViewModel::lootText) { lootText ->
            popup(lootText)
        }
    }

    fun playerLife(percentage : Float) {
        playerInfo.life(percentage)
    }

    private fun Actor.resetFadeOutDelay() {
        this.actions.filterIsInstance<SequenceAction>().lastOrNull()?.let { sequence ->
            val delay = sequence.actions.last() as DelayAction
            delay.time = 0f
        }
    }

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
fun <S> KWidget<S>.mainGameView(
    model : MainGameViewModel,
    skin : Skin = Scene2DSkin.defaultSkin,
    init: MainGameView.(S) -> Unit = {}
) : MainGameView = actor(MainGameView(model, skin), init)
