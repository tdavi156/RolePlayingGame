package com.github.jacks.roleplayinggame.components

import com.badlogic.gdx.math.Vector2
import ktx.math.vec2

data class SpawnerComponent(
    var spawnerId : Int = -1,
    var mapId : Int = -1,
    var entityToSpawn : String = "",
    var location : Vector2 = vec2(),
    var spawnTimer : Float = 60f,
    var currentTime : Float = 0f,
    var isSpawned : Boolean = false,
)
