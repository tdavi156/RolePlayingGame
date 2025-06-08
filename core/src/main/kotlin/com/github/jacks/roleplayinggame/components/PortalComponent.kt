package com.github.jacks.roleplayinggame.components

import com.github.quillraven.fleks.Entity

data class PortalComponent(
    var id : Int = -1,
    var toMap : String = "",
    var toPortal : Int = -1,
    var triggerEntities : MutableSet<Entity> = mutableSetOf()
)
