package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.InventoryComponent
import com.github.jacks.roleplayinggame.components.ItemComponent
import com.github.jacks.roleplayinggame.components.ItemType
import com.github.jacks.roleplayinggame.events.EntityAddItemEvent
import com.github.jacks.roleplayinggame.events.MapChangeEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem

@AllOf([InventoryComponent::class])
class InventorySystem(
    private val inventoryComponents : ComponentMapper<InventoryComponent>,
    private val itemComponents : ComponentMapper<ItemComponent>,
    private val gameStage : Stage
) : IteratingSystem() {


    override fun onTickEntity(entity: Entity) {
        val inventory = inventoryComponents[entity]
        if (inventory.itemsToAdd.isEmpty()) {
            return
        }

        inventory.itemsToAdd.forEach { itemType ->
            val slotIndex : Int = getNextEmptySlotIndex(inventory)
            if (slotIndex == -1) {
                return
            }

            val newItem = createItem(itemType, slotIndex)
            inventory.items += newItem
            gameStage.fire(EntityAddItemEvent(entity, itemType))
        }
        inventory.itemsToAdd.clear()
    }

    private fun getNextEmptySlotIndex(inventory : InventoryComponent) : Int {
        for(index in 0 until InventoryComponent.INVENTORY_CAPACITY) {
            if (inventory.items.none { itemComponents[it].slotIndex == index }) {
                return index
            }
        }

        //no empty slots, return -1 index
        return -1
    }

    private fun createItem(itemType : ItemType, slotIndex : Int) : Entity {
        return world.entity {
            add<ItemComponent> {
                this.itemType = itemType
                this.slotIndex = slotIndex
            }
        }
    }
}
