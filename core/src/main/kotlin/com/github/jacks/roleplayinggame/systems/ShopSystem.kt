package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.configurations.ShopConfig
import com.github.jacks.roleplayinggame.configurations.battleEnchantmentItemById
import com.github.jacks.roleplayinggame.configurations.consumableItemById
import com.github.jacks.roleplayinggame.configurations.equipmentItemById
import com.github.jacks.roleplayinggame.configurations.questItemById
import com.github.jacks.roleplayinggame.configurations.shopConfigById
import com.github.jacks.roleplayinggame.events.ShopBuyConfirmedEvent
import com.github.jacks.roleplayinggame.events.ShopClosedEvent
import com.github.jacks.roleplayinggame.events.ShopInteractionEvent
import com.github.jacks.roleplayinggame.events.ShopOpenEvent
import com.github.jacks.roleplayinggame.events.ShopSellConfirmedEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.quillraven.fleks.IntervalSystem
import kotlin.math.ceil

class ShopSystem(
    private val gameStage: Stage,
) : IntervalSystem(), EventListener {

    private var activeShopConfig: ShopConfig? = null

    private val inventorySystem by lazy { world.system<InventorySystem>() }
    private val resourceSystem  by lazy { world.system<ResourceSystem>() }

    override fun onTick() = Unit

    override fun handle(event: Event): Boolean {
        when (event) {
            is ShopInteractionEvent -> {
                val config = shopConfigById(event.shopId) ?: return false
                activeShopConfig = config
                gameStage.fire(ShopOpenEvent(config))
                return true
            }
            is ShopBuyConfirmedEvent -> {
                handleBuy(event.itemId, event.quantity)
                return true
            }
            is ShopSellConfirmedEvent -> {
                handleSell(event.itemId, event.quantity)
                return true
            }
            is ShopClosedEvent -> {
                activeShopConfig = null
                return true
            }
            else -> return false
        }
    }

    private fun handleBuy(itemId: Int, quantity: Int) {
        val resources = resourceSystem.resources
        when (itemId) {
            in 1000..1999 -> {
                val item = equipmentItemById(itemId) ?: return
                val totalCost = item.goldValue * quantity
                if (resources.gold < totalCost) return
                resources.gold -= totalCost
                repeat(quantity) { inventorySystem.addItem(item) }
                resourceSystem.saveResources()
            }
            in 2000..2999 -> {
                val item = consumableItemById(itemId) ?: return
                val totalCost = item.goldValue * quantity
                if (resources.gold < totalCost) return
                resources.gold -= totalCost
                repeat(quantity) { inventorySystem.addItem(item) }
                resourceSystem.saveResources()
            }
            in 3000..3999 -> {
                val item = questItemById(itemId) ?: return
                // Quest items have no goldValue — treat as free
                repeat(quantity) { inventorySystem.addItem(item) }
                resourceSystem.saveResources()
            }
            in 4000..4999 -> {
                val item = battleEnchantmentItemById(itemId) ?: return
                val totalCost = item.goldValue * quantity
                if (resources.gold < totalCost) return
                resources.gold -= totalCost
                repeat(quantity) { inventorySystem.addItem(item) }
                resourceSystem.saveResources()
            }
        }
    }

    private fun handleSell(itemId: Int, quantity: Int) {
        val resources = resourceSystem.resources
        when (itemId) {
            in 1000..1999 -> {
                val item = equipmentItemById(itemId) ?: return
                val sellPrice = ceil(item.goldValue / 2f).toInt() * quantity
                resources.gold += sellPrice
                repeat(quantity) { inventorySystem.removeItem(itemId) }
                resourceSystem.saveResources()
            }
            in 2000..2999 -> {
                val item = consumableItemById(itemId) ?: return
                val sellPrice = ceil(item.goldValue / 2f).toInt() * quantity
                resources.gold += sellPrice
                repeat(quantity) { inventorySystem.removeItem(itemId) }
                resourceSystem.saveResources()
            }
            in 4000..4999 -> {
                val item = battleEnchantmentItemById(itemId) ?: return
                val sellPrice = ceil(item.goldValue / 2f).toInt() * quantity
                resources.gold += sellPrice
                repeat(quantity) { inventorySystem.removeItem(itemId) }
                resourceSystem.saveResources()
            }
            // Quest items (3000–3999) are not sellable — no-op
        }
    }
}
