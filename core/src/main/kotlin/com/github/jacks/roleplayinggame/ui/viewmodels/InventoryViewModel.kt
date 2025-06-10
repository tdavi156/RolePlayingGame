package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.InventoryComponent
import com.github.jacks.roleplayinggame.components.ItemComponent
import com.github.jacks.roleplayinggame.components.ItemType
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.events.EntityAddItemEvent
import com.github.jacks.roleplayinggame.events.EntityLootEvent
import com.github.quillraven.fleks.World

class InventoryViewModel(
    world : World,
    gameStage : Stage
) : PropertyChangeSource(), EventListener {

    private val playerComponents = world.mapper<PlayerComponent>()
    private val inventoryComponents = world.mapper<InventoryComponent>()
    private val statComponents = world.mapper<StatComponent>()
    private val itemComponents = world.mapper<ItemComponent>()
    private val playerEntities = world.family(allOf = arrayOf(PlayerComponent::class))
    var playerItems by propertyNotify(listOf<ItemModel>())

    private val playerInventoryComponent : InventoryComponent
        get() = inventoryComponents[playerEntities.first()]

    private val playerStatComponent : StatComponent
        get() = statComponents[playerEntities.first()]

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

    fun equip(itemModel : ItemModel) {
        val item = playerItemByModel(itemModel)
        val itemType = item.itemType
        item.equipped = true
        itemModel.isEquipped = true
        playerStatComponent.increaseStat(itemType.statType, itemType.statValue)
    }

    fun unequip(itemModel : ItemModel) {
        val item = playerItemByModel(itemModel)
        val itemType = item.itemType
        item.equipped = false
        itemModel.isEquipped = false
        playerStatComponent.decreaseStat(itemType.statType, itemType.statValue)
    }

    fun inventoryItem(slotIndex : Int, itemModel : ItemModel) {
        playerItemByModel(itemModel).slotIndex = slotIndex
        itemModel.slotIndex = slotIndex
    }

    private fun playerItemByModel(itemModel : ItemModel) : ItemComponent {
        return itemComponents[playerInventoryComponent.items.first { it.id == itemModel.itemEntityId }]
    }
}

