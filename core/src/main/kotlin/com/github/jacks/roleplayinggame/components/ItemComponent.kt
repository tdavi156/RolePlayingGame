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
    val uiAtlasKey : String,
    val itemName : String,
    val statType : StatType,
    val statValue : Float
) {
    UNDEFINED(ItemCategory.UNDEFINED, "", "", StatType.UNDEFINED, 0f),
    HELMET(ItemCategory.HELMET, "helmet", "Helmet", StatType.MAX_HEALTH, 10f),
    SWORD(ItemCategory.WEAPON, "sword", "Sword", StatType.ATTACK_DAMAGE, 3f),
    BIG_SWORD(ItemCategory.WEAPON, "sword2", "Big Sword", StatType.ATTACK_DAMAGE, 5f),
    BOOTS(ItemCategory.BOOTS, "boots", "Boots", StatType.MOVE_SPEED, 1f),
    ARMOR(ItemCategory.ARMOR, "armor", "Armor", StatType.DEFENSE, 1f);
}

data class ItemComponent(
    var itemType : ItemType = ItemType.UNDEFINED,
    var slotIndex : Int = -1,
    var equipped : Boolean = false,
)
