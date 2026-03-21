package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.github.jacks.roleplayinggame.ui.Buttons
import com.github.jacks.roleplayinggame.ui.Drawables
import com.github.jacks.roleplayinggame.ui.Labels
import com.github.jacks.roleplayinggame.ui.get
import com.github.jacks.roleplayinggame.ui.viewmodels.SkillViewModel
import ktx.actors.txt
import ktx.scene2d.KTable
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor
import ktx.scene2d.label
import ktx.scene2d.stack
import ktx.scene2d.table
import ktx.scene2d.textButton

class SkillView(
    model: SkillViewModel,
    skin: Skin,
) : Table(skin), KTable {

    private lateinit var availablePointsLabel: Label

    // Stamina row
    private lateinit var staminaMinusBtn: TextButton
    private lateinit var staminaPendingLabel: Label
    private lateinit var staminaPlusBtn: TextButton

    // Strength row
    private lateinit var strengthMinusBtn: TextButton
    private lateinit var strengthPendingLabel: Label
    private lateinit var strengthPlusBtn: TextButton

    // Agility row
    private lateinit var agilityMinusBtn: TextButton
    private lateinit var agilityPendingLabel: Label
    private lateinit var agilityPlusBtn: TextButton

    // Intelligence row
    private lateinit var intelligenceMinusBtn: TextButton
    private lateinit var intelligencePendingLabel: Label
    private lateinit var intelligencePlusBtn: TextButton

    // Wisdom row
    private lateinit var wisdomMinusBtn: TextButton
    private lateinit var wisdomPendingLabel: Label
    private lateinit var wisdomPlusBtn: TextButton

    // Footer sections
    private lateinit var normalFooter: Table
    private lateinit var saveConfirmFooter: Table
    private lateinit var cancelConfirmFooter: Table

    init {
        val vm = model
        setFillParent(true)

        table { outerCell ->
            background = skin[Drawables.FRAME_BGD]
            pad(12f)

            // Title
            label("Skills", Labels.MEDIUM.skinKey) { it.colspan(5).center().padBottom(6f) }
            row()

            // Available points header
            this@SkillView.availablePointsLabel =
                label("Skill Points Available: 0", Labels.SMALL.skinKey) { it.colspan(5).left().padBottom(10f) }
            row()

            // ── Stamina row ──────────────────────────────────────────────────
            label("Stamina:", Labels.SMALL.skinKey) { it.left().expandX() }
            this@SkillView.staminaMinusBtn = textButton("-", Buttons.RED_BUTTON_SMALL.skinKey) { cell ->
                cell.width(30f).height(30f)
                isDisabled = true
                addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) { vm.removeStamina() }
                })
            }
            this@SkillView.staminaPendingLabel = label("0", Labels.SMALL.skinKey) { it.width(30f).center() }
            this@SkillView.staminaPlusBtn = textButton("+", Buttons.GREEN_BUTTON_SMALL.skinKey) { cell ->
                cell.width(30f).height(30f)
                addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) { vm.addStamina() }
                })
            }
            label("+10 Max HP per point", Labels.SMALL.skinKey) { it.width(140f).right() }
            row()

            // ── Strength row ─────────────────────────────────────────────────
            label("Strength:", Labels.SMALL.skinKey) { it.left().expandX() }
            this@SkillView.strengthMinusBtn = textButton("-", Buttons.RED_BUTTON_SMALL.skinKey) { cell ->
                cell.width(30f).height(30f)
                isDisabled = true
                addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) { vm.removeStrength() }
                })
            }
            this@SkillView.strengthPendingLabel = label("0", Labels.SMALL.skinKey) { it.width(30f).center() }
            this@SkillView.strengthPlusBtn = textButton("+", Buttons.GREEN_BUTTON_SMALL.skinKey) { cell ->
                cell.width(30f).height(30f)
                addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) { vm.addStrength() }
                })
            }
            label("+3 Attack Damage per point", Labels.SMALL.skinKey) { it.width(140f).right() }
            row()

            // ── Agility row ──────────────────────────────────────────────────
            label("Agility:", Labels.SMALL.skinKey) { it.left().expandX() }
            this@SkillView.agilityMinusBtn = textButton("-", Buttons.RED_BUTTON_SMALL.skinKey) { cell ->
                cell.width(30f).height(30f)
                isDisabled = true
                addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) { vm.removeAgility() }
                })
            }
            this@SkillView.agilityPendingLabel = label("0", Labels.SMALL.skinKey) { it.width(30f).center() }
            this@SkillView.agilityPlusBtn = textButton("+", Buttons.GREEN_BUTTON_SMALL.skinKey) { cell ->
                cell.width(30f).height(30f)
                addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) { vm.addAgility() }
                })
            }
            label("+Acc/Eva/Speed per point", Labels.SMALL.skinKey) { it.width(140f).right() }
            row()

            // ── Intelligence row ─────────────────────────────────────────────
            label("Intelligence:", Labels.SMALL.skinKey) { it.left().expandX() }
            this@SkillView.intelligenceMinusBtn = textButton("-", Buttons.RED_BUTTON_SMALL.skinKey) { cell ->
                cell.width(30f).height(30f)
                isDisabled = true
                addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) { vm.removeIntelligence() }
                })
            }
            this@SkillView.intelligencePendingLabel = label("0", Labels.SMALL.skinKey) { it.width(30f).center() }
            this@SkillView.intelligencePlusBtn = textButton("+", Buttons.GREEN_BUTTON_SMALL.skinKey) { cell ->
                cell.width(30f).height(30f)
                addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) { vm.addIntelligence() }
                })
            }
            label("+3 Spell Dmg, +5 Mana per point", Labels.SMALL.skinKey) { it.width(140f).right() }
            row()

            // ── Wisdom row ───────────────────────────────────────────────────
            label("Wisdom:", Labels.SMALL.skinKey) { it.left().expandX() }
            this@SkillView.wisdomMinusBtn = textButton("-", Buttons.RED_BUTTON_SMALL.skinKey) { cell ->
                cell.width(30f).height(30f)
                isDisabled = true
                addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) { vm.removeWisdom() }
                })
            }
            this@SkillView.wisdomPendingLabel = label("0", Labels.SMALL.skinKey) { it.width(30f).center() }
            this@SkillView.wisdomPlusBtn = textButton("+", Buttons.GREEN_BUTTON_SMALL.skinKey) { cell ->
                cell.width(30f).height(30f)
                addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) { vm.addWisdom() }
                })
            }
            label("+2 Resistance, +3 Mana per point", Labels.SMALL.skinKey) { it.width(140f).right() }
            row()

            // ── Footer ───────────────────────────────────────────────────────
            stack { stackCell ->
                this@SkillView.normalFooter = table {
                    textButton("Save", Buttons.GREEN_BUTTON_MEDIUM.skinKey) { cell ->
                        cell.width(100f).height(35f).padRight(10f)
                        addListener(object : ChangeListener() {
                            override fun changed(event: ChangeEvent, actor: Actor) { vm.requestSave() }
                        })
                    }
                    textButton("Cancel", Buttons.RED_BUTTON_MEDIUM.skinKey) { cell ->
                        cell.width(100f).height(35f)
                        addListener(object : ChangeListener() {
                            override fun changed(event: ChangeEvent, actor: Actor) { vm.requestCancel() }
                        })
                    }
                }

                this@SkillView.saveConfirmFooter = table {
                    label("Stat changes cannot be undone. Confirm?", Labels.SMALL.skinKey) {
                        it.colspan(2).center().padBottom(4f)
                    }
                    row()
                    textButton("Yes", Buttons.GREEN_BUTTON_MEDIUM.skinKey) { cell ->
                        cell.width(80f).height(35f).padRight(10f)
                        addListener(object : ChangeListener() {
                            override fun changed(event: ChangeEvent, actor: Actor) { vm.confirmSave() }
                        })
                    }
                    textButton("No", Buttons.RED_BUTTON_MEDIUM.skinKey) { cell ->
                        cell.width(80f).height(35f)
                        addListener(object : ChangeListener() {
                            override fun changed(event: ChangeEvent, actor: Actor) { vm.cancelSave() }
                        })
                    }
                    isVisible = false
                }

                this@SkillView.cancelConfirmFooter = table {
                    label("Discard unsaved changes?", Labels.SMALL.skinKey) {
                        it.colspan(2).center().padBottom(4f)
                    }
                    row()
                    textButton("Yes", Buttons.GREEN_BUTTON_MEDIUM.skinKey) { cell ->
                        cell.width(80f).height(35f).padRight(10f)
                        addListener(object : ChangeListener() {
                            override fun changed(event: ChangeEvent, actor: Actor) { vm.confirmCancel() }
                        })
                    }
                    textButton("No", Buttons.RED_BUTTON_MEDIUM.skinKey) { cell ->
                        cell.width(80f).height(35f)
                        addListener(object : ChangeListener() {
                            override fun changed(event: ChangeEvent, actor: Actor) { vm.dismissCancelConfirm() }
                        })
                    }
                    isVisible = false
                }

                stackCell.colspan(5).center().padTop(8f).height(70f)
            }

            outerCell.expand().center().width(460f)
        }

        // ── Data bindings ────────────────────────────────────────────────────

        model.onPropertyChange(SkillViewModel::availableSkillPoints) { pts ->
            availablePointsLabel.txt = "Skill Points Available: $pts"
            staminaPlusBtn.isDisabled      = pts <= 0
            strengthPlusBtn.isDisabled     = pts <= 0
            agilityPlusBtn.isDisabled      = pts <= 0
            intelligencePlusBtn.isDisabled = pts <= 0
            wisdomPlusBtn.isDisabled       = pts <= 0
        }

        model.onPropertyChange(SkillViewModel::pendingStamina) { pts ->
            staminaPendingLabel.txt        = "$pts"
            staminaMinusBtn.isDisabled     = pts <= 0
        }
        model.onPropertyChange(SkillViewModel::pendingStrength) { pts ->
            strengthPendingLabel.txt       = "$pts"
            strengthMinusBtn.isDisabled    = pts <= 0
        }
        model.onPropertyChange(SkillViewModel::pendingAgility) { pts ->
            agilityPendingLabel.txt        = "$pts"
            agilityMinusBtn.isDisabled     = pts <= 0
        }
        model.onPropertyChange(SkillViewModel::pendingIntelligence) { pts ->
            intelligencePendingLabel.txt   = "$pts"
            intelligenceMinusBtn.isDisabled = pts <= 0
        }
        model.onPropertyChange(SkillViewModel::pendingWisdom) { pts ->
            wisdomPendingLabel.txt         = "$pts"
            wisdomMinusBtn.isDisabled      = pts <= 0
        }

        model.onPropertyChange(SkillViewModel::showSaveConfirm) { show ->
            normalFooter.isVisible      = !show && !model.showCancelConfirm
            saveConfirmFooter.isVisible = show
        }

        model.onPropertyChange(SkillViewModel::showCancelConfirm) { show ->
            normalFooter.isVisible        = !show && !model.showSaveConfirm
            cancelConfirmFooter.isVisible = show
        }
    }
}

@Scene2dDsl
fun <S> KWidget<S>.skillView(
    model: SkillViewModel,
    skin: Skin = Scene2DSkin.defaultSkin,
    init: SkillView.(S) -> Unit = { }
): SkillView = actor(SkillView(model, skin), init)
