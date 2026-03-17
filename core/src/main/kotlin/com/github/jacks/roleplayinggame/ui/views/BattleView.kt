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
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.github.jacks.roleplayinggame.components.BattlePhase
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

        // === Row 3: Bottom panel with action grid / message area ===
        val bottomPanel = Stack()

        // -- Action table (2x2 grid) --
        actionTable = Table(skin).apply {
            defaults().expand().fill().pad(4f).minSize(0f)
            add(com.badlogic.gdx.scenes.scene2d.ui.TextButton("Attack", skin, Buttons.GREEN_BUTTON_MEDIUM.skinKey).apply {
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent, x: Float, y: Float) {
                        model.onAttack()
                    }
                })
            })
            add(com.badlogic.gdx.scenes.scene2d.ui.TextButton("Skills", skin, Buttons.BLUE_BUTTON_MEDIUM.skinKey).apply {
                isDisabled = true
            }).row()
            add(com.badlogic.gdx.scenes.scene2d.ui.TextButton("Items", skin, Buttons.YELLOW_BUTTON_MEDIUM.skinKey).apply {
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent, x: Float, y: Float) {
                        model.onItems()
                    }
                })
            })
            add(com.badlogic.gdx.scenes.scene2d.ui.TextButton("Flee", skin, Buttons.RED_BUTTON_MEDIUM.skinKey).apply {
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent, x: Float, y: Float) {
                        model.onFlee()
                    }
                })
            })
        }

        // -- Message table (full-width label) --
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

        bottomPanel.add(actionTable)
        bottomPanel.add(messageTable)

        val panelWrapper = Table(skin).apply {
            background = panelBgd
            add(bottomPanel).expand().fill()
        }
        add(panelWrapper).colspan(2).expandX().fillX()
            .height(Value.percentHeight(0.25f, this@BattleView))

        // === Data bindings ===
        model.onPropertyChange(BattleViewModel::playerLife) { pct ->
            playerStatBar.life(pct)
        }
        model.onPropertyChange(BattleViewModel::enemyLife) { pct ->
            enemyStatBar.life(pct)
        }
        model.onPropertyChange(BattleViewModel::playerMana) { pct ->
            playerStatBar.mana(pct)
        }
        model.onPropertyChange(BattleViewModel::enemyMana) { pct ->
            enemyStatBar.mana(pct)
        }
        model.onPropertyChange(BattleViewModel::playerName) { name ->
            playerStatBar.updateName(name)
        }
        model.onPropertyChange(BattleViewModel::enemyName) { name ->
            enemyStatBar.updateName(name)
        }
        model.onPropertyChange(BattleViewModel::playerLevel) { level ->
            playerStatBar.updateLevel(level)
        }
        model.onPropertyChange(BattleViewModel::enemyLevel) { level ->
            enemyStatBar.updateLevel(level)
        }
        model.onPropertyChange(BattleViewModel::battleLog) { log ->
            if (log.isNotBlank()) messageLabel.txt = log
        }
        model.onPropertyChange(BattleViewModel::battlePhase) { phase ->
            if (phase == BattlePhase.PLAYER_TURN) {
                actionTable.isVisible = true
                messageTable.isVisible = false
            } else {
                actionTable.isVisible = false
                messageTable.isVisible = true
            }
        }
    }
}

@Scene2dDsl
fun <S> KWidget<S>.battleView(
    model: BattleViewModel,
    skin: Skin = Scene2DSkin.defaultSkin,
    init: BattleView.(S) -> Unit = {}
): BattleView = actor(BattleView(model, skin), init)
