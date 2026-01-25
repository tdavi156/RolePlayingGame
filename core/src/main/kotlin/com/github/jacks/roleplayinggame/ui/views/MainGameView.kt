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
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.github.jacks.roleplayinggame.ui.Buttons
import com.github.jacks.roleplayinggame.ui.Drawables
import com.github.jacks.roleplayinggame.ui.Labels
import com.github.jacks.roleplayinggame.ui.get
import com.github.jacks.roleplayinggame.ui.viewmodels.MainGameViewModel
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

    private val preferences : Preferences by lazy { Gdx.app.getPreferences("rolePlayingGamePrefs") }

    // initial values from preferences
    private var playerHealth = preferences["player_health", 1.0]
    private var playerMana = preferences["player_mana", 1.0]
    private var playerExperience = preferences["player_experience", 0]

    // buttons
    private lateinit var characterInfoButton : TextButton
    private var inventoryButton : TextButton
    private var skillButton : TextButton
    private var questButton : TextButton
    private var mapButton : TextButton
    private var menuButton : TextButton

    // labels
//    private lateinit var characterInfoToolTipLabel : Label
//    private lateinit var inventoryToolTipLabel : Label
//    private lateinit var skillToolTipLabel : Label
//    private lateinit var questToolTipLabel : Label
//    private lateinit var mapToolTipLabel : Label

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
//            stack { stackCell ->
//                this@MainGameView.characterInfoToolTipLabel = label("Character", Labels.TEST_LABEL.skinKey) { cell ->
//                    cell.setSize(120f, 25f)
//                    //cell.width(120f).height(25f).pad(0f, 5f, 0f, 0f)
//                    this.setAlignment(Align.center)
//                    this.isVisible = false
//                }
//                this@MainGameView.inventoryToolTipLabel = label("Inventory", Labels.TEST_LABEL.skinKey) { cell ->
//                    cell.setSize(120f, 25f)
//                    //cell.width(120f).height(25f).pad(0f, 5f, 0f, 0f)
//                    this.setAlignment(Align.center)
//                    this.isVisible = false
//                }
//                this@MainGameView.skillToolTipLabel = label("Skills", Labels.TEST_LABEL.skinKey) { cell ->
//                    cell.setSize(120f, 25f)
//                    //cell.width(120f).height(25f).pad(0f, 5f, 0f, 0f)
//                    this.setAlignment(Align.center)
//                    this.isVisible = false
//                }
//                this@MainGameView.questToolTipLabel = label("Quest Log", Labels.TEST_LABEL.skinKey) { cell ->
//                    cell.setSize(120f, 25f)
//                    //cell.width(120f).height(25f).pad(0f, 5f, 0f, 0f)
//                    this.setAlignment(Align.center)
//                    this.isVisible = false
//                }
//                this@MainGameView.mapToolTipLabel = label("Map", Labels.TEST_LABEL.skinKey) { cell ->
//                    cell.setSize(240f, 25f)
//                    //cell.width(120f).height(25f).pad(0f, 5f, 0f, 0f)
//                    this.setAlignment(Align.center)
//                    this.isVisible = false
//                }
//                stackCell.expand().fill().bottom().left().width(250f).height(40f)
//            }
//            row()

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
//                this.addListener(object : InputListener() {
//                    override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
//                        this@MainGameView.characterInfoToolTipLabel.isVisible = isOver
//                        this@MainGameView.characterInfoToolTipLabel.setPosition(
//                            this@MainGameView.getTooltipLocation(this@MainGameView.characterInfoButton, "x"),
//                            this@MainGameView.getTooltipLocation(this@MainGameView.characterInfoButton, "y")
//                        )
//                        super.enter(event, x, y, pointer, fromActor)
//                    }
//                    override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
//                        this@MainGameView.characterInfoToolTipLabel.isVisible = isOver
//                        super.exit(event, x, y, pointer, toActor)
//                    }
//                })
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
//                this.addListener(object : InputListener() {
//                    override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
//                        this@MainGameView.inventoryToolTipLabel.isVisible = isOver
//                        this@MainGameView.inventoryToolTipLabel.setPosition(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
//                        super.enter(event, x, y, pointer, fromActor)
//                    }
//                    override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
//                        this@MainGameView.inventoryToolTipLabel.isVisible = isOver
//                        super.exit(event, x, y, pointer, toActor)
//                    }
//                })
                }
                this@MainGameView.skillButton = textButton("Skills (L)", Buttons.BROWN_BUTTON_MEDIUM.skinKey) { cell ->
                    cell.expandX().width(100f).height(30f).pad(0f,2f,2f,2f)
                    this.addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            // set the active view, similar to the menu buttons
                            // change the active view to be accurate according to the order of the gameScreen.init
                            this@MainGameView.changeActiveView(7)
                        }
                    })
//                this.addListener(object : InputListener() {
//                    override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
//                        this@MainGameView.skillToolTipLabel.isVisible = isOver
//                        this@MainGameView.skillToolTipLabel.setPosition(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
//                        super.enter(event, x, y, pointer, fromActor)
//                    }
//                    override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
//                        this@MainGameView.skillToolTipLabel.isVisible = isOver
//                        super.exit(event, x, y, pointer, toActor)
//                    }
//                })
                }
                this@MainGameView.questButton = textButton("Quests (J)", Buttons.BROWN_BUTTON_MEDIUM.skinKey) { cell ->
                    cell.expandX().width(115f).height(30f).pad(0f,2f,2f,2f)
                    this.addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            // set the active view, similar to the menu buttons
                            // change the active view to be accurate according to the order of the gameScreen.init
                            this@MainGameView.changeActiveView(8)
                        }
                    })
//                this.addListener(object : InputListener() {
//                    override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
//                        this@MainGameView.questToolTipLabel.isVisible = isOver
//                        this@MainGameView.questToolTipLabel.setPosition(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
//                        super.enter(event, x, y, pointer, fromActor)
//                    }
//                    override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
//                        this@MainGameView.questToolTipLabel.isVisible = isOver
//                        super.exit(event, x, y, pointer, toActor)
//                    }
//                })
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
//                this.addListener(object : InputListener() {
//                    override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
//                        this@MainGameView.mapToolTipLabel.isVisible = isOver
//                        this@MainGameView.mapToolTipLabel.setPosition(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
//                        super.enter(event, x, y, pointer, fromActor)
//                    }
//                    override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
//                        this@MainGameView.mapToolTipLabel.isVisible = isOver
//                        super.exit(event, x, y, pointer, toActor)
//                    }
//                })
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
        model.onPropertyChange(MainGameViewModel::playerLife) { playerLife ->
        //    playerLife(playerLife)
        }
        model.onPropertyChange(MainGameViewModel::lootText) { lootText ->
            //popup(lootText)
        }
        model.onPropertyChange(MainGameViewModel::expAmount) { expAmount -> expAmountChanged(expAmount) }
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

    private fun getTooltipLocation(button : Button, coordinateDirection : String) : Float {
        return when(coordinateDirection) {
            "x" -> { (button.x + button.width + 10f) }
            "y" -> { (button.y + button.height + 10f) }
            else -> button.x
        }
    }

    private fun expAmountChanged(expAmount : Int) {
        // calculate the exp amount, or perhaps recieve it as a percentage
        experienceBar.scaleX = 0.5f
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
