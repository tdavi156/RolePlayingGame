package com.github.jacks.roleplayinggame.configurations

data class QuestItemData(
    val id: Int,
    val itemName: String,
    val uiAtlasKey: String,
    val itemDescription: String,
    val questId: Int = 0,
    val isSellable: Boolean = false,
)

val QuestItems: List<QuestItemData> = listOf(
    // QuestItemData(
    //     id = 3000,
    //     itemName = "Item Name",
    //     uiAtlasKey = "atlas_key",
    //     itemDescription = "A mysterious item.",
    //     questId = 0
    // ),
    QuestItemData(3001, "Ancient Scroll",  "boots", "An old scroll with faded text.", questId = 0),
    QuestItemData(3002, "Broken Amulet",   "boots", "A cracked amulet humming faintly.", questId = 0),
    QuestItemData(3003, "Strange Crystal", "boots", "A crystal that glows in the dark.", questId = 0),
)

fun questItemById(id: Int): QuestItemData? = QuestItems.find { it.id == id }
