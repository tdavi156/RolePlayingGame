package com.github.jacks.roleplayinggame.components

import com.badlogic.gdx.math.MathUtils

enum class ItemCategory {
    UNDEFINED,
    HELMET,
    WEAPON,
    ARMOR,
    BOOTS;
}

enum class ItemType(
    val category : ItemCategory,
    val uiAtlasKey : String,
    val itemName : String
) {
    UNDEFINED(ItemCategory.UNDEFINED, "", ""),
    HELMET(ItemCategory.HELMET, "helmet", "Helmet"),
    SWORD(ItemCategory.WEAPON, "sword", "Sword"),
    BIG_SWORD(ItemCategory.WEAPON, "sword2", "Big Sword"),
    BOOTS(ItemCategory.BOOTS, "boots", "Boots"),
    ARMOR(ItemCategory.ARMOR, "armor", "Armor");
}

data class ItemComponent(
    var itemType : ItemType = ItemType.UNDEFINED,
    var slotIndex : Int = -1,
    var equipped : Boolean = false
)
