package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.github.jacks.roleplayinggame.ui.Buttons
import com.github.jacks.roleplayinggame.ui.Drawables
import com.github.jacks.roleplayinggame.ui.Labels
import com.github.jacks.roleplayinggame.ui.get
import com.github.jacks.roleplayinggame.ui.viewmodels.AbilityNodeUiState
import com.github.jacks.roleplayinggame.ui.viewmodels.AbilityViewModel
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

class AbilityView(
    val model: AbilityViewModel,
    skin: Skin,
) : Table(skin), KTable {

    private lateinit var pointsLabel: Label
    private lateinit var characterLabel: Label
    private lateinit var treeContainer: Table
    private lateinit var normalFooter: Table
    private lateinit var saveConfirmFooter: Table
    private lateinit var cancelConfirmFooter: Table

    // Placeholder drawables for node visual states (colored 1x1 pixel textures)
    private val skilledDrawable: TextureRegionDrawable
    private val pendingDrawable: TextureRegionDrawable
    private val unlockableDrawable: TextureRegionDrawable
    private val lockedDrawable: TextureRegionDrawable
    private val lineDrawable: TextureRegionDrawable

    init {
        // Build placeholder drawables
        skilledDrawable   = makeColorDrawable(0.3f, 0.8f, 0.3f, 1f)   // green — skilled
        pendingDrawable   = makeColorDrawable(0.9f, 0.8f, 0.1f, 1f)   // yellow — pending
        unlockableDrawable = makeColorDrawable(0.4f, 0.6f, 0.9f, 1f)  // blue — unlockable
        lockedDrawable    = makeColorDrawable(0.3f, 0.3f, 0.3f, 0.8f) // dark grey — locked
        lineDrawable      = makeColorDrawable(0.5f, 0.5f, 0.5f, 1f)   // grey — connecting line

        setFillParent(true)

        table { outerCell ->
            background = skin[Drawables.FRAME_BGD]
            pad(12f)

            // Title
            label("Ability Tree", Labels.MEDIUM.skinKey) { it.colspan(3).center().padBottom(6f) }
            row()

            // Header: points + character switcher
            this@AbilityView.pointsLabel =
                label("Ability Points: 0", Labels.SMALL.skinKey) { it.left().expandX() }
            textButton("<", Buttons.GREY_BUTTON_SMALL.skinKey) { cell ->
                cell.width(30f).height(30f)
                addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) {
                        this@AbilityView.model.switchCharacter(-1)
                    }
                })
            }
            this@AbilityView.characterLabel =
                label("Character 1", Labels.SMALL.skinKey) { it.padLeft(4f).padRight(4f) }
            textButton(">", Buttons.GREY_BUTTON_SMALL.skinKey) { cell ->
                cell.width(30f).height(30f)
                addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) {
                        this@AbilityView.model.switchCharacter(1)
                    }
                })
            }
            row()

            // Tree panel (scrollable)
            this@AbilityView.treeContainer = Table(skin)
            val scrollPane = ScrollPane(this@AbilityView.treeContainer).apply {
                setScrollingDisabled(true, false)
                setOverscroll(false, false)
            }
            add(scrollPane).colspan(4).expand().fill().padTop(8f).padBottom(8f)
            row()

            // Footer — Stack with three mutually exclusive sections
            stack { stackCell ->
                this@AbilityView.normalFooter = table {
                    textButton("Save", Buttons.GREEN_BUTTON_MEDIUM.skinKey) { cell ->
                        cell.width(100f).height(35f).padRight(10f)
                        addListener(object : ChangeListener() {
                            override fun changed(event: ChangeEvent, actor: Actor) { this@AbilityView.model.requestSave() }
                        })
                    }
                    textButton("Cancel", Buttons.RED_BUTTON_MEDIUM.skinKey) { cell ->
                        cell.width(100f).height(35f)
                        addListener(object : ChangeListener() {
                            override fun changed(event: ChangeEvent, actor: Actor) { this@AbilityView.model.requestCancel() }
                        })
                    }
                }

                this@AbilityView.saveConfirmFooter = table {
                    label("Changes cannot be undone. Confirm?", Labels.SMALL.skinKey) {
                        it.colspan(2).center().padBottom(4f)
                    }
                    row()
                    textButton("Yes", Buttons.GREEN_BUTTON_MEDIUM.skinKey) { cell ->
                        cell.width(80f).height(35f).padRight(10f)
                        addListener(object : ChangeListener() {
                            override fun changed(event: ChangeEvent, actor: Actor) { this@AbilityView.model.confirmSave() }
                        })
                    }
                    textButton("No", Buttons.RED_BUTTON_MEDIUM.skinKey) { cell ->
                        cell.width(80f).height(35f)
                        addListener(object : ChangeListener() {
                            override fun changed(event: ChangeEvent, actor: Actor) { this@AbilityView.model.cancelSave() }
                        })
                    }
                    isVisible = false
                }

                this@AbilityView.cancelConfirmFooter = table {
                    label("Discard unsaved changes?", Labels.SMALL.skinKey) {
                        it.colspan(2).center().padBottom(4f)
                    }
                    row()
                    textButton("Yes", Buttons.GREEN_BUTTON_MEDIUM.skinKey) { cell ->
                        cell.width(80f).height(35f).padRight(10f)
                        addListener(object : ChangeListener() {
                            override fun changed(event: ChangeEvent, actor: Actor) { this@AbilityView.model.confirmCancel() }
                        })
                    }
                    textButton("No", Buttons.RED_BUTTON_MEDIUM.skinKey) { cell ->
                        cell.width(80f).height(35f)
                        addListener(object : ChangeListener() {
                            override fun changed(event: ChangeEvent, actor: Actor) { this@AbilityView.model.dismissCancelConfirm() }
                        })
                    }
                    isVisible = false
                }

                stackCell.colspan(4).center().padTop(4f).height(70f)
            }

            outerCell.expand().center().width(440f)
        }

        // === Data bindings ===

        model.onPropertyChange(AbilityViewModel::abilityPoints) { pts ->
            pointsLabel.txt = "Ability Points: $pts"
        }

        model.onPropertyChange(AbilityViewModel::characterName) { name ->
            characterLabel.txt = name
        }

        model.onPropertyChange(AbilityViewModel::nodes) { nodeStates ->
            rebuildTree(nodeStates, skin)
        }

        model.onPropertyChange(AbilityViewModel::showSaveConfirm) { show ->
            normalFooter.isVisible      = !show && !model.showCancelConfirm
            saveConfirmFooter.isVisible = show
        }

        model.onPropertyChange(AbilityViewModel::showCancelConfirm) { show ->
            normalFooter.isVisible        = !show && !model.showSaveConfirm
            cancelConfirmFooter.isVisible = show
        }
    }

    private fun rebuildTree(nodeStates: List<AbilityNodeUiState>, skin: Skin) {
        treeContainer.clear()
        treeContainer.defaults().center()

        nodeStates.forEachIndexed { index, state ->
            // Connecting line from the previous node (skip for first)
            if (index > 0) {
                val lineBgd = if (!state.isLocked) lineDrawable.tint(Color(0.7f, 0.7f, 0.7f, 1f))
                              else lineDrawable.tint(Color(0.3f, 0.3f, 0.3f, 1f))
                treeContainer.add(Table(skin).apply { background = lineBgd }).width(4f).height(24f)
                treeContainer.row()
            }

            // Node cell
            val nodeBackground = when {
                state.isSkilled   -> skilledDrawable
                state.isPending   -> pendingDrawable
                state.isUnlockable -> unlockableDrawable
                else              -> lockedDrawable
            }

            val nodeRow = Table(skin).apply {
                // Icon placeholder: 64x64 colored rectangle
                val iconTable = Table(skin).apply { background = nodeBackground }
                add(iconTable).size(64f).padRight(8f)

                // Name and action buttons
                val infoCol = Table(skin).apply {
                    add(Label(state.node.name, skin, Labels.SMALL.skinKey)).left().row()
                    val descLabel = Label(state.node.description, skin, Labels.TINY.skinKey).apply {
                        wrap = true
                        setAlignment(Align.left)
                    }
                    add(descLabel).left().width(160f).row()

                    // Mana cost
                    add(Label("MP: ${state.node.manaCost}", skin, Labels.TINY.skinKey)).left().padTop(2f).row()

                    // +/- buttons
                    val btnRow = Table(skin)
                    if (state.isPending) {
                        btnRow.add(TextButton("-", skin, Buttons.RED_BUTTON_SMALL.skinKey).apply {
                            addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent, actor: Actor) {
                                    model.removePending(state.node.id)
                                }
                            })
                        }).size(30f).padRight(4f)
                    } else if (state.isUnlockable) {
                        btnRow.add(TextButton("+", skin, Buttons.GREEN_BUTTON_SMALL.skinKey).apply {
                            addListener(object : ChangeListener() {
                                override fun changed(event: ChangeEvent, actor: Actor) {
                                    model.addPending(state.node.id)
                                }
                            })
                        }).size(30f)
                    } else if (state.isSkilled) {
                        btnRow.add(Label("✓ Learned", skin, Labels.TINY.skinKey))
                    } else {
                        btnRow.add(Label("Locked", skin, Labels.TINY.skinKey))
                    }
                    add(btnRow).left().padTop(4f)
                }
                add(infoCol).left().expand()
            }

            treeContainer.add(nodeRow).left().fillX().padLeft(16f).padRight(16f)
            treeContainer.row()
        }
    }

    private fun makeColorDrawable(r: Float, g: Float, b: Float, a: Float): TextureRegionDrawable {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(r, g, b, a)
        pixmap.fill()
        val drawable = TextureRegionDrawable(TextureRegion(Texture(pixmap)))
        pixmap.dispose()
        return drawable
    }

    private fun TextureRegionDrawable.tint(color: Color): TextureRegionDrawable {
        return this.tint(color) as TextureRegionDrawable
    }
}

@Scene2dDsl
fun <S> KWidget<S>.abilityView(
    model: AbilityViewModel,
    skin: Skin = Scene2DSkin.defaultSkin,
    init: AbilityView.(S) -> Unit = {}
): AbilityView = actor(AbilityView(model, skin), init)
