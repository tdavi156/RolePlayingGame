package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.InventoryComponent
import com.github.jacks.roleplayinggame.components.ItemComponent
import com.github.jacks.roleplayinggame.components.ItemType
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.events.EntityAddItemEvent
import com.github.jacks.roleplayinggame.events.EntityLootEvent
import com.github.quillraven.fleks.World

class InventoryViewModel(
    world : World,
    gameStage : Stage
) : PropertyChangeSource(), EventListener {

    private val playerComponents = world.mapper<PlayerComponent>()
    private val inventoryComponents = world.mapper<InventoryComponent>()
    private val itemComponents = world.mapper<ItemComponent>()
    private val playerEntities = world.family(allOf = arrayOf(PlayerComponent::class))
    var playerItems by propertyNotify(listOf<ItemModel>())

    private val playerInventoryComponent : InventoryComponent
        get() = inventoryComponents[playerEntities.first()]

    init {
        gameStage.addListener(this)
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is EntityAddItemEvent -> {
                if (event.entity in playerComponents) {
                    playerItems = inventoryComponents[event.entity].items.map {
                        val itemComponent = itemComponents[it]
                        ItemModel(
                            it.id,
                            itemComponent.itemType.category,
                            itemComponent.itemType.uiAtlasKey,
                            itemComponent.slotIndex,
                            itemComponent.equipped
                        )
                    }
                }
            }
            is EntityLootEvent -> {
                if (event.entity in playerComponents) {
                    inventoryComponents[event.entity].itemsToAdd += ItemType.entries.filterNot { it == ItemType.UNDEFINED }.random()
                }
            }
            else -> return false
        }
        return true
    }

    fun equip(itemModel : ItemModel, equipped : Boolean) {
        playerItemByModel(itemModel).equipped = equipped
        itemModel.isEquipped = equipped
    }

    fun inventoryItem(slotIndex : Int, itemModel : ItemModel) {
        playerItemByModel(itemModel).slotIndex = slotIndex
        itemModel.slotIndex = slotIndex
    }

    private fun playerItemByModel(itemModel : ItemModel) : ItemComponent {
        return itemComponents[playerInventoryComponent.items.first { it.id == itemModel.itemEntityId }]
    }
}

