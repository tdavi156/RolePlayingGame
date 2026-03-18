package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.github.jacks.roleplayinggame.components.BattlePhase
import com.github.jacks.roleplayinggame.configurations.AbilityNode
import com.github.jacks.roleplayinggame.ui.Buttons
import com.github.jacks.roleplayinggame.ui.Labels
import com.github.jacks.roleplayinggame.ui.viewmodels.BattleViewModel
import com.github.jacks.roleplayinggame.ui.widgets.BattleStatBar
import com.github.jacks.roleplayinggame.ui.widgets.battleStatBar
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

class BattleView(
    model: BattleViewModel,
    skin: Skin
) : Table(skin), KTable {

    private val playerStatBar: BattleStatBar
    private val enemyStatBar: BattleStatBar
    private val actionTable: Table
    private val messageTable: Table
    private val messageLabel: Label
    private val spellsButton: TextButton

    // Spell panel
    private val spellPanel: Table
    private val spellGridTable: Table
    private val spellInfoLabel: Label
    private val spellMpLabel: Label
    private val spellFeedbackLabel: Label
    private var spellPanelVisible = false

    init {
        setFillParent(true)

        // Create dark panel background drawable and cache in skin
        if (!skin.has("battlePanelBgd", TextureRegionDrawable::class.java)) {
            val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
            pixmap.setColor(0.15f, 0.15f, 0.15f, 1.0f)
            pixmap.fill()
            val drawable = TextureRegionDrawable(TextureRegion(Texture(pixmap)))
            pixmap.dispose()
            skin.add("battlePanelBgd", drawable, TextureRegionDrawable::class.java)
        }
        val panelBgd = skin.get("battlePanelBgd", TextureRegionDrawable::class.java)

        // === Row 1: Spacer (left) + Enemy stat bar (right) ===
        table { it.expandX() } // spacer
        enemyStatBar = battleStatBar(skin) {
            it.width(Value.percentWidth(0.30f, this@BattleView))
                .padTop(8f).padRight(8f).row()
        }

        // === Row 2: Player stat bar (lower-left) + spacer — fills middle space ===
        table { innerTable ->
            this@BattleView.playerStatBar = battleStatBar(skin) {
                it.width(Value.percentWidth(0.30f, this@BattleView))
                    .expandY().bottom().padLeft(8f).padBottom(8f)
            }
            table { it.expandX() } // spacer
            innerTable.expand().fill().colspan(2).row()
        }

        // === Row 3: Bottom panel ===
        val bottomPanel = Stack()

        // -- Action table (2x2 grid) --
        actionTable = Table(skin).apply {
            defaults().expand().fill().pad(4f).minSize(0f)
            add(TextButton("Attack", skin, Buttons.GREEN_BUTTON_MEDIUM.skinKey).apply {
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent, x: Float, y: Float) {
                        model.onAttack()
                    }
                })
            })
            this@BattleView.spellsButton = TextButton("Spells", skin, Buttons.BLUE_BUTTON_MEDIUM.skinKey).apply {
                isDisabled = true
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent, x: Float, y: Float) {
                        if (!isDisabled) {
                            model.refreshPlayerMana()
                            showSpellPanel(model, skin)
                        }
                    }
                })
            }
            add(spellsButton).row()
            add(TextButton("Items", skin, Buttons.YELLOW_BUTTON_MEDIUM.skinKey).apply {
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent, x: Float, y: Float) {
                        model.onItems()
                    }
                })
            })
            add(TextButton("Escape", skin, Buttons.RED_BUTTON_MEDIUM.skinKey).apply {
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent, x: Float, y: Float) {
                        model.onFlee()
                    }
                })
            })
        }

        // -- Message table (full-width label, tap to dismiss) --
        messageTable = Table(skin).apply {
            this@BattleView.messageLabel = Label("", skin, Labels.DEFAULT.skinKey).apply {
                setAlignment(Align.topLeft)
                wrap = true
            }
            add(this@BattleView.messageLabel).expand().fill().pad(14f)
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent, x: Float, y: Float) {
                    model.onLogDismissed()
                }
            })
            isVisible = false
        }

        // -- Spell panel: left 65% = 2-column spell grid; right 35% = spell info --
        spellGridTable = Table(skin)
        spellInfoLabel = Label("", skin, Labels.SMALL.skinKey).apply {
            wrap = true
            setAlignment(Align.topLeft)
        }
        spellMpLabel = Label("", skin, Labels.SMALL.skinKey)
        spellFeedbackLabel = Label("", skin, Labels.RED.skinKey).apply {
            isVisible = false
        }

        spellPanel = Table(skin).apply {
            // Left: spell button grid
            add(this@BattleView.spellGridTable).expand().fill()
                .width(Value.percentWidth(0.65f, this))

            // Right: info + feedback + Back button
            val infoPanel = Table(skin).apply {
                pad(6f)
                add(this@BattleView.spellInfoLabel).expand().fill().top().row()
                add(this@BattleView.spellMpLabel).left().padBottom(2f).row()
                add(this@BattleView.spellFeedbackLabel).left().padBottom(4f).row()
                add(TextButton("Back", skin, Buttons.GREY_BUTTON_MEDIUM.skinKey).apply {
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent, x: Float, y: Float) {
                            hideSpellPanel()
                        }
                    })
                }).bottom().right().height(32f)
            }
            add(infoPanel).expand().fill()
                .width(Value.percentWidth(0.35f, this))
            isVisible = false
        }

        bottomPanel.add(actionTable)
        bottomPanel.add(messageTable)
        bottomPanel.add(spellPanel)

        val panelWrapper = Table(skin).apply {
            background = panelBgd
            add(bottomPanel).expand().fill()
        }
        add(panelWrapper).colspan(2).expandX().fillX()
            .height(Value.percentHeight(0.25f, this@BattleView))

        // === Data bindings ===
        model.onPropertyChange(BattleViewModel::playerLife) { pct -> playerStatBar.life(pct) }
        model.onPropertyChange(BattleViewModel::enemyLife)  { pct -> enemyStatBar.life(pct) }
        model.onPropertyChange(BattleViewModel::playerMana) { pct -> playerStatBar.mana(pct) }
        model.onPropertyChange(BattleViewModel::enemyMana)  { pct -> enemyStatBar.mana(pct) }
        model.onPropertyChange(BattleViewModel::playerName) { name -> playerStatBar.updateName(name) }
        model.onPropertyChange(BattleViewModel::enemyName)  { name -> enemyStatBar.updateName(name) }
        model.onPropertyChange(BattleViewModel::playerLevel) { level -> playerStatBar.updateLevel(level) }
        model.onPropertyChange(BattleViewModel::enemyLevel)  { level -> enemyStatBar.updateLevel(level) }

        model.onPropertyChange(BattleViewModel::battleLog) { log ->
            if (log.isNotBlank()) messageLabel.txt = log
        }
        model.onPropertyChange(BattleViewModel::battlePhase) { phase ->
            if (phase == BattlePhase.PLAYER_TURN) {
                if (spellPanelVisible) {
                    spellPanelVisible = false
                    spellPanel.isVisible = false
                }
                actionTable.isVisible = true
                messageTable.isVisible = false
            } else {
                actionTable.isVisible = false
                if (spellPanelVisible) {
                    spellPanelVisible = false
                    spellPanel.isVisible = false
                }
                messageTable.isVisible = true
            }
        }
        model.onPropertyChange(BattleViewModel::spellsButtonEnabled) { enabled ->
            spellsButton.isDisabled = !enabled
        }
        model.onPropertyChange(BattleViewModel::availableSpells) { spells ->
            // Rebuild grid if panel is currently open
            if (spellPanelVisible) rebuildSpellGrid(spells, model, skin)
        }
    }

    private fun showSpellPanel(model: BattleViewModel, skin: Skin) {
        spellPanelVisible = true
        actionTable.isVisible = false
        spellPanel.isVisible = true
        spellInfoLabel.setText("")
        spellMpLabel.txt = ""
        spellFeedbackLabel.isVisible = false
        rebuildSpellGrid(model.availableSpells, model, skin)
    }

    private fun hideSpellPanel() {
        spellPanelVisible = false
        spellPanel.isVisible = false
        actionTable.isVisible = true
    }

    /**
     * Builds a 2-column grid of spell TextButtons.
     * Affordable spells use blue; unaffordable use grey.
     * Clicking an affordable spell casts it; clicking an unaffordable one shows "Not enough MP".
     */
    private fun rebuildSpellGrid(spells: List<AbilityNode>, model: BattleViewModel, skin: Skin) {
        spellGridTable.clear()
        spellGridTable.defaults().expand().fill().pad(4f).minSize(0f)

        if (spells.isEmpty()) {
            spellGridTable.add(Label("No spells learned", skin, Labels.SMALL.skinKey)).center()
            return
        }

        // Pad to even count so rows are complete
        val cells = spells.toMutableList<AbilityNode?>()
        if (cells.size % 2 != 0) cells.add(null)

        cells.forEachIndexed { index, node ->
            if (node == null) {
                spellGridTable.add(Table(skin))
            } else {
                val affordable = model.playerCurrentMana >= node.manaCost
                val btnStyle = if (affordable) Buttons.BLUE_BUTTON_MEDIUM.skinKey
                               else Buttons.GREY_BUTTON_MEDIUM.skinKey
                val btn = TextButton(node.name, skin, btnStyle).apply {
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent, x: Float, y: Float) {
                            spellInfoLabel.setText(node.description)
                            spellMpLabel.txt = "MP: ${node.manaCost}"
                            if (affordable) {
                                spellFeedbackLabel.isVisible = false
                                hideSpellPanel()
                                model.onSpellCast(node.id)
                            } else {
                                spellFeedbackLabel.txt = "Not enough MP"
                                spellFeedbackLabel.isVisible = true
                            }
                        }
                    })
                }
                spellGridTable.add(btn)
            }
            if ((index + 1) % 2 == 0) spellGridTable.row()
        }
    }
}

@Scene2dDsl
fun <S> KWidget<S>.battleView(
    model: BattleViewModel,
    skin: Skin = Scene2DSkin.defaultSkin,
    init: BattleView.(S) -> Unit = {}
): BattleView = actor(BattleView(model, skin), init)
