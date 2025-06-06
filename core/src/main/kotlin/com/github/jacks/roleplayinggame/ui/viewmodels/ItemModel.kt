package com.github.jacks.roleplayinggame.ui.viewmodels

import com.github.jacks.roleplayinggame.components.ItemCategory

data class ItemModel(
    val itemEntityId : Int,
    val itemCategory : ItemCategory,
    val atlasKey : String,
    var slotIndex : Int,
    var isEquipped : Boolean
)
