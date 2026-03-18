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

    // Attack row
    private lateinit var attackMinusBtn: TextButton
    private lateinit var attackPendingLabel: Label
    private lateinit var attackPlusBtn: TextButton
    private lateinit var attackPreviewLabel: Label

    // Defense row
    private lateinit var defenseMinusBtn: TextButton
    private lateinit var defensePendingLabel: Label
    private lateinit var defensePlusBtn: TextButton
    private lateinit var defensePreviewLabel: Label

    // Footer sections (shown one at a time via visibility in a Stack)
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

            // ── Attack row ──────────────────────────────────────────────────────
            label("Attack:", Labels.SMALL.skinKey) { it.left().expandX() }
            this@SkillView.attackMinusBtn = textButton("-", Buttons.RED_BUTTON_SMALL.skinKey) { cell ->
                cell.width(30f).height(30f)
                isDisabled = true
                addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) { vm.removeAttackPoint() }
                })
            }
            this@SkillView.attackPendingLabel = label("0", Labels.SMALL.skinKey) { it.width(30f).center() }
            this@SkillView.attackPlusBtn = textButton("+", Buttons.GREEN_BUTTON_SMALL.skinKey) { cell ->
                cell.width(30f).height(30f)
                addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) { vm.addAttackPoint() }
                })
            }
            this@SkillView.attackPreviewLabel = label("→ 0 ATK", Labels.SMALL.skinKey) { it.width(90f).right() }
            row()

            // ── Defense row ─────────────────────────────────────────────────────
            label("Defense:", Labels.SMALL.skinKey) { it.left().expandX() }
            this@SkillView.defenseMinusBtn = textButton("-", Buttons.RED_BUTTON_SMALL.skinKey) { cell ->
                cell.width(30f).height(30f)
                isDisabled = true
                addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) { vm.removeDefensePoint() }
                })
            }
            this@SkillView.defensePendingLabel = label("0", Labels.SMALL.skinKey) { it.width(30f).center() }
            this@SkillView.defensePlusBtn = textButton("+", Buttons.GREEN_BUTTON_SMALL.skinKey) { cell ->
                cell.width(30f).height(30f)
                addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) { vm.addDefensePoint() }
                })
            }
            this@SkillView.defensePreviewLabel = label("→ 0 DEF", Labels.SMALL.skinKey) { it.width(90f).right() }
            row()

            // ── Footer — Stack holds three mutually exclusive sections ───────────
            stack { stackCell ->
                // Normal footer: Save + Cancel
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

                // Save confirmation footer
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

                // Cancel confirmation footer
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

            outerCell.expand().center().width(420f)
        }

        // ── Data bindings ────────────────────────────────────────────────────────

        model.onPropertyChange(SkillViewModel::availableSkillPoints) { pts ->
            availablePointsLabel.txt = "Skill Points Available: $pts"
            attackPlusBtn.isDisabled  = pts <= 0
            defensePlusBtn.isDisabled = pts <= 0
        }

        model.onPropertyChange(SkillViewModel::pendingAttackPoints) { pts ->
            attackPendingLabel.txt  = "$pts"
            attackMinusBtn.isDisabled = pts <= 0
            attackPreviewLabel.txt  = "→ ${(model.baseAttackDamage + pts * 2).toInt()} ATK"
        }

        model.onPropertyChange(SkillViewModel::pendingDefensePoints) { pts ->
            defensePendingLabel.txt  = "$pts"
            defenseMinusBtn.isDisabled = pts <= 0
            defensePreviewLabel.txt  = "→ ${(model.baseDefense + pts).toInt()} DEF"
        }

        model.onPropertyChange(SkillViewModel::baseAttackDamage) { base ->
            attackPreviewLabel.txt = "→ ${(base + model.pendingAttackPoints * 2).toInt()} ATK"
        }

        model.onPropertyChange(SkillViewModel::baseDefense) { base ->
            defensePreviewLabel.txt = "→ ${(base + model.pendingDefensePoints).toInt()} DEF"
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
