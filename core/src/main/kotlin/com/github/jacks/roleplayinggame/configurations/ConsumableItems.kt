package com.github.jacks.roleplayinggame.configurations

data class ConsumableItemData(
    val id: Int,
    val itemName: String,
    val uiAtlasKey: String,
    val statType: ConsumableStatType,
    val statValue: Int,
    val goldValue: Int = 10,
    val isSellable: Boolean = true,
)

val ConsumableItems: List<ConsumableItemData> = listOf(
    // ConsumableItemData(
    //     id = 2000,
    //     itemName = "Item Name",
    //     uiAtlasKey = "atlas_key",
    //     statType = ConsumableStatType.HEALTH,
    //     statValue = 20
    // ),
    ConsumableItemData(2001, "Health Potion",  "armor", ConsumableStatType.HEALTH, 20),
    ConsumableItemData(2002, "Mega Potion",    "armor", ConsumableStatType.HEALTH, 50),
    ConsumableItemData(2003, "Mana Potion",    "armor", ConsumableStatType.MANA,   30),
)

fun consumableItemById(id: Int): ConsumableItemData? = ConsumableItems.find { it.id == id }
