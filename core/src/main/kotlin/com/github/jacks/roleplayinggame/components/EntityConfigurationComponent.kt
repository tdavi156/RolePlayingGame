package com.github.jacks.roleplayinggame.components

enum class ConfigurationType {
    UNDEFINED, PLAYER, SLIME, TREASURE_BOX
}

data class EntityConfigurationComponent(
    var configurationName : String = "",
    var configurationType : ConfigurationType = ConfigurationType.UNDEFINED
)
