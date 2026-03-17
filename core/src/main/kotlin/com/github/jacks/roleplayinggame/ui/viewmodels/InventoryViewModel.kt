package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.InventoryComponent
import com.github.jacks.roleplayinggame.components.ItemComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.configurations.ItemCategory
import com.github.jacks.roleplayinggame.configurations.Items
import com.github.jacks.roleplayinggame.configurations.itemByName
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
                        val itemData = itemByName(itemComponent.name)
                        ItemModel(
                            itemComponent.name,
                            itemData?.category ?: ItemCategory.UNDEFINED,
                            itemData?.uiAtlasKey ?: "",
                            itemComponent.slotIndex,
                            itemComponent.equipped
                        )
                    }
                }
            }
            is EntityLootEvent -> {
                if (event.entity in playerComponents) {
                    inventoryComponents[event.entity].itemsToAdd += Items.random().name
                }
            }
            else -> return false
        }
        return true
    }

    fun equip(itemModel : ItemModel) {
        val item = playerItemByModel(itemModel)
        val itemData = itemByName(item.name) ?: return
        item.equipped = true
        itemModel.isEquipped = true
        itemData.stats.forEach { (statType, value) ->
            playerStatComponent.increaseStat(statType, value.toFloat())
        }
    }

    fun unequip(itemModel : ItemModel) {
        val item = playerItemByModel(itemModel)
        val itemData = itemByName(item.name) ?: return
        item.equipped = false
        itemModel.isEquipped = false
        itemData.stats.forEach { (statType, value) ->
            playerStatComponent.decreaseStat(statType, value.toFloat())
        }
    }

    fun inventoryItem(slotIndex : Int, itemModel : ItemModel) {
        playerItemByModel(itemModel).slotIndex = slotIndex
        itemModel.slotIndex = slotIndex
    }

    private fun playerItemByModel(itemModel : ItemModel) : ItemComponent {
        return itemComponents[playerInventoryComponent.items.first { itemComponents[it].name == itemModel.name }]
    }
}
