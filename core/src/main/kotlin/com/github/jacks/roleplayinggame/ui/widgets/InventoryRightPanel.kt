package com.github.jacks.roleplayinggame.ui.widgets

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Scaling
import com.github.jacks.roleplayinggame.configurations.BattleEnchantmentItemData
import com.github.jacks.roleplayinggame.configurations.ConsumableItemData
import com.github.jacks.roleplayinggame.configurations.EquipmentItemData
import com.github.jacks.roleplayinggame.configurations.QuestItemData
import com.github.jacks.roleplayinggame.systems.InventorySystem
import com.github.jacks.roleplayinggame.ui.Drawables
import com.github.jacks.roleplayinggame.ui.Labels
import com.github.jacks.roleplayinggame.ui.get
import com.github.jacks.roleplayinggame.ui.viewmodels.InventoryTab
import com.github.jacks.roleplayinggame.ui.viewmodels.InventoryViewModel
import ktx.scene2d.KTable
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor

class InventoryRightPanel(
    private val model: InventoryViewModel,
    private val skin: Skin,
) : Table(skin), KTable {

    private val tabLabels    = mutableMapOf<InventoryTab, Label>()
    private val itemListTable = Table(skin).apply { defaults().minSize(0f) }
    private val scrollPane    = ScrollPane(itemListTable).apply {
        setScrollingDisabled(true, false)
        setOverscroll(false, false)
        setFadeScrollBars(false)
    }
    private val infoTable = Table(skin).apply { defaults().minSize(0f) }

    init {
        defaults().minSize(0f)

        buildTabBar()
        add(scrollPane).expand().fill().row()
        add(infoTable).expandX().fillX().height(INFO_HEIGHT).row()

        model.onPropertyChange(InventoryViewModel::activeTab) {
            updateTabHighlights()
            rebuildItemList()
            rebuildInfoPanel()
        }
        model.onPropertyChange(InventoryViewModel::focusedItemIndex) {
            updateRowHighlights()
            rebuildInfoPanel()
        }
        model.onPropertyChange(InventoryViewModel::showingActionMenu) { rebuildItemList() }
        model.onPropertyChange(InventoryViewModel::actionMenuFocusIndex) { rebuildItemList() }

        rebuildItemList()
        rebuildInfoPanel()
    }

    // --- Tab bar ---

    private fun buildTabBar() {
        val tabRow = Table(skin).apply { defaults().minSize(0f) }
        InventoryTab.entries.forEach { tab ->
            val lbl = Label(tab.displayName, skin, Labels.SMALL.skinKey)
            tabLabels[tab] = lbl
            lbl.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent, x: Float, y: Float) {
                    model.activeTab = tab
                    model.focusedItemIndex = 0
                    model.showingActionMenu = false
                }
            })
            tabRow.add(lbl).expandX().pad(2f)
        }
        add(tabRow).expandX().fillX().padBottom(1f).row()
        updateTabHighlights()
    }

    private fun updateTabHighlights() {
        tabLabels.forEach { (tab, lbl) ->
            val styleKey = if (tab == model.activeTab) Labels.SMALL_WHITE_BGD.skinKey else Labels.SMALL.skinKey
            lbl.style = skin.get(styleKey, Label.LabelStyle::class.java)
        }
    }

    // --- Item list ---

    private fun rebuildItemList() {
        itemListTable.clear()
        val items = model.activeItemList
        if (items.isEmpty()) {
            itemListTable.add(Label("(empty)", skin, Labels.SMALL.skinKey)).expandX().left().pad(3f).row()
            return
        }
        items.forEachIndexed { index, entry ->
            val row = Table(skin).apply { defaults().minSize(0f) }
            val isFocused = index == model.focusedItemIndex
            if (isFocused) row.background = skin[Drawables.BACKGROUND_GREY]

            val item = entry.item as Any
            addIconToRow(row, item.uiAtlasKey())

            if (isFocused && model.showingActionMenu) {
                // Show inline action options instead of normal name+qty
                row.add(Label(item.displayName(), skin, Labels.SMALL.skinKey)).expandX().left()
                val actionLabel = when (model.activeTab) {
                    InventoryTab.EQUIPMENT   -> "Equip"
                    InventoryTab.CONSUMABLES -> "Use"
                    else -> ""
                }
                if (actionLabel.isNotEmpty()) {
                    val actionStyle = if (model.actionMenuFocusIndex == 0) Labels.SMALL_WHITE_BGD.skinKey else Labels.SMALL.skinKey
                    val cancelStyle = if (model.actionMenuFocusIndex == 1) Labels.SMALL_WHITE_BGD.skinKey else Labels.SMALL.skinKey
                    row.add(Label(actionLabel, skin, actionStyle)).right().padRight(2f)
                    row.add(Label("Cancel",    skin, cancelStyle)).right().padRight(3f)
                }
            } else {
                row.add(Label(item.displayName(), skin, Labels.SMALL.skinKey)).expandX().left()
                if (entry.quantity > 1) {
                    row.add(Label("x${entry.quantity}", skin, Labels.SMALL.skinKey)).right().padRight(3f)
                }
            }

            val rowIndex = index
            row.addListener(object : ClickListener() {
                override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                    if (pointer == -1) model.focusedItemIndex = rowIndex
                }
                override fun clicked(event: InputEvent, x: Float, y: Float) {
                    model.focusedItemIndex = rowIndex
                    if (tapCount >= 2 &&
                        (model.activeTab == InventoryTab.EQUIPMENT || model.activeTab == InventoryTab.CONSUMABLES) &&
                        model.activeItemList.isNotEmpty()) {
                        model.actionMenuFocusIndex = 0
                        model.showingActionMenu = true
                    }
                }
            })

            itemListTable.add(row).expandX().fillX().row()
        }
    }

    private fun addIconToRow(row: Table, atlasKey: String) {
        if (skin.has(atlasKey, Drawable::class.java)) {
            val icon = Image(skin.getDrawable(atlasKey)).apply { setScaling(Scaling.fit) }
            row.add(icon).size(ICON_SIZE).padLeft(2f).padRight(3f)
        } else {
            row.add().size(ICON_SIZE).padLeft(2f).padRight(3f)
        }
    }

    private fun updateRowHighlights() {
        itemListTable.cells.forEachIndexed { index, cell ->
            val actor = cell.actor
            if (actor is Table) {
                actor.background = if (index == model.focusedItemIndex) skin[Drawables.BACKGROUND_GREY] else null
            }
        }
    }

    // --- Info panel ---

    private fun rebuildInfoPanel() {
        infoTable.clear()
        infoTable.background = skin[Drawables.FRAME_FGD]
        val items = model.activeItemList
        val safeIndex = model.focusedItemIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        if (items.isEmpty()) {
            infoTable.add(Label("", skin, Labels.SMALL.skinKey)).expand().top().left().pad(3f)
            return
        }
        val entry = items[safeIndex]
        val infoText = buildInfoText(entry)
        infoTable.add(Label(infoText, skin, Labels.SMALL.skinKey)).expand().top().left().pad(3f)
    }

    private fun buildInfoText(entry: InventorySystem.InventoryEntry<*>): String = when (model.activeTab) {
        InventoryTab.EQUIPMENT -> {
            val item = entry.item as EquipmentItemData
            item.stats.entries.joinToString("\n") { (stat, value) ->
                "${stat.name.lowercase().replace('_', ' ')}: $value"
            }
        }
        InventoryTab.CONSUMABLES -> {
            val item = entry.item as ConsumableItemData
            "Restores ${item.statValue} ${item.statType.name}"
        }
        InventoryTab.QUEST_ITEMS -> {
            val item = entry.item as QuestItemData
            "${item.itemDescription}\nQuest ID: ${item.questId}"
        }
        InventoryTab.ENCHANTMENTS -> {
            val item = entry.item as BattleEnchantmentItemData
            "${item.itemDescription}\n${item.statType.name}: +${item.statValue}"
        }
    }

    // --- Helpers ---

    private fun Any.displayName(): String = when (this) {
        is EquipmentItemData         -> name
        is ConsumableItemData        -> itemName
        is QuestItemData             -> itemName
        is BattleEnchantmentItemData -> itemName
        else -> "Unknown"
    }

    private fun Any.uiAtlasKey(): String = when (this) {
        is EquipmentItemData         -> uiAtlasKey
        is ConsumableItemData        -> uiAtlasKey
        is QuestItemData             -> uiAtlasKey
        is BattleEnchantmentItemData -> uiAtlasKey
        else -> ""
    }

    private val InventoryTab.displayName: String
        get() = when (this) {
            InventoryTab.EQUIPMENT    -> "Equipment"
            InventoryTab.CONSUMABLES  -> "Consumables"
            InventoryTab.QUEST_ITEMS  -> "Quest Items"
            InventoryTab.ENCHANTMENTS -> "Enchants"
        }

    companion object {
        private const val ICON_SIZE   = 10f
        private const val INFO_HEIGHT = 40f
    }
}

@Scene2dDsl
fun <S> KWidget<S>.inventoryRightPanel(
    model: InventoryViewModel,
    skin: Skin = Scene2DSkin.defaultSkin,
    init: InventoryRightPanel.(S) -> Unit = {}
): InventoryRightPanel = actor(InventoryRightPanel(model, skin), init)
