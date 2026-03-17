package com.github.jacks.roleplayinggame.components

import com.github.jacks.roleplayinggame.configurations.ItemCategory

data class ItemComponent(
    var name : String = "",
    var slotIndex : Int = -1,
    var equipped : Boolean = false,
)
