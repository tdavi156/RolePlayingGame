package com.github.jacks.roleplayinggame.components

enum class SpawnerType {
    INVALID_SPAWNER, PLAYER_SPAWNER, SLIME_SPAWNER
}

data class SpawnerComponent(
    var spawnerId : Int,
    var mapId : Int,
    var spawnerType : SpawnerType,
    var spawnTimer : Float,
    var currentTime : Float,
    var isSpawned : Boolean
) {
}
