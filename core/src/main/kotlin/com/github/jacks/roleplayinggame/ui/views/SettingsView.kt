package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.github.jacks.roleplayinggame.configurations.CombatAnimationSpeed
import com.github.jacks.roleplayinggame.ui.Buttons
import com.github.jacks.roleplayinggame.ui.Drawables
import com.github.jacks.roleplayinggame.ui.Labels
import com.github.jacks.roleplayinggame.ui.get
import com.github.jacks.roleplayinggame.ui.viewmodels.SettingsViewModel
import ktx.actors.txt
import ktx.scene2d.KTable
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor
import ktx.scene2d.label
import ktx.scene2d.table
import ktx.scene2d.textButton

class SettingsView(
    private val model: SettingsViewModel,
    skin: Skin
) : Table(skin), KTable {

    private val rowTables = mutableListOf<Table>()

    private lateinit var masterValueLabel: Label
    private lateinit var musicValueLabel: Label
    private lateinit var effectsValueLabel: Label

    private lateinit var speedNormalBtn: TextButton
    private lateinit var speedFastBtn: TextButton
    private lateinit var speedOffBtn: TextButton

    private lateinit var autoClearOnBtn: TextButton
    private lateinit var autoClearOffBtn: TextButton

    private lateinit var saveBtn: TextButton
    private lateinit var cancelBtn: TextButton

    init {
        // Capture references as locals to avoid @DslMarker implicit receiver restrictions inside DSL lambdas
        val vm = model
        val rows = rowTables
        val hover = { idx: Int ->
            object : InputListener() {
                override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                    vm.focusedRow = idx
                }
            }
        }

        setFillParent(true)

        table { outerCell ->
            background = skin[Drawables.FRAME_BGD]
            pad(12f)

            // Title
            label("Settings", Labels.MEDIUM.skinKey) { it.colspan(1).center().padBottom(10f) }
            row()

            // Row 0: Master Volume
            val r0 = table { rowCell ->
                pad(4f)
                label("Master Volume:", Labels.SMALL.skinKey) { it.left().expandX() }
                textButton("<", Buttons.GREY_BUTTON_SMALL.skinKey) { cell ->
                    cell.width(30f).height(30f)
                    addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            vm.masterVolume = (vm.masterVolume - 10).coerceIn(0, 100)
                        }
                    })
                }
                this@SettingsView.masterValueLabel = label("100%", Labels.SMALL.skinKey) { it.width(55f).center() }
                textButton(">", Buttons.GREY_BUTTON_SMALL.skinKey) { cell ->
                    cell.width(30f).height(30f)
                    addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            vm.masterVolume = (vm.masterVolume + 10).coerceIn(0, 100)
                        }
                    })
                }
                rowCell.fillX().padBottom(2f)
                addListener(hover(0))
            }
            rows.add(r0)
            row()

            // Row 1: Music Volume
            val r1 = table { rowCell ->
                pad(4f)
                label("Music Volume:", Labels.SMALL.skinKey) { it.left().expandX() }
                textButton("<", Buttons.GREY_BUTTON_SMALL.skinKey) { cell ->
                    cell.width(30f).height(30f)
                    addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            vm.musicVolume = (vm.musicVolume - 10).coerceIn(0, 100)
                        }
                    })
                }
                this@SettingsView.musicValueLabel = label("100%", Labels.SMALL.skinKey) { it.width(55f).center() }
                textButton(">", Buttons.GREY_BUTTON_SMALL.skinKey) { cell ->
                    cell.width(30f).height(30f)
                    addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            vm.musicVolume = (vm.musicVolume + 10).coerceIn(0, 100)
                        }
                    })
                }
                rowCell.fillX().padBottom(2f)
                addListener(hover(1))
            }
            rows.add(r1)
            row()

            // Row 2: Effects Volume
            val r2 = table { rowCell ->
                pad(4f)
                label("Effects Volume:", Labels.SMALL.skinKey) { it.left().expandX() }
                textButton("<", Buttons.GREY_BUTTON_SMALL.skinKey) { cell ->
                    cell.width(30f).height(30f)
                    addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            vm.effectsVolume = (vm.effectsVolume - 10).coerceIn(0, 100)
                        }
                    })
                }
                this@SettingsView.effectsValueLabel = label("100%", Labels.SMALL.skinKey) { it.width(55f).center() }
                textButton(">", Buttons.GREY_BUTTON_SMALL.skinKey) { cell ->
                    cell.width(30f).height(30f)
                    addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            vm.effectsVolume = (vm.effectsVolume + 10).coerceIn(0, 100)
                        }
                    })
                }
                rowCell.fillX().padBottom(2f)
                addListener(hover(2))
            }
            rows.add(r2)
            row()

            // Row 3: Combat Animation Speed
            val r3 = table { rowCell ->
                pad(4f)
                label("Combat Speed:", Labels.SMALL.skinKey) { it.left().expandX() }
                this@SettingsView.speedNormalBtn = textButton("Normal", Buttons.GREY_BUTTON_SMALL.skinKey) { cell ->
                    cell.width(70f).height(30f).padLeft(4f)
                    addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            vm.combatAnimationSpeed = CombatAnimationSpeed.NORMAL
                        }
                    })
                }
                this@SettingsView.speedFastBtn = textButton("Fast", Buttons.GREY_BUTTON_SMALL.skinKey) { cell ->
                    cell.width(55f).height(30f).padLeft(4f)
                    addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            vm.combatAnimationSpeed = CombatAnimationSpeed.FAST
                        }
                    })
                }
                this@SettingsView.speedOffBtn = textButton("Off", Buttons.GREY_BUTTON_SMALL.skinKey) { cell ->
                    cell.width(45f).height(30f).padLeft(4f)
                    addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            vm.combatAnimationSpeed = CombatAnimationSpeed.OFF
                        }
                    })
                }
                rowCell.fillX().padBottom(2f)
                addListener(hover(3))
            }
            rows.add(r3)
            row()

            // Row 4: Auto Clear Text
            val r4 = table { rowCell ->
                pad(4f)
                label("Auto Clear Text:", Labels.SMALL.skinKey) { it.left().expandX() }
                this@SettingsView.autoClearOnBtn = textButton("On", Buttons.GREY_BUTTON_SMALL.skinKey) { cell ->
                    cell.width(50f).height(30f).padLeft(4f)
                    addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            vm.autoClearText = true
                        }
                    })
                }
                this@SettingsView.autoClearOffBtn = textButton("Off", Buttons.GREY_BUTTON_SMALL.skinKey) { cell ->
                    cell.width(50f).height(30f).padLeft(4f)
                    addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            vm.autoClearText = false
                        }
                    })
                }
                rowCell.fillX().padBottom(2f)
                addListener(hover(4))
            }
            rows.add(r4)
            row()

            // Row 5: Save / Cancel
            val r5 = table { rowCell ->
                pad(4f)
                this@SettingsView.saveBtn = textButton("Save", Buttons.GREEN_BUTTON_MEDIUM.skinKey) { cell ->
                    cell.width(100f).height(35f).padRight(10f)
                    addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            vm.save()
                        }
                    })
                }
                this@SettingsView.cancelBtn = textButton("Cancel", Buttons.RED_BUTTON_MEDIUM.skinKey) { cell ->
                    cell.width(100f).height(35f)
                    addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent, actor: Actor) {
                            vm.cancel()
                        }
                    })
                }
                rowCell.fillX().center().padBottom(2f)
                addListener(hover(5))
            }
            rows.add(r5)

            outerCell.expand().center().width(420f)
        }

        // --- Data bindings ---

        model.onPropertyChange(SettingsViewModel::masterVolume) { v ->
            masterValueLabel.txt = "$v%"
        }
        model.onPropertyChange(SettingsViewModel::musicVolume) { v ->
            musicValueLabel.txt = "$v%"
        }
        model.onPropertyChange(SettingsViewModel::effectsVolume) { v ->
            effectsValueLabel.txt = "$v%"
        }
        model.onPropertyChange(SettingsViewModel::combatAnimationSpeed) { speed ->
            speedNormalBtn.color = if (speed == CombatAnimationSpeed.NORMAL) Color.ORANGE else Color.WHITE
            speedFastBtn.color = if (speed == CombatAnimationSpeed.FAST) Color.ORANGE else Color.WHITE
            speedOffBtn.color = if (speed == CombatAnimationSpeed.OFF) Color.ORANGE else Color.WHITE
        }
        model.onPropertyChange(SettingsViewModel::autoClearText) { on ->
            autoClearOnBtn.color = if (on) Color.ORANGE else Color.WHITE
            autoClearOffBtn.color = if (!on) Color.ORANGE else Color.WHITE
        }
        model.onPropertyChange(SettingsViewModel::focusedRow) { row ->
            rowTables.forEachIndexed { index, table ->
                table.background = if (index == row) skin[Drawables.ROW_HIGHLIGHT] else null
            }
        }
        model.onPropertyChange(SettingsViewModel::row5FocusedButton) { btn ->
            saveBtn.color = if (btn == 0) Color.ORANGE else Color.WHITE
            cancelBtn.color = if (btn == 1) Color.ORANGE else Color.WHITE
        }
    }
}

@Scene2dDsl
fun <S> KWidget<S>.settingsView(
    model: SettingsViewModel,
    skin: Skin = Scene2DSkin.defaultSkin,
    init: SettingsView.(S) -> Unit = {}
): SettingsView = actor(SettingsView(model, skin), init)
