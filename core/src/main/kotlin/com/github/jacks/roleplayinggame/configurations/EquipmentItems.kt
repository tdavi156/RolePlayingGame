package com.github.jacks.roleplayinggame.configurations

// ID Ranges:
// Equipment:           1000–1999
// Consumables:         2000–2999
// Quest Items:         3000–3999
// Battle Enchantments: 4000–4999

enum class ItemCategory { UNDEFINED, HELMET, WEAPON, ARMOR, BOOTS }

data class EquipmentItemData(
    val id: Int,
    val name: String,
    val category: ItemCategory,
    val uiAtlasKey: String,
    val stats: Map<EquipmentStatType, Int>,
    val isSellable: Boolean = true,
    val goldValue: Int = 10,
)

val EquipmentItems: List<EquipmentItemData> = listOf(
    // EquipmentItemData(
    //     id = 1000,
    //     name = "Item Name",
    //     category = ItemCategory.WEAPON,
    //     uiAtlasKey = "atlas_key",
    //     stats = mapOf(EquipmentStatType.ATTACK_DAMAGE to 10, EquipmentStatType.DEFENSE to 2)
    // ),
    EquipmentItemData(1001, "Helmet",   ItemCategory.HELMET, "helmet", mapOf(EquipmentStatType.MAX_HEALTH    to 10)),
    EquipmentItemData(1002, "Sword",    ItemCategory.WEAPON, "sword",  mapOf(EquipmentStatType.ATTACK_DAMAGE to 3)),
    EquipmentItemData(1003, "Big Sword",ItemCategory.WEAPON, "sword2", mapOf(EquipmentStatType.ATTACK_DAMAGE to 5)),
    EquipmentItemData(1004, "Boots",    ItemCategory.BOOTS,  "boots",  mapOf(EquipmentStatType.MOVE_SPEED    to 1)),
    EquipmentItemData(1005, "Armor",    ItemCategory.ARMOR,  "armor",  mapOf(EquipmentStatType.DEFENSE       to 1)),
)

fun equipmentItemById(id: Int): EquipmentItemData? = EquipmentItems.find { it.id == id }
fun equipmentItemByName(name: String): EquipmentItemData? = EquipmentItems.find { it.name == name }
