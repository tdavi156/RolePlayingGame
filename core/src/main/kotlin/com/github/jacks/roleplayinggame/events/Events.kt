package com.github.jacks.roleplayinggame.events

import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.AnimationModel
import com.github.jacks.roleplayinggame.components.ItemType
import com.github.jacks.roleplayinggame.dialog.Dialog
import com.github.quillraven.fleks.Entity

fun Stage.fire(event : Event) {
    this.root.fire(event)
}

class InitializeGameEvent : Event()
class GamePauseEvent : Event()
class GameResumeEvent : Event()

data class MapChangeEvent(val map : TiledMap) : Event()
data class BattleMapChangeEvent(val map : TiledMap) : Event()
data class PortalEvent(val toMap : String, val toPortal : Int) : Event()
data class BattleEvent(val toMap : String) : Event()

class CollisionDespawnEvent(val cell : Cell) : Event()
class EntityAttackEvent(val model : AnimationModel) : Event()
class EntityDeathEvent(val model : AnimationModel) : Event()
class EntityRespawnEvent(val entity : Entity) : Event()
class EntityLootEvent(val entity : Entity, val model : AnimationModel) : Event()
class EntityTakeDamageEvent(val entity : Entity) : Event()
class EntityAddItemEvent(val entity : Entity, val itemType : ItemType) : Event()
class EntityDialogEvent(val dialog : Dialog) : Event()
