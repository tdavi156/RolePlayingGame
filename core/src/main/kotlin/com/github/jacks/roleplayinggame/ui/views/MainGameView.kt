package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent
import com.badlogic.gdx.utils.Align
import com.github.jacks.roleplayinggame.ui.Buttons
import com.github.jacks.roleplayinggame.ui.Drawables
import com.github.jacks.roleplayinggame.ui.Labels
import com.github.jacks.roleplayinggame.ui.get
import com.github.jacks.roleplayinggame.ui.viewmodels.MainGameViewModel
import com.github.jacks.roleplayinggame.ui.views.MenuView
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
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.stack
import ktx.scene2d.table
import ktx.scene2d.textButton
import java.math.BigDecimal
import java.math.RoundingMode

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
    private var inventoryButton : TextButton
    private var skillsButton : TextButton
    private var questsButton : TextButton
    private var mapButton : TextButton

    // labels
    private lateinit var characterInfoToolTipLabel : Label
    private lateinit var inventoryToolTipLabel : Label
    private lateinit var skillsToolTipLabel : Label
    private lateinit var questsToolTipLabel : Label
    private lateinit var mapToolTipLabel : Label

    // images
    private var experienceBar : Image


    init {
        // UI elements
        setFillParent(true)
        table { tableCell ->

            this@MainGameView.characterInfoButton = textButton("C", Buttons.BROWN_BUTTON_MEDIUM.skinKey) { cell ->
                cell.expand().top().left().width(40f).height(40f).pad(3f,5f,3f,0f)
                this.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) {
                        // set the active view, similar to the menu buttons
                        // change the active view to be accurate according to the order of the gameScreen.init
                        this@MainGameView.changeActiveView(3)
                    }
                })
                this.addListener(object : InputListener() {
                    override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                        this@MainGameView.characterInfoToolTipLabel.isVisible = isOver
                        this@MainGameView.characterInfoToolTipLabel.setPosition(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
                        super.enter(event, x, y, pointer, fromActor)
                    }
                    override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                        this@MainGameView.characterInfoToolTipLabel.isVisible = isOver
                        super.exit(event, x, y, pointer, toActor)
                    }
                })
            }

            table { progressBarTable ->
                stack { stackCell ->
                    image(skin[Drawables.BAR_GREY_THICK])
                    this@MainGameView.experienceBar = image(skin[Drawables.BAR_GREEN_THICK]) { cell ->
                        scaleX = 0f
                    }
                    stackCell.center().width(600f).height(30f)
                }
                progressBarTable.expandX().top().height(40f).padTop(15f)
            }
            tableCell.expand().fill().pad(5f, 0f, 5f, 0f)
        }

        // data binding
        model.onPropertyChange(MainGameViewModel::playerLife) { playerLife ->
        //    playerLife(playerLife)
        }
        model.onPropertyChange(MainGameViewModel::lootText) { lootText ->
            //popup(lootText)
        }
        model.onPropertyChange(MainGameViewModel::expAmount) { amount -> expAmountChanged(amount) }
    }

//    fun playerLife(percentage : Float) {
//        playerInfo.life(percentage)
//    }

    private fun Actor.resetFadeOutDelay() {
        this.actions.filterIsInstance<SequenceAction>().lastOrNull()?.let { sequence ->
            val delay = sequence.actions.last() as DelayAction
            delay.time = 0f
        }
    }

    private fun changeActiveView(actorId : Int) {
        stage.actors.get(1).isVisible = actorId == 1
        stage.actors.get(2).isVisible = actorId == 2
        stage.actors.get(3).isVisible = actorId == 3
    }

    private fun expAmountChanged(rate : Int) {
        productionRate = rate
        productionRateLabel.txt = "You are producing ${formatNumberWithLetter(rate)} food per second."
        val prodMan = BigDecimalMath.mantissa(productionRate)
        val prodExp = BigDecimalMath.exponent(productionRate).toBigDecimal()
        val expPercent = prodExp.divide(PLANETARY_EXPONENT, 6, RoundingMode.UP)
        val manPercent = expPercent * prodMan.divide(BigDecimal(10))
        var prodPercent = 0f

        if (expPercent != null && expPercent < BigDecimal(1 / 308)) {
            prodPercent = manPercent.toFloat()
        }

        if (productionRate > ONE && expPercent != null) {
            prodPercent = (expPercent + manPercent).toFloat()
        }
        productionRateProgressLabel.txt = "${"%.2f".format(prodPercent.coerceAtMost(1f) * 100f)} %"
        colonizationProgress.scaleX = prodPercent
    }

//    fun popup(infoText : String) {
//        popupLabel.txt = infoText
//
//        if (popupLabel.parent.alpha == 0f) {
//            popupLabel.parent.clearActions()
//            popupLabel.parent += sequence(fadeIn(0.2f), delay(1.5f, fadeOut(0.3f)))
//        } else {
//            popupLabel.parent.resetFadeOutDelay()
//        }
//    }
}

@Scene2dDsl
fun <S> KWidget<S>.mainGameView(
    model : MainGameViewModel,
    skin : Skin = Scene2DSkin.defaultSkin,
    init: MainGameView.(S) -> Unit = {}
) : MainGameView = actor(MainGameView(model, skin), init)
