package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.github.jacks.roleplayinggame.configurations.battleEnchantmentItemById
import com.github.jacks.roleplayinggame.configurations.consumableItemById
import com.github.jacks.roleplayinggame.configurations.equipmentItemById
import com.github.jacks.roleplayinggame.configurations.questItemById
import com.github.jacks.roleplayinggame.events.ShopBuyConfirmedEvent
import com.github.jacks.roleplayinggame.events.ShopClosedEvent
import com.github.jacks.roleplayinggame.events.ShopSellConfirmedEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.ui.Colors
import com.github.jacks.roleplayinggame.ui.Drawables
import com.github.jacks.roleplayinggame.ui.Labels
import com.github.jacks.roleplayinggame.ui.get
import com.github.jacks.roleplayinggame.ui.viewmodels.ShopBuyRow
import com.github.jacks.roleplayinggame.ui.viewmodels.ShopMode
import com.github.jacks.roleplayinggame.ui.viewmodels.ShopSellRow
import com.github.jacks.roleplayinggame.ui.viewmodels.ShopTab
import com.github.jacks.roleplayinggame.ui.viewmodels.ShopViewModel
import com.github.jacks.roleplayinggame.ui.viewmodels.ShopViewModel.Companion.NO_PENDING
import ktx.scene2d.KTable
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor

class ShopView(
    val model: ShopViewModel,
    skin: Skin,
) : Table(skin), KTable {

    // ─── Sub-tables ──────────────────────────────────────────────────────────

    private val shopPanel     = Table(skin).apply { defaults().minSize(0f) }
    private val topBar        = Table(skin).apply { defaults().minSize(0f) }
    private val tabBar        = Table(skin).apply { defaults().minSize(0f) }
    private val itemListTable = Table(skin).apply { defaults().minSize(0f) }
    private val infoTable     = Table(skin).apply { defaults().minSize(0f) }
    private val insufficientLabel: Label

    private val scrollPane = ScrollPane(itemListTable).apply {
        setScrollingDisabled(true, false)
        setOverscroll(false, false)
        setFadeScrollBars(false)
    }

    private val tabLabels     = mutableMapOf<ShopTab, Label>()
    private var buyModeLabel: Label?  = null
    private var sellModeLabel: Label? = null
    private var goldLabel: Label?     = null

    // ─── Init ────────────────────────────────────────────────────────────────

    init {
        setFillParent(true)
        defaults().minSize(0f)

        // Right-side overlay — push the panel to the right edge
        add().expandX()
        add(shopPanel).expandY().fillY().width(PANEL_WIDTH)

        // Panel background
        shopPanel.background = skin[Drawables.FRAME_BGD]

        // ── Top bar: BUY | SELL  ·  Gold: Xg ──
        val buyLbl  = Label("BUY",  skin, Labels.SMALL.skinKey)
        val sellLbl = Label("SELL", skin, Labels.SMALL.skinKey)
        val goldLbl = Label("Gold: 0g", skin, Labels.SMALL.skinKey)
        buyModeLabel  = buyLbl
        sellModeLabel = sellLbl
        goldLabel     = goldLbl

        buyLbl.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                model.shopMode        = ShopMode.BUY
                model.focusedItemIndex = 0
                model.pendingItemId   = NO_PENDING
            }
        })
        sellLbl.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                model.shopMode        = ShopMode.SELL
                model.focusedItemIndex = 0
                model.pendingItemId   = NO_PENDING
            }
        })

        topBar.add(buyLbl).padLeft(4f).padRight(2f)
        topBar.add(sellLbl).padRight(4f)
        topBar.add(goldLbl).expandX().right().padRight(4f)
        shopPanel.add(topBar).expandX().fillX().padTop(2f).padBottom(1f).row()

        // ── Tab bar ──
        buildTabBar()
        shopPanel.add(tabBar).expandX().fillX().padBottom(1f).row()

        // ── Insufficient gold popup — hidden by default ──
        insufficientLabel = Label("Not enough gold!", skin, Labels.SMALL_RED_BGD.skinKey)
        insufficientLabel.isVisible = false
        shopPanel.add(insufficientLabel).expandX().fillX().padBottom(1f).row()

        // ── Item list (scrollable) ──
        shopPanel.add(scrollPane).expand().fill().row()

        // ── Description panel (fixed height, always visible) ──
        infoTable.background = skin[Drawables.FRAME_FGD]
        shopPanel.add(infoTable).expandX().fillX().height(INFO_HEIGHT).row()

        // ─── ViewModel bindings ──────────────────────────────────────────────

        model.onPropertyChange(ShopViewModel::shopMode) {
            updateModeHighlights()
            rebuildItemList()
            rebuildInfoPanel()
        }
        model.onPropertyChange(ShopViewModel::activeTab) {
            updateTabHighlights()
            rebuildItemList()
            rebuildInfoPanel()
        }
        model.onPropertyChange(ShopViewModel::focusedItemIndex) {
            updateRowHighlights()
            rebuildInfoPanel()
        }
        model.onPropertyChange(ShopViewModel::pendingItemId)    { rebuildItemList() }
        model.onPropertyChange(ShopViewModel::pendingQuantity)  { rebuildItemList() }
        model.onPropertyChange(ShopViewModel::insufficientGoldVisible) { visible ->
            insufficientLabel.isVisible = visible
            if (visible) {
                insufficientLabel.clearActions()
                insufficientLabel.addAction(Actions.sequence(
                    Actions.delay(INSUFFICIENT_GOLD_DURATION),
                    Actions.run { model.insufficientGoldVisible = false }
                ))
            }
        }
        model.onPropertyChange(ShopViewModel::refreshToken) {
            updateGoldLabel()
            rebuildItemList()
            rebuildInfoPanel()
        }

        // Initial render
        updateModeHighlights()
        updateTabHighlights()
        rebuildItemList()
        rebuildInfoPanel()
    }

    // ─── Top bar ─────────────────────────────────────────────────────────────

    private fun updateModeHighlights() {
        val labelStyle = skin.get(Labels.SMALL.skinKey, Label.LabelStyle::class.java)
        buyModeLabel?.style  = labelStyle
        sellModeLabel?.style = labelStyle
        buyModeLabel?.color  = if (model.shopMode == ShopMode.BUY)  Colors.ORANGE.color else Color.WHITE
        sellModeLabel?.color = if (model.shopMode == ShopMode.SELL) Colors.ORANGE.color else Color.WHITE
        updateGoldLabel()
    }

    private fun updateGoldLabel() {
        goldLabel?.setText("Gold: ${model.currentGold}g")
    }

    // ─── Tab bar ─────────────────────────────────────────────────────────────

    private fun buildTabBar() {
        ShopTab.entries.forEach { tab ->
            val lbl = Label(tab.displayName, skin, Labels.SMALL.skinKey)
            tabLabels[tab] = lbl
            lbl.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent, x: Float, y: Float) {
                    model.activeTab        = tab
                    model.focusedItemIndex = 0
                    model.pendingItemId    = NO_PENDING
                }
            })
            tabBar.add(lbl).expandX().pad(1f)
        }
        updateTabHighlights()
    }

    private fun updateTabHighlights() {
        val labelStyle = skin.get(Labels.SMALL.skinKey, Label.LabelStyle::class.java)
        tabLabels.forEach { (tab, lbl) ->
            lbl.style = labelStyle
            lbl.color = if (tab == model.activeTab) Colors.ORANGE.color else Color.WHITE
        }
    }

    // ─── Item list ────────────────────────────────────────────────────────────

    private fun rebuildItemList() {
        itemListTable.clear()
        when (model.shopMode) {
            ShopMode.BUY  -> buildBuyList()
            ShopMode.SELL -> buildSellList()
        }
    }

    private fun buildBuyList() {
        val items = model.buyItemList
        if (items.isEmpty()) {
            itemListTable.add(Label("(empty)", skin, Labels.SMALL.skinKey)).expandX().left().pad(3f).row()
            return
        }
        items.forEachIndexed { index, row ->
            val isLeave   = row.id == NO_PENDING
            val isFocused = index == model.focusedItemIndex
            val isPending = !isLeave && row.id == model.pendingItemId

            val rowTable = Table(skin).apply { defaults().minSize(0f) }
            if (isFocused) rowTable.background = skin[Drawables.ROW_HIGHLIGHT]

            if (!isLeave) addIconToRow(rowTable, row.iconKey)

            when {
                isPending -> {
                    rowTable.add(Label(row.name, skin, Labels.SMALL.skinKey)).expandX().left()
                    rowTable.add(Label("< ${model.pendingQuantity} >", skin, Labels.SMALL.skinKey)).right().padRight(3f)
                    rowTable.add(Label("${row.goldValue * model.pendingQuantity}g", skin, Labels.SMALL.skinKey)).right().padRight(3f)
                }
                isLeave -> {
                    rowTable.add(Label("Leave", skin, Labels.SMALL.skinKey)).expandX().left().colspan(2).padLeft(4f)
                }
                else -> {
                    val nameColor = if (row.canAfford) Color.WHITE else Color(0.5f, 0.5f, 0.5f, 1f)
                    rowTable.add(Label(row.name, skin, Labels.SMALL.skinKey).apply { color = nameColor }).expandX().left()
                    rowTable.add(Label("${row.goldValue}g", skin, Labels.SMALL.skinKey).apply { color = nameColor }).right().padRight(3f)
                }
            }

            attachBuyRowListeners(rowTable, index, row)
            itemListTable.add(rowTable).expandX().fillX().row()
        }
    }

    private fun buildSellList() {
        val items = model.sellItemList
        if (items.isEmpty()) {
            itemListTable.add(Label("(empty)", skin, Labels.SMALL.skinKey)).expandX().left().pad(3f).row()
            return
        }
        items.forEachIndexed { index, row ->
            val isLeave   = row.id == NO_PENDING
            val isFocused = index == model.focusedItemIndex
            val isPending = !isLeave && row.id == model.pendingItemId

            val rowTable = Table(skin).apply { defaults().minSize(0f) }
            if (isFocused) rowTable.background = skin[Drawables.ROW_HIGHLIGHT]

            if (!isLeave) addIconToRow(rowTable, row.iconKey)

            when {
                isPending -> {
                    rowTable.add(Label(row.name, skin, Labels.SMALL.skinKey)).expandX().left()
                    rowTable.add(Label("< ${model.pendingQuantity} >", skin, Labels.SMALL.skinKey)).right().padRight(3f)
                    rowTable.add(Label("${row.sellPrice * model.pendingQuantity}g", skin, Labels.SMALL.skinKey)).right().padRight(3f)
                }
                isLeave -> {
                    rowTable.add(Label("Leave", skin, Labels.SMALL.skinKey)).expandX().left().colspan(2).padLeft(4f)
                }
                else -> {
                    val nameColor = if (row.sellable) Color.WHITE else Color(0.5f, 0.5f, 0.5f, 1f)
                    rowTable.add(Label(row.name, skin, Labels.SMALL.skinKey).apply { color = nameColor }).expandX().left()
                    if (row.availableQty > 1) {
                        rowTable.add(Label("x${row.availableQty}", skin, Labels.SMALL.skinKey).apply { color = nameColor }).right().padRight(2f)
                    }
                    if (row.sellable) {
                        rowTable.add(Label("${row.sellPrice}g", skin, Labels.SMALL.skinKey).apply { color = nameColor }).right().padRight(3f)
                    }
                }
            }

            attachSellRowListeners(rowTable, index, row)
            itemListTable.add(rowTable).expandX().fillX().row()
        }
    }

    // ─── Row listeners (mouse: hover, click, double-click) ───────────────────

    private fun attachBuyRowListeners(rowTable: Table, index: Int, row: ShopBuyRow) {
        rowTable.addListener(object : ClickListener() {
            override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                if (pointer == -1) model.focusedItemIndex = index
            }
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                model.focusedItemIndex = index
                if (row.id == NO_PENDING) {
                    stage?.fire(ShopClosedEvent())
                    return
                }
                if (!row.canAfford) {
                    model.insufficientGoldVisible = true
                    return
                }
                if (tapCount >= 2 && model.pendingItemId == NO_PENDING) {
                    model.pendingItemId   = row.id
                    model.pendingQuantity = 1
                }
            }
        })
    }

    private fun attachSellRowListeners(rowTable: Table, index: Int, row: ShopSellRow) {
        rowTable.addListener(object : ClickListener() {
            override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                if (pointer == -1) model.focusedItemIndex = index
            }
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                model.focusedItemIndex = index
                if (row.id == NO_PENDING) {
                    stage?.fire(ShopClosedEvent())
                    return
                }
                if (!row.sellable) return
                if (tapCount >= 2 && model.pendingItemId == NO_PENDING) {
                    model.pendingItemId   = row.id
                    model.pendingQuantity = 1
                }
            }
        })
    }

    private fun updateRowHighlights() {
        itemListTable.cells.forEachIndexed { index, cell ->
            val actor = cell.actor
            if (actor is Table) {
                actor.background = if (index == model.focusedItemIndex) skin[Drawables.ROW_HIGHLIGHT] else null
            }
        }
    }

    // ─── Icon helper ─────────────────────────────────────────────────────────

    private fun addIconToRow(row: Table, atlasKey: String) {
        if (atlasKey.isNotEmpty() && skin.has(atlasKey, Drawable::class.java)) {
            val icon = Image(skin.getDrawable(atlasKey)).apply { setScaling(Scaling.fit) }
            row.add(icon).size(ICON_SIZE).padLeft(2f).padRight(3f)
        } else {
            row.add().size(ICON_SIZE).padLeft(2f).padRight(3f)
        }
    }

    // ─── Description panel ───────────────────────────────────────────────────

    private fun rebuildInfoPanel() {
        infoTable.clear()
        val text = buildDescription()
        val lbl = Label(text, skin, Labels.SMALL.skinKey)
        lbl.setAlignment(Align.topLeft)
        lbl.wrap = true
        infoTable.add(lbl).expand().top().left().pad(3f).width(PANEL_WIDTH - 12f)
    }

    private fun buildDescription(): String {
        return when (model.shopMode) {
            ShopMode.BUY -> {
                val list = model.buyItemList
                if (list.isEmpty()) return ""
                val safe = model.focusedItemIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))
                val row = list[safe]
                if (row.id == NO_PENDING) "Exit the shop." else descriptionForId(row.id)
            }
            ShopMode.SELL -> {
                val list = model.sellItemList
                if (list.isEmpty()) return ""
                val safe = model.focusedItemIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))
                val row = list[safe]
                if (row.id == NO_PENDING) "Exit the shop." else descriptionForId(row.id)
            }
        }
    }

    private fun descriptionForId(itemId: Int): String = when (itemId) {
        in 1000..1999 -> equipmentItemById(itemId)?.stats?.entries
            ?.joinToString("\n") { (stat, value) -> "${stat.name.lowercase().replace('_', ' ')}: +$value" } ?: ""
        in 2000..2999 -> consumableItemById(itemId)?.let { "Restores ${it.statValue} ${it.statType.name}" } ?: ""
        in 3000..3999 -> questItemById(itemId)?.itemDescription ?: ""
        in 4000..4999 -> battleEnchantmentItemById(itemId)
            ?.let { "${it.itemDescription}\n${it.statType.name}: +${it.statValue}" } ?: ""
        else -> ""
    }

    // ─── Confirm / fire transaction ──────────────────────────────────────────

    /** Called by the keyboard handler when Enter is pressed on an active quantity selector. */
    fun confirmPending() {
        val itemId = model.pendingItemId
        if (itemId == NO_PENDING) return
        val qty = model.pendingQuantity
        when (model.shopMode) {
            ShopMode.BUY  -> stage?.fire(ShopBuyConfirmedEvent(itemId, qty))
            ShopMode.SELL -> stage?.fire(ShopSellConfirmedEvent(itemId, qty))
        }
    }

    // ─── Companions ──────────────────────────────────────────────────────────

    companion object {
        private const val PANEL_WIDTH                = 280f
        private const val ICON_SIZE                  = 10f
        private const val INFO_HEIGHT                = 45f
        private const val INSUFFICIENT_GOLD_DURATION = 1.5f

        private val ShopTab.displayName: String
            get() = when (this) {
                ShopTab.EQUIPMENT    -> "Equip"
                ShopTab.CONSUMABLES  -> "Items"
                ShopTab.QUEST_ITEMS  -> "Quest"
                ShopTab.ENCHANTMENTS -> "Ench."
            }
    }
}

@Scene2dDsl
fun <S> KWidget<S>.shopView(
    model: ShopViewModel,
    skin: Skin = Scene2DSkin.defaultSkin,
    init: ShopView.(S) -> Unit = {}
): ShopView = actor(ShopView(model, skin), init)
