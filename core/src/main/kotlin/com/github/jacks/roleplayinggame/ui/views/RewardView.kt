package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.github.jacks.roleplayinggame.ui.Buttons
import com.github.jacks.roleplayinggame.ui.Labels
import com.github.jacks.roleplayinggame.ui.viewmodels.RewardViewModel
import ktx.scene2d.KTable
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor

class RewardView(
    model: RewardViewModel,
    skin: Skin,
) : Table(skin), KTable {

    private val expLabel: Label
    private val goldLabel: Label
    private val itemRow: Table
    private val itemIcon: Image
    private val itemNameLabel: Label

    init {
        setFillParent(true)

        // Semi-transparent full-screen dim
        if (!skin.has("rewardDimBgd", TextureRegionDrawable::class.java)) {
            val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
            pixmap.setColor(0f, 0f, 0f, 0.65f)
            pixmap.fill()
            val drawable = TextureRegionDrawable(TextureRegion(Texture(pixmap)))
            pixmap.dispose()
            skin.add("rewardDimBgd", drawable, TextureRegionDrawable::class.java)
        }
        background = skin.get("rewardDimBgd", TextureRegionDrawable::class.java)

        // Dark panel background
        if (!skin.has("rewardPanelBgd", TextureRegionDrawable::class.java)) {
            val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
            pixmap.setColor(0.12f, 0.12f, 0.12f, 1f)
            pixmap.fill()
            val drawable = TextureRegionDrawable(TextureRegion(Texture(pixmap)))
            pixmap.dispose()
            skin.add("rewardPanelBgd", drawable, TextureRegionDrawable::class.java)
        }
        val panelBgd = skin.get("rewardPanelBgd", TextureRegionDrawable::class.java)

        // Inner panel
        val panel = Table(skin).apply {
            background = panelBgd
            pad(20f)
            defaults().left().padBottom(8f)

            // Title
            add(Label("Battle Rewards", skin, Labels.MEDIUM.skinKey))
                .colspan(2).center().padBottom(16f).row()

            // EXP row
            add(Label("EXP:", skin, Labels.DEFAULT.skinKey)).right().padRight(8f)
            this@RewardView.expLabel = Label("+0", skin, Labels.YELLOW.skinKey)
            add(this@RewardView.expLabel).row()

            // Gold row
            add(Label("Gold:", skin, Labels.DEFAULT.skinKey)).right().padRight(8f)
            this@RewardView.goldLabel = Label("+0", skin, Labels.YELLOW.skinKey)
            add(this@RewardView.goldLabel).row()

            // Item row (hidden by default)
            this@RewardView.itemIcon = Image().apply {
                setScaling(Scaling.contain)
                setSize(20f, 20f)
            }
            this@RewardView.itemNameLabel = Label("", skin, Labels.DEFAULT.skinKey).apply {
                setAlignment(Align.left)
            }
            this@RewardView.itemRow = Table(skin).apply {
                add(Label("Item:", skin, Labels.DEFAULT.skinKey)).right().padRight(8f)
                add(this@RewardView.itemIcon).size(20f, 20f).padRight(6f)
                add(this@RewardView.itemNameLabel)
                isVisible = false
            }
            add(this@RewardView.itemRow).colspan(2).left().row()

            // Confirm button
            val confirmButton = com.badlogic.gdx.scenes.scene2d.ui.TextButton(
                "OK", skin, Buttons.GREEN_BUTTON_MEDIUM.skinKey
            ).apply {
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent, x: Float, y: Float) {
                        model.onConfirm()
                    }
                })
            }
            add(confirmButton).colspan(2).center().padTop(12f).minWidth(80f)
        }

        add(panel)
    }

    // === Data bindings ===

    init {
        model.onPropertyChange(RewardViewModel::expGained) { exp ->
            expLabel.setText("+$exp")
        }
        model.onPropertyChange(RewardViewModel::goldGained) { gold ->
            goldLabel.setText("+$gold")
        }
        model.onPropertyChange(RewardViewModel::hasItemDrop) { has ->
            itemRow.isVisible = has
        }
        model.onPropertyChange(RewardViewModel::itemDropName) { name ->
            itemNameLabel.setText(name)
        }
        model.onPropertyChange(RewardViewModel::itemDropAtlasKey) { key ->
            itemIcon.drawable = if (key.isNotBlank()) runCatching { skin.getDrawable(key) }.getOrNull() else null
        }
    }
}

@Scene2dDsl
fun <S> KWidget<S>.rewardView(
    model: RewardViewModel,
    skin: Skin = Scene2DSkin.defaultSkin,
    init: RewardView.(S) -> Unit = {}
): RewardView = actor(RewardView(model, skin), init)
