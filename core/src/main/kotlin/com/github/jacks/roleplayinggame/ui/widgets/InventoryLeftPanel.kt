package com.github.jacks.roleplayinggame.ui.widgets

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.github.jacks.roleplayinggame.configurations.BattleEnchantmentItemData
import com.github.jacks.roleplayinggame.ui.Drawables
import com.github.jacks.roleplayinggame.ui.Labels
import com.github.jacks.roleplayinggame.ui.get
import com.github.jacks.roleplayinggame.ui.viewmodels.InventoryContext
import com.github.jacks.roleplayinggame.ui.viewmodels.InventoryTab
import com.github.jacks.roleplayinggame.ui.viewmodels.InventoryViewModel
import ktx.scene2d.KTable
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor

class InventoryLeftPanel(
    private val model: InventoryViewModel,
    private val skin: Skin,
) : Table(skin), KTable {

    init {
        defaults().minSize(0f)
        rebuildContent()

        model.onPropertyChange(InventoryViewModel::activeTab) { rebuildContent() }
        model.onPropertyChange(InventoryViewModel::activeContext) { rebuildContent() }
        model.onPropertyChange(InventoryViewModel::focusedCharacterIndex) { rebuildContent() }
        model.onPropertyChange(InventoryViewModel::focusedItemIndex) { rebuildContent() }
        model.onPropertyChange(InventoryViewModel::resultMessage) { rebuildContent() }
    }

    private fun rebuildContent() {
        clearListeners()
        clear()
        // Result / warning message takes priority over normal content
        if (model.resultMessage.isNotEmpty()) {
            buildResultMessage(model.resultMessage)
            return
        }
        when (model.activeTab) {
            InventoryTab.EQUIPMENT,
            InventoryTab.CONSUMABLES  -> buildCharacterList()
            InventoryTab.QUEST_ITEMS  -> buildQuestStub()
            InventoryTab.ENCHANTMENTS -> buildEnchantmentDetail()
        }
    }

    private fun buildResultMessage(message: String) {
        add(Label(message, skin, Labels.SMALL.skinKey)).expandX().fillX().left().pad(4f).row()
        add(Label("Press Enter to continue", skin, Labels.SMALL.skinKey)).expandX().left().padLeft(4f).row()
        add().expand().row()
        addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                model.dismissResult()
            }
        })
    }

    // --- Equipment / Consumables: character list ---

    private fun buildCharacterList() {
        val characters = model.partyCharacters
        if (characters.isEmpty()) {
            add(Label("No party", skin, Labels.SMALL.skinKey)).expand().center()
            return
        }
        characters.forEachIndexed { index, charInfo ->
            val focused = model.activeContext == InventoryContext.LEFT
                    && index == model.focusedCharacterIndex
            val row = buildCharacterRow(charInfo.name, charInfo.hpPct, focused)
            val charIndex = index
            row.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent, x: Float, y: Float) {
                    model.focusedCharacterIndex = charIndex
                    model.activeContext = InventoryContext.LEFT
                    model.confirmPendingAction(charIndex)
                }
            })
            add(row).expandX().fillX().padBottom(3f).row()
        }
        // fill remaining space so rows stay top-aligned
        add().expand().row()
    }

    private fun buildCharacterRow(name: String, hpPct: Float, focused: Boolean): Table {
        val row = Table(skin).apply { defaults().minSize(0f) }
        if (focused) row.background = skin[Drawables.BACKGROUND_GREY]

        // Portrait placeholder
        val portrait = Image(skin[Drawables.PLAYER])
        row.add(portrait).size(PORTRAIT_SIZE).padLeft(3f).padRight(4f)

        // Name + HP bar stacked vertically
        val infoCol = Table(skin).apply { defaults().minSize(0f) }
        infoCol.add(Label(name, skin, Labels.SMALL.skinKey)).expandX().left().row()

        val hpBarBg = Image(skin[Drawables.BAR_GREY_THICK])
        val hpBarFill = Image(skin[Drawables.BAR_GREEN_THICK]).also { fill ->
            fill.setOrigin(0f, 0f)
            fill.setScale(MathUtils.clamp(hpPct, 0f, 1f), 1f)
        }
        val barStack = Table(skin)
        barStack.add(hpBarBg).expandX().fillX().height(BAR_HEIGHT)
        barStack.addActor(hpBarFill)
        infoCol.add(barStack).expandX().fillX().row()

        row.add(infoCol).expandX().fillX().padRight(3f)
        return row
    }

    // --- Quest Items: stub display ---

    private fun buildQuestStub() {
        val lines = listOf(
            "Quest ID:          ----",
            "Quest Name:        ----",
            "Quest Description: ----",
            "Quest Progress:    ----",
        )
        lines.forEach { line ->
            add(Label(line, skin, Labels.SMALL.skinKey)).expandX().left().padLeft(4f).padTop(3f).row()
        }
        add().expand().row()
    }

    // --- Enchantments: focused item detail ---

    private fun buildEnchantmentDetail() {
        val items = model.activeItemList
        val safeIndex = model.focusedItemIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        if (items.isEmpty()) {
            add(Label("No enchantment selected", skin, Labels.SMALL.skinKey)).expand().center()
            return
        }
        val entry = items[safeIndex]
        val enchant = entry.item as? BattleEnchantmentItemData ?: return

        add(Label(enchant.itemName, skin, Labels.SMALL.skinKey)).expandX().left().pad(4f, 4f, 2f, 4f).row()

        val separator = Table(skin)
        separator.background = skin[Drawables.BAR_BLACK_THIN]
        add(separator).expandX().fillX().height(1f).padBottom(3f).row()

        add(Label(enchant.itemDescription, skin, Labels.SMALL.skinKey)).expandX().left().pad(0f, 4f, 3f, 4f).row()

        val statLine = "${enchant.statType.name.lowercase().replace('_', ' ')}: +${enchant.statValue}"
        add(Label(statLine, skin, Labels.SMALL.skinKey)).expandX().left().padLeft(4f).row()

        add().expand().row()
    }

    companion object {
        private const val PORTRAIT_SIZE = 20f
        private const val BAR_HEIGHT = 5f
    }
}

@Scene2dDsl
fun <S> KWidget<S>.inventoryLeftPanel(
    model: InventoryViewModel,
    skin: Skin = Scene2DSkin.defaultSkin,
    init: InventoryLeftPanel.(S) -> Unit = {}
): InventoryLeftPanel = actor(InventoryLeftPanel(model, skin), init)
