package com.github.jacks.roleplayinggame.components

import com.github.jacks.roleplayinggame.configurations.ItemCategory

class InventoryComponent {
    val equippedItems: MutableMap<ItemCategory, Int?> = mutableMapOf(
        ItemCategory.HELMET to null,
        ItemCategory.WEAPON to null,
        ItemCategory.ARMOR  to null,
        ItemCategory.BOOTS  to null,
    )
}
