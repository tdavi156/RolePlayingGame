package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.BattleAction
import com.github.jacks.roleplayinggame.components.BattleComponent
import com.github.jacks.roleplayinggame.components.BattlePhase
import com.github.jacks.roleplayinggame.components.PhysicsComponent.Companion.physicsComponentFromShape2D
import com.github.jacks.roleplayinggame.components.PortalComponent
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.events.BattleActionSelectedEvent
import com.github.jacks.roleplayinggame.events.BattleEndEvent
import com.github.jacks.roleplayinggame.events.BattleEvent
import com.github.jacks.roleplayinggame.events.BattleHealthUpdateEvent
import com.github.jacks.roleplayinggame.events.BattlePhaseChangedEvent
import com.github.jacks.roleplayinggame.events.MapChangeEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import ktx.app.gdxError
import ktx.tiled.id
import ktx.tiled.layer
import ktx.tiled.property
import ktx.tiled.shape

@AllOf([BattleComponent::class])
class BattleSystem(
    private val physicsWorld: World,
    private val gameStage: Stage,
    private val battleComponents: ComponentMapper<BattleComponent>,
    private val statComponents: ComponentMapper<StatComponent>,
) : IteratingSystem(), EventListener {

    private var currentBattleEntity: Entity? = null
    private var currentPlayerEntity: Entity? = null

    // -------------------------------------------------------------------------
    // Main tick — drives the state machine each ECS frame
    // -------------------------------------------------------------------------

    override fun onTickEntity(entity: Entity) {
        val battleComponent = battleComponents[entity]

        // Start a new battle when the player walks into this enemy
        if (battleComponent.triggerEntities.isNotEmpty() && !battleComponent.battleInProgress) {
            battleComponent.battleInProgress    = true
            currentBattleEntity                 = entity
            currentPlayerEntity                 = battleComponent.triggerEntities.first()  // stored by PhysicsSystem
            battleComponent.triggerEntities.clear()
            battleComponent.phase               = BattlePhase.PLAYER_TURN
            battleComponent.pendingPlayerAction = BattleAction.NONE
            battleComponent.resolvingPlayer     = true
            gameStage.fire(BattleEvent(enemy = entity))
            fireHealthUpdate(currentPlayerEntity!!, entity)   // initialise health bars
            return
        }

        if (!battleComponent.battleInProgress) return

        // State machine
        when (battleComponent.phase) {
            BattlePhase.PLAYER_TURN -> { /* idle - waiting for BattleActionSelectedEvent */ }
            BattlePhase.RESOLVING   -> resolveAction(entity, battleComponent)
            BattlePhase.ENEMY_TURN  -> executeEnemyTurn(entity, battleComponent)
            BattlePhase.BATTLE_END  -> endBattle(entity, battleComponent)
        }
    }
    // -------------------------------------------------------------------------
    // RESOLVING - apply the pending action’s damage, then advance phase
    // -------------------------------------------------------------------------

    private fun resolveAction(enemyEntity: Entity, battleComponent: BattleComponent) {
        val playerEntity = currentPlayerEntity ?: return

        if (battleComponent.resolvingPlayer) {
            // Player chosen action
            when (battleComponent.pendingPlayerAction) {
                BattleAction.ATTACK -> applyDamage(attacker = playerEntity, target = enemyEntity)
                BattleAction.FLEE   -> {
                    transitionPhase(battleComponent, BattlePhase.BATTLE_END)
                    return
                }
                BattleAction.NONE   -> { /* no-op */ }
            }
            fireHealthUpdate(playerEntity, enemyEntity)
            val enemyStat = statComponents.getOrNull(enemyEntity)
            if (enemyStat != null && enemyStat.isDead) {
                transitionPhase(battleComponent, BattlePhase.BATTLE_END)
            } else {
                transitionPhase(battleComponent, BattlePhase.ENEMY_TURN)
            }

        } else {
            // Enemy action (always basic attack)
            applyDamage(attacker = enemyEntity, target = playerEntity)
            fireHealthUpdate(playerEntity, enemyEntity)
            val playerStat = statComponents.getOrNull(playerEntity)
            if (playerStat != null && playerStat.isDead) {
                transitionPhase(battleComponent, BattlePhase.BATTLE_END)
            } else {
                transitionPhase(battleComponent, BattlePhase.PLAYER_TURN)
            }
        }
    }

    // -------------------------------------------------------------------------
    // ENEMY_TURN - simple AI: always attack, then hand off to RESOLVING
    // -------------------------------------------------------------------------

    private fun executeEnemyTurn(enemyEntity: Entity, battleComponent: BattleComponent) {
        battleComponent.resolvingPlayer = false          // next RESOLVING pass = enemy branch
        transitionPhase(battleComponent, BattlePhase.RESOLVING)
    }

    // -------------------------------------------------------------------------
    // BATTLE_END - fire event so GameScreen fades back to overworld
    // -------------------------------------------------------------------------

    private fun endBattle(entity: Entity, battleComponent: BattleComponent) {
        battleComponent.battleInProgress = false
        battleComponent.phase            = BattlePhase.PLAYER_TURN   // reset for next battle
        currentBattleEntity              = null
        currentPlayerEntity              = null
        gameStage.fire(BattleEndEvent())
    }
    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Set the new phase and notify all listeners (ViewModel, View). */
    private fun transitionPhase(battleComponent: BattleComponent, newPhase: BattlePhase) {
        battleComponent.phase = newPhase
        gameStage.fire(BattlePhaseChangedEvent(newPhase))
    }

    /** Apply damage formula: max(attackDamage - defense, 1). */
    private fun applyDamage(attacker: Entity, target: Entity) {
        val attackerStat = statComponents.getOrNull(attacker) ?: return
        val targetStat   = statComponents.getOrNull(target)   ?: return
        val damage = (attackerStat.attackDamage - targetStat.defense).coerceAtLeast(1f)
        targetStat.currentHealth = (targetStat.currentHealth - damage).coerceAtLeast(0f)
    }

    /** Broadcast current health percentages so the ViewModel can update the UI. */
    private fun fireHealthUpdate(playerEntity: Entity, enemyEntity: Entity) {
        val pStat = statComponents.getOrNull(playerEntity)
        val eStat = statComponents.getOrNull(enemyEntity)
        val playerPct = if (pStat != null && pStat.maxHealth > 0f)
            (pStat.currentHealth / pStat.maxHealth).coerceIn(0f, 1f) else 1f
        val enemyPct  = if (eStat != null && eStat.maxHealth > 0f)
            (eStat.currentHealth / eStat.maxHealth).coerceIn(0f, 1f) else 1f
        gameStage.fire(BattleHealthUpdateEvent(playerPct, enemyPct))
    }

    // -------------------------------------------------------------------------
    // Event handling
    // -------------------------------------------------------------------------

    override fun handle(event: Event): Boolean {
        when (event) {
            // Player tapped an action button
            is BattleActionSelectedEvent -> {
                val battleEntity    = currentBattleEntity ?: return false
                val battleComponent = battleComponents.getOrNull(battleEntity) ?: return false
                if (battleComponent.phase != BattlePhase.PLAYER_TURN) return false
                battleComponent.pendingPlayerAction = event.action
                battleComponent.resolvingPlayer     = true
                transitionPhase(battleComponent, BattlePhase.RESOLVING)
                return true
            }

            // External BattleEndEvent (safety / future-proofing)
            is BattleEndEvent -> {
                currentBattleEntity?.let { entity ->
                    battleComponents.getOrNull(entity)?.battleInProgress = false
                }
                currentBattleEntity = null
                currentPlayerEntity = null
                return true
            }

            // Portal creation on map load
            is MapChangeEvent -> {
                val portalLayer = event.map.layer("portals")
                portalLayer.objects.forEach { mapObject ->
                    val toMap    = mapObject.property("toMap", "")
                    val toPortal = mapObject.property("toPortal", -1)
                    if (toMap.isBlank()) { return@forEach }
                    if (toPortal == -1)  { gdxError("Portal " + mapObject.id + " does not have toPortal property.") }
                    world.entity {
                        add<PortalComponent> {
                            this.id       = mapObject.id
                            this.toMap    = toMap
                            this.toPortal = toPortal
                        }
                        physicsComponentFromShape2D(physicsWorld, 0, 0, mapObject.shape, true)
                    }
                }
                return true
            }

            else -> return false
        }
    }
}
