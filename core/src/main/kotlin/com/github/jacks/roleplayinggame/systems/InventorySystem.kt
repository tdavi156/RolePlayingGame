package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.jacks.roleplayinggame.configurations.BattleEnchantmentItemData
import com.github.jacks.roleplayinggame.configurations.ConsumableItemData
import com.github.jacks.roleplayinggame.configurations.EquipmentItemData
import com.github.jacks.roleplayinggame.components.InventoryComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.configurations.ItemCategory
import com.github.jacks.roleplayinggame.configurations.QuestItemData
import com.github.jacks.roleplayinggame.configurations.battleEnchantmentItemById
import com.github.jacks.roleplayinggame.configurations.consumableItemById
import com.github.jacks.roleplayinggame.configurations.equipmentItemById
import com.github.jacks.roleplayinggame.configurations.questItemById
import com.github.jacks.roleplayinggame.saveManager.InventorySaveData
import com.github.quillraven.fleks.IntervalSystem

class InventorySystem : IntervalSystem(), EventListener {

    data class InventoryEntry<T>(val item: T, var quantity: Int)

    val equipment    = mutableListOf<InventoryEntry<EquipmentItemData>>()
    val consumables  = mutableListOf<InventoryEntry<ConsumableItemData>>()
    val questItems   = mutableListOf<InventoryEntry<QuestItemData>>()
    val enchantments = mutableListOf<InventoryEntry<BattleEnchantmentItemData>>()

    override fun onTick() = Unit

    override fun handle(event: Event): Boolean = false

    fun addItem(item: EquipmentItemData) {
        val entry = equipment.find { it.item.id == item.id }
        if (entry != null) entry.quantity++ else equipment += InventoryEntry(item, 1)
    }

    fun addItem(item: ConsumableItemData) {
        val entry = consumables.find { it.item.id == item.id }
        if (entry != null) entry.quantity++ else consumables += InventoryEntry(item, 1)
    }

    fun addItem(item: QuestItemData) {
        val entry = questItems.find { it.item.id == item.id }
        if (entry != null) entry.quantity++ else questItems += InventoryEntry(item, 1)
    }

    fun addItem(item: BattleEnchantmentItemData) {
        val entry = enchantments.find { it.item.id == item.id }
        if (entry != null) entry.quantity++ else enchantments += InventoryEntry(item, 1)
    }

    /**
     * Clears all inventory lists and repopulates them from [data].
     * Called by [InitializeGameSystem] when loading an existing save.
     * Items whose IDs are not found in the config are silently skipped (forward-compat).
     */
    fun restoreInventory(data: InventorySaveData) {
        equipment.clear()
        consumables.clear()
        questItems.clear()
        enchantments.clear()
        data.equipment.forEach    { e -> equipmentItemById(e.id)?.let          { equipment    += InventoryEntry(it, e.quantity) } }
        data.consumables.forEach  { e -> consumableItemById(e.id)?.let         { consumables  += InventoryEntry(it, e.quantity) } }
        data.questItems.forEach   { e -> questItemById(e.id)?.let              { questItems   += InventoryEntry(it, e.quantity) } }
        data.enchantments.forEach { e -> battleEnchantmentItemById(e.id)?.let  { enchantments += InventoryEntry(it, e.quantity) } }
    }

    fun removeItem(id: Int) {
        when (id) {
            in 1000..1999 -> {
                val entry = equipment.find { it.item.id == id } ?: return
                if (--entry.quantity <= 0) equipment.remove(entry)
            }
            in 2000..2999 -> {
                val entry = consumables.find { it.item.id == id } ?: return
                if (--entry.quantity <= 0) consumables.remove(entry)
            }
            in 3000..3999 -> {
                val entry = questItems.find { it.item.id == id } ?: return
                if (--entry.quantity <= 0) questItems.remove(entry)
            }
            in 4000..4999 -> {
                val entry = enchantments.find { it.item.id == id } ?: return
                if (--entry.quantity <= 0) enchantments.remove(entry)
            }
        }
    }

    private val playerFamily by lazy { world.family(allOf = arrayOf(PlayerComponent::class)) }
    private val inventoryMapper by lazy { world.mapper<InventoryComponent>() }

    fun getEquippedIds(): Map<ItemCategory, Int?> {
        val playerEntity = playerFamily.firstOrNull() ?: return emptyMap()
        return inventoryMapper[playerEntity].equippedItems
    }
}
