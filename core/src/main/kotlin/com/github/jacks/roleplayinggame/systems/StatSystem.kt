package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.jacks.roleplayinggame.components.InventoryComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.configurations.ConsumableStatType
import com.github.jacks.roleplayinggame.configurations.consumableItemById
import com.github.jacks.roleplayinggame.configurations.equipmentItemById
import com.github.jacks.roleplayinggame.events.EquipItemEvent
import com.github.jacks.roleplayinggame.events.UseConsumableEvent
import com.github.quillraven.fleks.IntervalSystem

class StatSystem : IntervalSystem(), EventListener {

    private val playerFamily  by lazy { world.family(allOf = arrayOf(PlayerComponent::class)) }
    private val statMapper    by lazy { world.mapper<StatComponent>() }
    private val inventoryMapper by lazy { world.mapper<InventoryComponent>() }

    override fun onTick() = Unit

    override fun handle(event: Event): Boolean {
        when (event) {
            is EquipItemEvent -> {
                val entity  = playerFamily.firstOrNull() ?: return false
                val stat    = statMapper.getOrNull(entity) ?: return false
                val inv     = inventoryMapper.getOrNull(entity) ?: return false
                val newItem = equipmentItemById(event.itemId) ?: return false

                // Remove old equipped item's stat bonuses
                val oldId = inv.equippedItems[newItem.category]
                if (oldId != null) {
                    equipmentItemById(oldId)?.stats?.forEach { (statType, value) ->
                        stat.decreaseStat(statType.toStatType(), value.toFloat())
                    }
                }

                // Update slot and apply new bonuses
                inv.equippedItems[newItem.category] = newItem.id
                newItem.stats.forEach { (statType, value) ->
                    stat.increaseStat(statType.toStatType(), value.toFloat())
                }

                // Clamp current values to their maximums
                stat.currentHealth = stat.currentHealth.coerceAtMost(stat.maxHealth)
                stat.currentMana   = stat.currentMana.coerceAtMost(stat.maxMana)
                return true
            }

            is UseConsumableEvent -> {
                val entity = playerFamily.firstOrNull() ?: return false
                val stat   = statMapper.getOrNull(entity) ?: return false
                val item   = consumableItemById(event.itemId) ?: return false

                when (item.statType) {
                    ConsumableStatType.HEALTH ->
                        stat.currentHealth = (stat.currentHealth + item.statValue).coerceAtMost(stat.maxHealth)
                    ConsumableStatType.MANA ->
                        stat.currentMana = (stat.currentMana + item.statValue).coerceAtMost(stat.maxMana)
                }

                world.system<InventorySystem>().removeItem(event.itemId)
                return true
            }

            else -> return false
        }
    }
}
