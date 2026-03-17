package com.github.jacks.roleplayinggame.configurations

import com.github.jacks.roleplayinggame.components.StatType

enum class ItemCategory { UNDEFINED, HELMET, WEAPON, ARMOR, BOOTS }

data class ItemData(
    val name: String,
    val category: ItemCategory,
    val uiAtlasKey: String,
    val stats: Map<StatType, Int>
)

val Items: List<ItemData> = listOf(
    // ItemData(
    //     name = "Item Name",
    //     category = ItemCategory.WEAPON,
    //     uiAtlasKey = "atlas_key",
    //     stats = mapOf(StatType.ATTACK_DAMAGE to 10, StatType.DEFENSE to 2)
    // ),
    ItemData("Helmet",   ItemCategory.HELMET,  "helmet", mapOf(StatType.MAX_HEALTH    to 10)),
    ItemData("Sword",    ItemCategory.WEAPON,  "sword",  mapOf(StatType.ATTACK_DAMAGE to 3)),
    ItemData("Big Sword",ItemCategory.WEAPON,  "sword2", mapOf(StatType.ATTACK_DAMAGE to 5)),
    ItemData("Boots",    ItemCategory.BOOTS,   "boots",  mapOf(StatType.MOVE_SPEED    to 1)),
    ItemData("Armor",    ItemCategory.ARMOR,   "armor",  mapOf(StatType.DEFENSE       to 1)),
)

fun itemByName(name: String): ItemData? = Items.find { it.name == name }
