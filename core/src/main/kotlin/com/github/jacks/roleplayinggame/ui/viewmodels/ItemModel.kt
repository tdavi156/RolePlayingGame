package com.github.jacks.roleplayinggame.ui.viewmodels

import com.github.jacks.roleplayinggame.configurations.ItemCategory

data class ItemModel(
    val name : String,
    val itemCategory : ItemCategory,
    val atlasKey : String,
    var slotIndex : Int,
    var isEquipped : Boolean
)
