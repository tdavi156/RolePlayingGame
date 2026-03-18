package com.github.jacks.roleplayinggame.events

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.math.Vector2
import com.github.jacks.roleplayinggame.ui.Fonts
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
// targetIndex = which enemy slot (0-based) the action is directed at; ignored for FLEE
data class BattleActionSelectedEvent(val action: BattleAction, val targetIndex: Int = 0) : Event()
// enemyHealthPcts: one entry per battle enemy entity, in spawn order
data class BattleHealthUpdateEvent(val playerHealthPct: Float, val enemyHealthPcts: List<Float>) : Event()
data class BattleLogEvent(val message: String) : Event()
class BattleLogDismissedEvent : Event()
data class BattleTargetSelectedEvent(val target: Entity) : Event()
// Fired after all battle entities are created on the battle map; enemies in spawn order
data class BattleReadyEvent(val enemies: List<Entity>) : Event()

// items: all non-null drops from the battle (may be empty); expGained and goldGained are totals
data class BattleRewardData(val expGained: Int, val goldGained: Int, val items: List<EquipmentItemData>)
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

class FloatingTextEvent(val position: Vector2, val text: String, val fontType: Fonts) : Event()

// Shop events
class ShopInteractionEvent(val shopId: Int) : Event()
class ShopOpenEvent(val shopConfig: com.github.jacks.roleplayinggame.configurations.ShopConfig) : Event()
class ShopClosedEvent : Event()
class ShopBuyConfirmedEvent(val itemId: Int, val quantity: Int) : Event()
class ShopSellConfirmedEvent(val itemId: Int, val quantity: Int) : Event()

// Level / skill / ability point events
class LevelUpEvent(val entity: Entity, val newLevel: Int) : Event()
class GainSkillPointEvent(val entity: Entity) : Event()
class GainAbilityPointEvent(val entity: Entity) : Event()
class SkillPointsSaveEvent(val entity: Entity, val pendingAttackPoints: Int, val pendingDefensePoints: Int) : Event()
class SkillPointsChangedEvent(val entity: Entity) : Event()
class SkillViewOpenEvent : Event()
class SkillViewClosedEvent : Event()
class AbilityViewOpenEvent : Event()
class AbilityViewClosedEvent : Event()
class AbilitySkillChangedEvent(val entity: Entity) : Event()
class AbilityPointsSaveEvent(val characterId: Int, val pendingIds: Set<Int>) : Event()

// Spell events — targetIndex: which enemy slot to target (0-based)
class CastSpellEvent(val abilityId: Int, val casterEntity: Entity, val targetIndex: Int = 0) : Event()
class SpellCastDismissedEvent : Event()

// Quest events
class AcceptQuestEvent(val questId: Int) : Event()
class CompleteQuestEvent(val questId: Int) : Event()
class DialogGiveItemEvent(val itemId: Int) : Event()
class DialogHealPlayerEvent : Event()
class QuestStateChangedEvent(val questId: Int) : Event()
class QuestViewOpenEvent : Event()
class QuestViewClosedEvent : Event()

// Enemy killed event (fired by BattleSystem on each individual enemy death during combat)
class EnemyKilledEvent(val enemyType: com.github.jacks.roleplayinggame.configurations.EnemyType) : Event()

// Speed-based turn order event (Part 6)
class CombatSpeedChangedEvent(val entity: Entity) : Event()

// Fired when a player entity's turn starts in battle (Part 10: per-turn Spells evaluation)
class PlayerTurnStartedEvent(val playerEntity: Entity) : Event()

// Enemy selection events (Part 7)
class EnemySelectionModeStartedEvent : Event()    // BattleViewModel → BattleSystem: enter selection
class EnemySelectionModeEndedEvent : Event()       // BattleViewModel → BattleSystem: exit selection
class EnemySelectNextEvent : Event()               // Input processor → BattleSystem: cycle forward
class EnemySelectPrevEvent : Event()               // Input processor → BattleSystem: cycle backward
class EnemySelectionConfirmedEvent : Event()       // Input processor → BattleViewModel: confirm target
class EnemySelectionCancelledEvent : Event()       // Input processor → BattleViewModel: cancel selection
data class EnemySelectionIndexChangedEvent(val newIndex: Int) : Event()  // BattleSystem → BattleViewModel

// Party / character events
data class SwitchActiveCharacterEvent(val newCharacterId: Int) : Event()
class PartyUpdatedEvent : Event()
data class AddCharacterToPartyEvent(val characterId: Int, val npcEntity: Entity) : Event()
class CharacterInfoViewOpenEvent : Event()
class CharacterInfoViewClosedEvent : Event()
