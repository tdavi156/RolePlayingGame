package com.github.jacks.roleplayinggame.components

data class LifeComponent(
    var health : Float = 0f,
    var maxHealth : Float = 0f,
    var healthRegeneration : Float = 0f,
    var takeDamage : Float = 0f,
    var attack : Float = 0f,
    var defense : Float = 0f,
)
