package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.github.jacks.roleplayinggame.events.AbilityViewOpenEvent
import com.github.jacks.roleplayinggame.events.GamePauseEvent
import com.github.jacks.roleplayinggame.events.SkillViewOpenEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.ui.Buttons
import com.github.jacks.roleplayinggame.ui.Drawables
import com.github.jacks.roleplayinggame.ui.get
import com.github.jacks.roleplayinggame.ui.viewmodels.MainGameViewModel
import ktx.actors.txt
import ktx.scene2d.KTable
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor
import ktx.scene2d.image
import ktx.scene2d.stack
import ktx.scene2d.table
import ktx.scene2d.textButton
import java.math.BigDecimal
import java.math.RoundingMode

class MainGameView(
    model : MainGameViewModel,
    private val gameStage: Stage,
    skin : Skin
) : Table(skin), KTable {

    // buttons
    private lateinit var characterInfoButton : TextButton
    private var inventoryButton : TextButton
    private var skillButton : TextButton
    private var questButton : TextButton
    private var mapButton : TextButton
    private var menuButton : TextButton

    // images
    private var experienceBar : Image


    init {
        // UI elements
        setFillParent(true)
        stage = getStage()
        table { tableCell ->
            table { emptyTableCell ->
                emptyTableCell.expand().fill().colspan(3)
            }
            row()

            table { playerInfoTableCell ->
                playerInfoTableCell.expandX().pad(4f)
            }

            table { progressBarTableCell ->
                stack { stackCell ->
                    image(skin[Drawables.BAR_GREY_THICK])
                    this@MainGameView.experienceBar = image(skin[Drawables.BAR_GREEN_THICK]) { cell ->
                        scaleX = 0.5f
                    }
                    stackCell.center().width(520f).height(25f)
                }
                progressBarTableCell.expandX().height(30f).pad(4f)
            }

            table { buttonsTableCell ->
                this@MainGameView.characterInfoButton = textButton("Character (C)", Buttons.BROWN_BUTTON_MEDIUM.skinKey) { cell ->
                    cell.expandX().width(150f).height(30f).pad(0f,5f,2f,2f)
                    this.addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            // pause while any view is active
                            this@MainGameView.changeActiveView(5)
                        }
                    })
                }
                this@MainGameView.inventoryButton = textButton("Inventory (I)", Buttons.BROWN_BUTTON_MEDIUM.skinKey) { cell ->
                    cell.expandX().width(130f).height(30f).pad(0f,2f,2f,2f)
                    this.addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            // set the active view, similar to the menu buttons
                            // change the active view to be accurate according to the order of the gameScreen.init
                            this@MainGameView.changeActiveView(6)
                        }
                    })
                }
                this@MainGameView.skillButton = textButton("Skills (L)", Buttons.BROWN_BUTTON_MEDIUM.skinKey) { cell ->
                    cell.expandX().width(100f).height(30f).pad(0f,2f,2f,2f)
                    this.addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            this@MainGameView.gameStage.fire(GamePauseEvent())
                            this@MainGameView.gameStage.fire(SkillViewOpenEvent())
                            stage.actors.filterIsInstance<BackgroundView>().firstOrNull()?.isVisible = true
                            stage.actors.filterIsInstance<SkillView>().firstOrNull()?.isVisible = true
                        }
                    })
                }
                textButton("Ability (J)", Buttons.BROWN_BUTTON_MEDIUM.skinKey) { cell ->
                    cell.expandX().width(105f).height(30f).pad(0f,2f,2f,2f)
                    this.addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            this@MainGameView.gameStage.fire(GamePauseEvent())
                            this@MainGameView.gameStage.fire(AbilityViewOpenEvent())
                            stage.actors.filterIsInstance<BackgroundView>().firstOrNull()?.isVisible = true
                            stage.actors.filterIsInstance<AbilityView>().firstOrNull()?.isVisible = true
                        }
                    })
                }
                this@MainGameView.questButton = textButton("Quests (Q)", Buttons.BROWN_BUTTON_MEDIUM.skinKey) { cell ->
                    cell.expandX().width(115f).height(30f).pad(0f,2f,2f,2f)
                    this.addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            // set the active view, similar to the menu buttons
                            // change the active view to be accurate according to the order of the gameScreen.init
                            this@MainGameView.changeActiveView(8)
                        }
                    })
                }
                this@MainGameView.mapButton = textButton("Map (M)", Buttons.BROWN_BUTTON_MEDIUM.skinKey) { cell ->
                    cell.expandX().width(90f).height(30f).pad(0f,2f,2f,2f)
                    this.addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            // set the active view, similar to the menu buttons
                            // change the active view to be accurate according to the order of the gameScreen.init
                            this@MainGameView.changeActiveView(9)
                        }
                    })
                }
                this@MainGameView.menuButton = textButton("-", Buttons.BROWN_BUTTON_MEDIUM.skinKey) { cell ->
                    cell.expandX().width(30f).height(30f).pad(0f,2f,2f,5f)
                    this.addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            // set the active view, similar to the menu buttons
                            // change the active view to be accurate according to the order of the gameScreen.init
                            this@MainGameView.changeActiveView(10)
                        }
                    })
                }
                buttonsTableCell.expandX().height(35f).pad(4f)
            }
            tableCell.expand().fill()
        }

        // data binding
        model.onPropertyChange(MainGameViewModel::lootText) { lootText ->
            //popup(lootText)
        }
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
        stage.actors.get(5).isVisible = (actorId == 5 && !stage.actors.get(5).isVisible)
        stage.actors.get(6).isVisible = (actorId == 6 && !stage.actors.get(6).isVisible)
        stage.actors.get(7).isVisible = (actorId == 7 && !stage.actors.get(7).isVisible)
        stage.actors.get(8).isVisible = (actorId == 8 && !stage.actors.get(8).isVisible)
        stage.actors.get(9).isVisible = (actorId == 9 && !stage.actors.get(9).isVisible)
        stage.actors.get(10).isVisible = (actorId == 10 && !stage.actors.get(10).isVisible)
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
    gameStage: Stage,
    skin : Skin = Scene2DSkin.defaultSkin,
    init: MainGameView.(S) -> Unit = {}
) : MainGameView = actor(MainGameView(model, gameStage, skin), init)
