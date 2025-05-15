package com.github.jacks.roleplayinggame.components

data class MoveComponent (
    var speed : Float = 0f,
    var cos : Float = 0f,
    var sin : Float = 0f,
    var direction : String = "to",
    var isRooted : Boolean = false
)
