package com.github.jacks.roleplayinggame.components

enum class ItemCategory {
    UNDEFINED,
    HELMET,
    WEAPON,
    ARMOR,
    BOOTS;
}

enum class ItemType(
    val category : ItemCategory,
    val uiAtlasKey : String
) {
    UNDEFINED(ItemCategory.UNDEFINED, ""),
    HELMET(ItemCategory.HELMET, "helmet"),
    SWORD(ItemCategory.WEAPON, "sword"),
    BIG_SWORD(ItemCategory.WEAPON, "sword2"),
    BOOTS(ItemCategory.BOOTS, "boots"),
    ARMOR(ItemCategory.ARMOR, "armor"),
}

data class ItemComponent(
    var itemType : ItemType = ItemType.UNDEFINED,
    var slotIndex : Int = -1,
    var equipped : Boolean = false
) {
}
