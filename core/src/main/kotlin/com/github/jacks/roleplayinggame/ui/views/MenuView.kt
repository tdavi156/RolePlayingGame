package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.github.jacks.roleplayinggame.ui.Buttons
import com.github.jacks.roleplayinggame.ui.Labels
import com.github.jacks.roleplayinggame.ui.viewmodels.MenuViewModel
import ktx.log.logger
import ktx.scene2d.KTable
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor
import ktx.scene2d.label
import ktx.scene2d.table
import ktx.scene2d.textButton

class MenuView(
    model : MenuViewModel,
    skin : Skin
) : Table(skin), KTable {

    private val preferences : Preferences by lazy { Gdx.app.getPreferences("planetaryIdlePrefs") }

    // buttons
    private val planetButton : TextButton
    private val galaxyButton : TextButton
    private val automationButton : TextButton
    private val challengesButton : TextButton
    private val shopButton : TextButton
    private val achievementsButton : TextButton
    private val statisticsButton : TextButton
    private val settingsButton : TextButton
    private val resetButton : TextButton
    private val quitButton : TextButton

    private lateinit var planetToolTipLabel : Label
    private lateinit var galaxyToolTipLabel : Label
    private lateinit var automationToolTipLabel : Label
    private lateinit var challengesToolTipLabel : Label
    private lateinit var shopToolTipLabel : Label
    private lateinit var achievementsToolTipLabel : Label
    private lateinit var statisticsToolTipLabel : Label
    private lateinit var settingsToolTipLabel : Label
    private lateinit var resetToolTipLabel : Label
    private lateinit var quitToolTipLabel : Label

    init {
        setFillParent(true)
        stage = getStage()

        // tooltips
        table { tooltipTableCell ->
            this@MenuView.planetToolTipLabel = label("planet", Labels.SMALL_GREY_BGD.skinKey) { cell ->
                cell.expand().top().right().width(150f).height(45f).pad(4f, 2f, 2f, 2f)
                this.setAlignment(Align.center)
                this.isVisible = false
            }
            row()
            this@MenuView.galaxyToolTipLabel = label("galaxy", Labels.SMALL_GREY_BGD.skinKey) { cell ->
                cell.expand().top().right().width(150f).height(45f).pad(2f, 2f, 2f, 2f)
                this.setAlignment(Align.center)
                this.isVisible = false
            }
            row()
            this@MenuView.automationToolTipLabel = label("automation", Labels.SMALL_GREY_BGD.skinKey) { cell ->
                cell.expand().top().right().width(150f).height(45f).pad(2f, 2f, 2f, 2f)
                this.setAlignment(Align.center)
                this.isVisible = false
            }
            row()
            this@MenuView.challengesToolTipLabel = label("challenges", Labels.SMALL_GREY_BGD.skinKey) { cell ->
                cell.expand().top().right().width(150f).height(45f).pad(2f, 2f, 2f, 2f)
                this.setAlignment(Align.center)
                this.isVisible = false
            }
            row()
            this@MenuView.shopToolTipLabel = label("shop", Labels.SMALL_GREY_BGD.skinKey) { cell ->
                cell.expand().top().right().width(150f).height(45f).pad(2f, 2f, 2f, 2f)
                this.setAlignment(Align.center)
                this.isVisible = false
            }
            row()
            this@MenuView.achievementsToolTipLabel = label("achievements", Labels.SMALL_GREY_BGD.skinKey) { cell ->
                cell.expand().top().right().width(150f).height(45f).pad(2f, 2f, 2f, 2f)
                this.setAlignment(Align.center)
                this.isVisible = false
            }
            row()
            this@MenuView.statisticsToolTipLabel = label("statistics", Labels.SMALL_GREY_BGD.skinKey) { cell ->
                cell.expand().top().right().width(150f).height(45f).pad(2f, 2f, 2f, 2f)
                this.setAlignment(Align.center)
                this.isVisible = false
            }
            row()
            this@MenuView.settingsToolTipLabel = label("settings", Labels.SMALL_GREY_BGD.skinKey) { cell ->
                cell.expand().top().right().width(150f).height(45f).pad(2f, 2f, 2f, 2f)
                this.setAlignment(Align.center)
                this.isVisible = false
            }
            row()
            this@MenuView.resetToolTipLabel = label("reset", Labels.SMALL_GREY_BGD.skinKey) { cell ->
                cell.expand().top().right().width(150f).height(45f).pad(2f, 2f, 2f, 2f)
                this.setAlignment(Align.center)
                this.isVisible = false
            }
            row()
            this@MenuView.quitToolTipLabel = label("quit", Labels.SMALL_GREY_BGD.skinKey) { cell ->
                cell.expand().top().right().width(150f).height(45f).pad(2f, 2f, 2f, 2f)
                this.setAlignment(Align.center)
                this.isVisible = false
            }
            row()
            tooltipTableCell.expand().top().right().width(230f).padTop(44f)
        }

        // menu buttons
        table { menuTableCell ->
            // planet, stage(1)
            this@MenuView.planetButton = textButton("Planet", Buttons.GREY_BUTTON_MEDIUM.skinKey) { cell ->
                cell.top().left().width(200f).height(45f).pad(4f,2f,2f,2f)
                this.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) {
                        this@MenuView.changeActiveActor(1)
                    }
                })
                this.addListener(object : InputListener() {
                    override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                        this@MenuView.planetToolTipLabel.isVisible = isOver
                        super.enter(event, x, y, pointer, fromActor)
                    }
                    override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                        this@MenuView.planetToolTipLabel.isVisible = isOver
                        super.exit(event, x, y, pointer, toActor)
                    }
                })
            }
            row()
            // galaxy
            this@MenuView.galaxyButton = textButton("Locked", Buttons.GREY_BUTTON_MEDIUM.skinKey) { cell ->
                cell.top().left().width(200f).height(45f).pad(2f,2f,2f,2f)
                isDisabled = true
                this.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) {
                        //this@MenuView.changeActiveActor(2)
                    }
                })
                this.addListener(object : InputListener() {
                    override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                        this@MenuView.galaxyToolTipLabel.isVisible = isOver
                        super.enter(event, x, y, pointer, fromActor)
                    }
                    override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                        this@MenuView.galaxyToolTipLabel.isVisible = isOver
                        super.exit(event, x, y, pointer, toActor)
                    }
                })
            }
            row()
            // automation
            this@MenuView.automationButton = textButton("Locked", Buttons.GREY_BUTTON_MEDIUM.skinKey) { cell ->
                cell.top().left().width(200f).height(45f).pad(2f,2f,2f,2f)
                isDisabled = true
                this.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) {
                        //this@MenuView.changeActiveActor(3)
                    }
                })
                this.addListener(object : InputListener() {
                    override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                        this@MenuView.automationToolTipLabel.isVisible = isOver
                        super.enter(event, x, y, pointer, fromActor)
                    }
                    override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                        this@MenuView.automationToolTipLabel.isVisible = isOver
                        super.exit(event, x, y, pointer, toActor)
                    }
                })
            }
            row()
            // challenges
            this@MenuView.challengesButton = textButton("Locked", Buttons.GREY_BUTTON_MEDIUM.skinKey) { cell ->
                cell.top().left().width(200f).height(45f).pad(2f,2f,2f,2f)
                isDisabled = true
                this.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) {
                        //this@MenuView.changeActiveActor(4)
                    }
                })
                this.addListener(object : InputListener() {
                    override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                        this@MenuView.challengesToolTipLabel.isVisible = isOver
                        super.enter(event, x, y, pointer, fromActor)
                    }
                    override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                        this@MenuView.challengesToolTipLabel.isVisible = isOver
                        super.exit(event, x, y, pointer, toActor)
                    }
                })
            }
            row()
            // shop, stage(2)
            this@MenuView.shopButton = textButton("Shop", Buttons.GREY_BUTTON_MEDIUM.skinKey) { cell ->
                cell.top().left().width(200f).height(45f).pad(2f,2f,2f,2f)
                isDisabled = false
                this.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) {
                        this@MenuView.changeActiveActor(2)
                    }
                })
                this.addListener(object : InputListener() {
                    override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                        this@MenuView.shopToolTipLabel.isVisible = isOver
                        super.enter(event, x, y, pointer, fromActor)
                    }
                    override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                        this@MenuView.shopToolTipLabel.isVisible = isOver
                        super.exit(event, x, y, pointer, toActor)
                    }
                })
            }
            row()
            // achievements, stage(3)
            this@MenuView.achievementsButton = textButton("Achievements", Buttons.GREY_BUTTON_MEDIUM.skinKey) { cell ->
                cell.top().left().width(200f).height(45f).pad(2f,2f,2f,2f)
                isDisabled = false
                this.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) {
                        this@MenuView.changeActiveActor(3)
                    }
                })
                this.addListener(object : InputListener() {
                    override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                        this@MenuView.achievementsToolTipLabel.isVisible = isOver
                        super.enter(event, x, y, pointer, fromActor)
                    }
                    override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                        this@MenuView.achievementsToolTipLabel.isVisible = isOver
                        super.exit(event, x, y, pointer, toActor)
                    }
                })
            }
            row()
            this@MenuView.statisticsButton = textButton("Statistics", Buttons.GREY_BUTTON_MEDIUM.skinKey) { cell ->
                cell.top().left().width(200f).height(45f).pad(2f,2f,2f,2f)
                isDisabled = true
                this.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) {
                        //this@MenuView.changeActiveActor(6)
                    }
                })
                this.addListener(object : InputListener() {
                    override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                        this@MenuView.statisticsToolTipLabel.isVisible = isOver
                        super.enter(event, x, y, pointer, fromActor)
                    }
                    override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                        this@MenuView.statisticsToolTipLabel.isVisible = isOver
                        super.exit(event, x, y, pointer, toActor)
                    }
                })
            }
            row()
            this@MenuView.settingsButton = textButton("Settings", Buttons.GREY_BUTTON_MEDIUM.skinKey) { cell ->
                cell.top().left().width(200f).height(45f).pad(2f,2f,2f,2f)
                isDisabled = true
                this.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) {
                        //this@MenuView.changeActiveActor(7)
                    }
                })
                this.addListener(object : InputListener() {
                    override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                        this@MenuView.settingsToolTipLabel.isVisible = isOver
                        super.enter(event, x, y, pointer, fromActor)
                    }
                    override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                        this@MenuView.settingsToolTipLabel.isVisible = isOver
                        super.exit(event, x, y, pointer, toActor)
                    }
                })
            }
            row()
            this@MenuView.resetButton = textButton("Reset Game", Buttons.GREY_BUTTON_MEDIUM.skinKey) { cell ->
                cell.top().left().width(200f).height(45f).pad(2f,2f,2f,2f)
                isDisabled = false
                this.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) {
                        log.debug { "Reset Game" }
                    }
                })
                this.addListener(object : InputListener() {
                    override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                        this@MenuView.resetToolTipLabel.isVisible = isOver
                        super.enter(event, x, y, pointer, fromActor)
                    }
                    override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                        this@MenuView.resetToolTipLabel.isVisible = isOver
                        super.exit(event, x, y, pointer, toActor)
                    }
                })
            }
            row()
            this@MenuView.quitButton = textButton("Quit Game", Buttons.GREY_BUTTON_MEDIUM.skinKey) { cell ->
                cell.top().left().width(200f).height(45f).pad(2f,2f,2f,2f)
                this.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) {
                        log.debug { "Save Game" }
                        log.debug { "Quit Game" }
                    }
                })
                this.addListener(object : InputListener() {
                    override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                        this@MenuView.quitToolTipLabel.isVisible = isOver
                        super.enter(event, x, y, pointer, fromActor)
                    }
                    override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                        this@MenuView.quitToolTipLabel.isVisible = isOver
                        super.exit(event, x, y, pointer, toActor)
                    }
                })
            }
            menuTableCell.top().right().width(204f).padTop(44f)
        }

        // Data Binding
        // model.onPropertyChange(PlanetModel::totalPopulationAmount) { amount -> totalPopAmountChange(amount) }
    }

    private fun changeActiveActor(actorId : Int) {
        stage.actors.get(1).isVisible = actorId == 1
        stage.actors.get(2).isVisible = actorId == 2
        stage.actors.get(3).isVisible = actorId == 3
    }

    companion object {
        private val log = logger<MenuView>()
    }
}

@Scene2dDsl
fun <S> KWidget<S>.menuView(
    model : MenuViewModel,
    skin : Skin = Scene2DSkin.defaultSkin,
    init : MenuView.(S) -> Unit = { }
) : MenuView = actor(MenuView(model, skin), init)
