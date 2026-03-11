package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.delay
import com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn
import com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.github.jacks.roleplayinggame.components.BattlePhase
import com.github.jacks.roleplayinggame.ui.Buttons
import com.github.jacks.roleplayinggame.ui.Drawables
import com.github.jacks.roleplayinggame.ui.Labels
import com.github.jacks.roleplayinggame.ui.get
import com.github.jacks.roleplayinggame.ui.viewmodels.BattleViewModel
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
import ktx.scene2d.textButton

class BattleView(
    model : BattleViewModel,
    skin : Skin
) : Table(skin), KTable {

    private lateinit var stage : Stage
    private val preferences : Preferences by lazy { Gdx.app.getPreferences("rolePlayingGamePrefs") }

    // initial values from preferences
    private var playerHealth = preferences["player_health", 1.0]
    private var playerMana = preferences["player_mana", 1.0]
    private var playerExperience = preferences["player_experience", 0]

    private val playerInfo : CharacterInfo
    private val enemyInfo : CharacterInfo
    private val popupLabel : Label
    private lateinit var actionMenu : Table

    init {
        setFillParent(true)

        // === Row 1: Enemy info (top-right) ===
        enemyInfo = characterInfo(Drawables.SLIME) {
            it.colspan(2).expandX().right().padTop(8f).padRight(8f).row()
        }

        // === Row 2: Battle log popup (centered, expands to fill middle) ===
        table {
            background = skin[Drawables.FRAME_BGD]
            this@BattleView.popupLabel = label(text = "", style = Labels.FRAME.skinKey) { labelCell ->
                this.setAlignment(Align.topLeft)
                this.wrap = true
                labelCell.expand().fill().pad(14f)
            }

            this.alpha = 0f
            it.width(130f).height(40f).colspan(2).expand().row()
        }

        // === Row 3: Player info (bottom-left) + Action menu (bottom-center) ===
        playerInfo = characterInfo(Drawables.PLAYER) {
            it.padBottom(8f).padLeft(8f)
        }

        actionMenu = table {
            defaults().pad(6f).minWidth(90f)
            textButton("Attack", Buttons.GREEN_BUTTON_MEDIUM.skinKey) {
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent, x: Float, y: Float) {
                        model.onAttack()
                    }
                })
                it.height(60f).width(200f).row()
            }
            textButton("Flee", Buttons.RED_BUTTON_MEDIUM.skinKey) {
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent, x: Float, y: Float) {
                        model.onFlee()
                    }
                })
            }
            it.height(60f).width(200f).expandX().padBottom(32f)
        }

        // Data bindings
        model.onPropertyChange(BattleViewModel::playerLife) { playerLife ->
            playerLife(playerLife)
        }
        model.onPropertyChange(BattleViewModel::enemyLife) { pct ->
            enemyLife(pct)
        }
        model.onPropertyChange(BattleViewModel::lootText) { lootText ->
            popup(lootText)
        }
        model.onPropertyChange(BattleViewModel::battleLog) { log ->
            if (log.isNotBlank()) popup(log)
        }
        model.onPropertyChange(BattleViewModel::battlePhase) { phase ->
            actionMenu.isVisible = (phase == BattlePhase.PLAYER_TURN)
        }
    }

    fun playerLife(percentage : Float) {
        playerInfo.life(percentage)
    }

    fun enemyLife(percentage : Float) {
        enemyInfo.life(percentage)
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
            popupLabel.parent += Actions.sequence(fadeIn(0.2f), delay(1.5f, fadeOut(0.3f)))
        } else {
            popupLabel.parent.resetFadeOutDelay()
        }
    }
}

@Scene2dDsl
fun <S> KWidget<S>.battleView(
    model : BattleViewModel,
    skin : Skin = Scene2DSkin.defaultSkin,
    init: BattleView.(S) -> Unit = {}
) : BattleView = actor(BattleView(model, skin), init)
