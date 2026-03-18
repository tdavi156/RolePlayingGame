package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.configurations.BattleEnchantmentItemData
import com.github.jacks.roleplayinggame.configurations.ConsumableItemData
import com.github.jacks.roleplayinggame.configurations.EquipmentItemData
import com.github.jacks.roleplayinggame.configurations.ItemCategory
import com.github.jacks.roleplayinggame.configurations.QuestItemData
import com.github.jacks.roleplayinggame.configurations.ShopConfig
import com.github.jacks.roleplayinggame.configurations.battleEnchantmentItemById
import com.github.jacks.roleplayinggame.configurations.consumableItemById
import com.github.jacks.roleplayinggame.configurations.equipmentItemById
import com.github.jacks.roleplayinggame.configurations.questItemById
import com.github.jacks.roleplayinggame.events.ShopBuyConfirmedEvent
import com.github.jacks.roleplayinggame.events.ShopClosedEvent
import com.github.jacks.roleplayinggame.events.ShopOpenEvent
import com.github.jacks.roleplayinggame.events.ShopSellConfirmedEvent
import com.github.jacks.roleplayinggame.systems.InventorySystem
import com.github.jacks.roleplayinggame.systems.PartySystem
import com.github.jacks.roleplayinggame.systems.ResourceSystem
import com.github.quillraven.fleks.World
import kotlin.math.ceil
import kotlin.math.floor

// ─── Enums ──────────────────────────────────────────────────────────────────

enum class ShopMode { BUY, SELL }
enum class ShopTab { EQUIPMENT, CONSUMABLES, QUEST_ITEMS, ENCHANTMENTS }

// ─── Row data classes ────────────────────────────────────────────────────────

/** A row in the BUY item list. id == LEAVE_ID marks the "Leave" sentinel row. */
data class ShopBuyRow(
    val id: Int,
    val name: String,
    val goldValue: Int,
    val canAfford: Boolean,
    val iconKey: String,
)

/** A row in the SELL item list. id == LEAVE_ID marks the "Leave" sentinel row. */
data class ShopSellRow(
    val id: Int,
    val name: String,
    val sellPrice: Int,
    val availableQty: Int,
    val sellable: Boolean,
    val iconKey: String,
)

// ─── ViewModel ───────────────────────────────────────────────────────────────

class ShopViewModel(
    private val world: World,
    private val gameStage: Stage,
) : PropertyChangeSource(), EventListener {

    private val inventorySystem get() = world.system<InventorySystem>()
    private val resourceSystem  get() = world.system<ResourceSystem>()
    private val partySystem     get() = world.system<PartySystem>()

    /** Count of how many times itemId is equipped across ALL unlocked characters. */
    private fun equippedCountAcrossParty(itemId: Int): Int {
        return partySystem.getUnlockedCharacters().count { charData ->
            charData.equippedItems.values.any { it == itemId }
        }
    }

    private var activeShopConfig: ShopConfig? = null

    // Observable properties
    var shopMode        by propertyNotify(ShopMode.BUY)
    var activeTab       by propertyNotify(ShopTab.EQUIPMENT)
    var focusedItemIndex by propertyNotify(0)
    /** -1 = no item selected for quantity input */
    var pendingItemId   by propertyNotify(NO_PENDING)
    var pendingQuantity by propertyNotify(1)
    var insufficientGoldVisible by propertyNotify(false)
    /** Incremented after every buy/sell so the view can refresh gold + lists. */
    var refreshToken    by propertyNotify(0)

    // ─── Computed accessors ──────────────────────────────────────────────────

    val currentGold: Int get() = resourceSystem.resources.gold

    val buyItemList: List<ShopBuyRow>
        get() {
            val config = activeShopConfig ?: return listOf(LEAVE_BUY_ROW)
            val gold = resourceSystem.resources.gold
            val ids = when (activeTab) {
                ShopTab.EQUIPMENT    -> config.equipmentIds
                ShopTab.CONSUMABLES  -> config.consumableIds
                ShopTab.QUEST_ITEMS  -> config.questIds
                ShopTab.ENCHANTMENTS -> config.enchantmentIds
            }
            val rows = ids.mapNotNull { id ->
                when (id) {
                    in 1000..1999 -> equipmentItemById(id)?.let { item ->
                        ShopBuyRow(item.id, item.name, item.goldValue, item.goldValue <= gold, item.uiAtlasKey)
                    }
                    in 2000..2999 -> consumableItemById(id)?.let { item ->
                        ShopBuyRow(item.id, item.itemName, item.goldValue, item.goldValue <= gold, item.uiAtlasKey)
                    }
                    in 3000..3999 -> questItemById(id)?.let { item ->
                        // Quest items have no price — treat as free / always affordable
                        ShopBuyRow(item.id, item.itemName, 0, true, item.uiAtlasKey)
                    }
                    in 4000..4999 -> battleEnchantmentItemById(id)?.let { item ->
                        ShopBuyRow(item.id, item.itemName, item.goldValue, item.goldValue <= gold, item.uiAtlasKey)
                    }
                    else -> null
                }
            }
            return rows + LEAVE_BUY_ROW
        }

    val sellItemList: List<ShopSellRow>
        get() {
            val rows = when (activeTab) {
                ShopTab.EQUIPMENT -> inventorySystem.equipment.map { entry ->
                    val item = entry.item as EquipmentItemData
                    val equippedCount = equippedCountAcrossParty(item.id)
                    val available = (entry.quantity - equippedCount).coerceAtLeast(0)
                    ShopSellRow(
                        id = item.id,
                        name = item.name,
                        sellPrice = ceil(item.goldValue / 2f).toInt(),
                        availableQty = available,
                        sellable = item.isSellable && available > 0,
                        iconKey = item.uiAtlasKey,
                    )
                }
                ShopTab.CONSUMABLES -> inventorySystem.consumables.map { entry ->
                    val item = entry.item as ConsumableItemData
                    ShopSellRow(
                        id = item.id,
                        name = item.itemName,
                        sellPrice = ceil(item.goldValue / 2f).toInt(),
                        availableQty = entry.quantity,
                        sellable = item.isSellable && entry.quantity > 0,
                        iconKey = item.uiAtlasKey,
                    )
                }
                ShopTab.QUEST_ITEMS -> inventorySystem.questItems.map { entry ->
                    val item = entry.item as QuestItemData
                    ShopSellRow(
                        id = item.id,
                        name = item.itemName,
                        sellPrice = 0,
                        availableQty = entry.quantity,
                        sellable = false, // quest items never sellable
                        iconKey = item.uiAtlasKey,
                    )
                }
                ShopTab.ENCHANTMENTS -> inventorySystem.enchantments.map { entry ->
                    val item = entry.item as BattleEnchantmentItemData
                    ShopSellRow(
                        id = item.id,
                        name = item.itemName,
                        sellPrice = ceil(item.goldValue / 2f).toInt(),
                        availableQty = entry.quantity,
                        sellable = item.isSellable && entry.quantity > 0,
                        iconKey = item.uiAtlasKey,
                    )
                }
            }
            return rows + LEAVE_SELL_ROW
        }

    /** Maximum quantity the player can buy/sell for the currently pending item. */
    val maxQuantity: Int
        get() {
            val itemId = pendingItemId
            if (itemId == NO_PENDING) return 1
            return when (shopMode) {
                ShopMode.BUY -> {
                    val gold = resourceSystem.resources.gold
                    val price = goldValueOf(itemId)
                    if (price <= 0) MAX_STACK else floor(gold.toFloat() / price).toInt().coerceAtLeast(1)
                }
                ShopMode.SELL -> {
                    when (itemId) {
                        in 1000..1999 -> {
                            val entry = inventorySystem.equipment.find { it.item.id == itemId }
                            val qty = entry?.quantity ?: 0
                            val equipped = equippedCountAcrossParty(itemId)
                            (qty - equipped).coerceAtLeast(0)
                        }
                        in 2000..2999 -> inventorySystem.consumables.find { it.item.id == itemId }?.quantity ?: 0
                        in 4000..4999 -> inventorySystem.enchantments.find { it.item.id == itemId }?.quantity ?: 0
                        else -> 0
                    }
                }
            }
        }

    // ─── Event handling ──────────────────────────────────────────────────────

    init {
        gameStage.addListener(this)
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is ShopOpenEvent -> {
                activeShopConfig = event.shopConfig
                shopMode         = ShopMode.BUY
                activeTab        = ShopTab.EQUIPMENT
                focusedItemIndex = 0
                pendingItemId    = NO_PENDING
                pendingQuantity  = 1
                insufficientGoldVisible = false
                refreshToken++
                return true
            }
            is ShopClosedEvent -> {
                activeShopConfig = null
                pendingItemId    = NO_PENDING
                pendingQuantity  = 1
                insufficientGoldVisible = false
                refreshToken++
                return true
            }
            is ShopBuyConfirmedEvent, is ShopSellConfirmedEvent -> {
                pendingItemId   = NO_PENDING
                pendingQuantity = 1
                refreshToken++          // triggers gold display + list rebuild in the view
                return true
            }
            else -> return false
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun goldValueOf(itemId: Int): Int = when (itemId) {
        in 1000..1999 -> equipmentItemById(itemId)?.goldValue ?: 0
        in 2000..2999 -> consumableItemById(itemId)?.goldValue ?: 0
        in 3000..3999 -> 0
        in 4000..4999 -> battleEnchantmentItemById(itemId)?.goldValue ?: 0
        else -> 0
    }

    companion object {
        const val NO_PENDING = -1
        private const val MAX_STACK = 99

        val LEAVE_BUY_ROW  = ShopBuyRow(NO_PENDING,  "Leave", 0, true, "")
        val LEAVE_SELL_ROW = ShopSellRow(NO_PENDING, "Leave", 0, 0, true, "")
    }
}
