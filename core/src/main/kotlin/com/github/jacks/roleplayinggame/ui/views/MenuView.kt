package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.github.jacks.roleplayinggame.events.SettingsClosedEvent
import com.github.jacks.roleplayinggame.events.SettingsOpenEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.ui.Buttons
import com.github.jacks.roleplayinggame.ui.viewmodels.MenuViewModel
import ktx.log.logger
import ktx.scene2d.KTable
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor
import ktx.scene2d.table
import ktx.scene2d.textButton

class MenuView(
    model: MenuViewModel,
    skin: Skin
) : Table(skin), KTable, EventListener {

    // buttons
    private val settingsButton: TextButton
    private val statisticsButton: TextButton
    private val achievementsButton: TextButton
    private val resetButton: TextButton // for testing purposes only
    private val quitButton: TextButton

    init {
        setFillParent(true)

        // menu buttons
        table { menuTableCell ->
            this@MenuView.settingsButton = textButton("Settings", Buttons.GREY_BUTTON_MEDIUM.skinKey) { cell ->
                cell.top().left().width(200f).height(45f).pad(2f, 2f, 2f, 2f)
                this.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) {
                        val s = this@MenuView.stage ?: return
                        s.fire(SettingsOpenEvent())
                        s.actors.filterIsInstance<SettingsView>().firstOrNull()?.isVisible = true
                    }
                })
            }
            row()
            this@MenuView.statisticsButton = textButton("Statistics", Buttons.GREY_BUTTON_MEDIUM.skinKey) { cell ->
                cell.top().left().width(200f).height(45f).pad(2f, 2f, 2f, 2f)
                this.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) {
                        //this@MenuView.changeActiveActor(6)
                    }
                })
            }
            row()
            this@MenuView.achievementsButton = textButton("Achievements", Buttons.GREY_BUTTON_MEDIUM.skinKey) { cell ->
                cell.top().left().width(200f).height(45f).pad(2f, 2f, 2f, 2f)
                this.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) {
                        this@MenuView.changeActiveActor(3)
                    }
                })
            }
            row()
            this@MenuView.resetButton = textButton("Reset Game", Buttons.GREY_BUTTON_MEDIUM.skinKey) { cell ->
                cell.top().left().width(200f).height(45f).pad(2f, 2f, 2f, 2f)
                isDisabled = true
                this.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) {
                        log.debug { "Reset Game" }
                    }
                })
            }
            row()
            this@MenuView.quitButton = textButton("Quit Game", Buttons.GREY_BUTTON_MEDIUM.skinKey) { cell ->
                cell.top().left().width(200f).height(45f).pad(2f, 2f, 2f, 2f)
                this.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) {
                        log.debug { "Quit Game" }
                    }
                })
            }
            menuTableCell.expand().fill().width(204f)
        }
    }

    override fun setStage(stage: Stage?) {
        val previous = getStage()
        super.setStage(stage)
        if (previous == null && stage != null) {
            stage.addListener(this)
        }
    }

    override fun handle(event: Event): Boolean {
        if (event is SettingsClosedEvent) {
            stage?.actors?.filterIsInstance<SettingsView>()?.firstOrNull()?.isVisible = false
            return true
        }
        return false
    }

    private fun changeActiveActor(actorId: Int) {
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
    model: MenuViewModel,
    skin: Skin = Scene2DSkin.defaultSkin,
    init: MenuView.(S) -> Unit = {}
): MenuView = actor(MenuView(model, skin), init)
