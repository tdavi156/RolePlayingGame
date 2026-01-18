package com.github.jacks.roleplayinggame.components

import com.github.quillraven.fleks.Entity

data class BattleComponent(
    var toMap : String = "",
    var triggerEntities : MutableSet<Entity> = mutableSetOf()
)
