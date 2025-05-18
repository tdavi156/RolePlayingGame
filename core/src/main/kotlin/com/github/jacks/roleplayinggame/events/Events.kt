package com.github.jacks.roleplayinggame.events

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.AnimationModel

fun Stage.fire(event : Event) {
    this.root.fire(event)
}

data class MapChangeEvent(val map : TiledMap) : Event()

class CollisionDespawnEvent(val cell : Cell) : Event()

class EntityAttackEvent(val model : AnimationModel) : Event()

class EntityDeathEvent(val model : AnimationModel) : Event()

class EntityLootEvent(val model : AnimationModel) : Event()
