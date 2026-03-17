package com.github.jacks.roleplayinggame.events

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.AnimationModel
import com.github.jacks.roleplayinggame.components.BattleAction
import com.github.jacks.roleplayinggame.components.BattleEndReason
import com.github.jacks.roleplayinggame.components.BattlePhase
import com.github.jacks.roleplayinggame.configurations.EquipmentItemData
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
data class BattleEvent(val enemy : Entity) : Event()
data class BattleEndEvent(val reason: BattleEndReason) : Event()
data class BattleTransitionStartEvent(val enemy: Entity) : Event()
data class BattleEndTransitionStartEvent(val reason: BattleEndReason) : Event()

// Battle state-machine events
data class BattlePhaseChangedEvent(val phase: BattlePhase) : Event()
data class BattleActionSelectedEvent(val action: BattleAction) : Event()
data class BattleHealthUpdateEvent(val playerHealthPct: Float, val enemyHealthPct: Float) : Event()
data class BattleLogEvent(val message: String) : Event()
class BattleLogDismissedEvent : Event()
data class BattleTargetSelectedEvent(val target: Entity) : Event()

data class BattleRewardData(val expGained: Int, val goldGained: Int, val itemDropped: EquipmentItemData?)
class BattleRewardEvent(val rewardData: BattleRewardData) : Event()
class RewardDismissedEvent : Event()

class CollisionDespawnEvent(val cell : Cell) : Event()
class EntityAttackEvent(val model : AnimationModel) : Event()
class EntityDeathEvent(val model : AnimationModel) : Event()
class EntityRespawnEvent(val entity : Entity) : Event()
class EntityLootEvent(val entity : Entity, val model : AnimationModel) : Event()
class EntityTakeDamageEvent(val entity : Entity) : Event()
class EntityAddItemEvent(val entity : Entity, val itemName : String) : Event()
class EntityDialogEvent(val dialog : Dialog) : Event()
class InteractionEvent : Event()

class SettingsOpenEvent : Event()
class SettingsClosedEvent : Event()

class InventoryOpenEvent : Event()
class InventoryClosedEvent : Event()

data class EquipItemEvent(val itemId: Int, val characterIndex: Int) : Event()
data class UseConsumableEvent(val itemId: Int, val characterIndex: Int, val isCombatItemUse: Boolean = false) : Event()

// Combat inventory events
class CombatInventoryOpenEvent : Event()
class CombatInventoryClosedEvent : Event()
class CombatItemUseDismissedEvent : Event()
data class ItemUseFlashEvent(val characterIndex: Int, val flashColor: Color) : Event()

// Shop events
class ShopInteractionEvent(val shopId: Int) : Event()
class ShopOpenEvent(val shopConfig: com.github.jacks.roleplayinggame.configurations.ShopConfig) : Event()
class ShopClosedEvent : Event()
class ShopBuyConfirmedEvent(val itemId: Int, val quantity: Int) : Event()
class ShopSellConfirmedEvent(val itemId: Int, val quantity: Int) : Event()
