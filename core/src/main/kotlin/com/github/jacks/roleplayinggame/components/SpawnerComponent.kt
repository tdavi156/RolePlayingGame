package com.github.jacks.roleplayinggame.components

import com.badlogic.gdx.math.Vector2

enum class SpawnerType {
    INVALID_SPAWNER, PLAYER_SPAWNER, SLIME_SPAWNER
}

data class SpawnerComponent(
    var spawnerId : Int,
    var mapId : Int,
    var location : Vector2,
    var spawnerType : SpawnerType,
    var spawnTimer : Float,
    var currentTime : Float,
    var isSpawned : Boolean
) {
}
