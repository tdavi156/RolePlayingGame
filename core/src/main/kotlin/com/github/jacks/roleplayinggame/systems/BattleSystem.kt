package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Scaling
import com.github.jacks.roleplayinggame.RolePlayingGame.Companion.UNIT_SCALE
import com.github.jacks.roleplayinggame.actors.FlipImage
import com.github.jacks.roleplayinggame.components.AnimationComponent
import com.github.jacks.roleplayinggame.components.AnimationModel
import com.github.jacks.roleplayinggame.components.AnimationType
import com.github.jacks.roleplayinggame.components.BattleAction
import com.github.jacks.roleplayinggame.components.BattleComponent
import com.github.jacks.roleplayinggame.components.BattleEndReason
import com.github.jacks.roleplayinggame.components.BattlePhase
import com.github.jacks.roleplayinggame.components.ImageComponent
import com.github.jacks.roleplayinggame.components.PhysicsComponent.Companion.physicsComponentFromShape2D
import com.github.jacks.roleplayinggame.components.PortalComponent
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.events.BattleActionSelectedEvent
import com.github.jacks.roleplayinggame.events.BattleEndEvent
import com.github.jacks.roleplayinggame.events.BattleEvent
import com.github.jacks.roleplayinggame.events.BattleHealthUpdateEvent
import com.github.jacks.roleplayinggame.events.BattleLogEvent
import com.github.jacks.roleplayinggame.events.BattleMapChangeEvent
import com.github.jacks.roleplayinggame.events.BattlePhaseChangedEvent
import com.github.jacks.roleplayinggame.events.MapChangeEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import ktx.app.gdxError
import ktx.tiled.height
import ktx.tiled.id
import ktx.tiled.layer
import ktx.tiled.property
import ktx.tiled.shape
import ktx.tiled.width
import ktx.tiled.x
import ktx.tiled.y

@AllOf([BattleComponent::class])
class BattleSystem(
    private val physicsWorld: World,
    private val gameStage: Stage,
    private val battleComponents: ComponentMapper<BattleComponent>,
    private val statComponents: ComponentMapper<StatComponent>,
    private val animationComponents: ComponentMapper<AnimationComponent>,
    private val imageComponents: ComponentMapper<ImageComponent>,
) : IteratingSystem(), EventListener {

    private var currentBattleEntity: Entity? = null
    private var currentPlayerEntity: Entity? = null

    // Saved overworld enemy data (preserved across the map transition)
    private var savedEnemyModel: AnimationModel = AnimationModel.UNDEFINED
    private var savedEnemyStats: StatComponent? = null
    private var savedEnemyImageWidth: Float = 0f
    private var savedEnemyImageHeight: Float = 0f

    // -------------------------------------------------------------------------
    // Main tick — drives the state machine each ECS frame
    // -------------------------------------------------------------------------

    override fun onTickEntity(entity: Entity) {
        val battleComponent = battleComponents[entity]

        // Start a new battle when the player walks into this enemy
        if (battleComponent.triggerEntities.isNotEmpty() && !battleComponent.battleInProgress) {
            battleComponent.battleInProgress    = true
            currentPlayerEntity                 = battleComponent.triggerEntities.first()  // stored by PhysicsSystem
            battleComponent.triggerEntities.clear()

            // Save overworld enemy data before the map transition destroys the entity
            savedEnemyModel  = animationComponents.getOrNull(entity)?.model ?: AnimationModel.UNDEFINED
            savedEnemyStats  = statComponents.getOrNull(entity)?.copy()
            val img = imageComponents.getOrNull(entity)?.image
            savedEnemyImageWidth  = img?.width  ?: 1f
            savedEnemyImageHeight = img?.height ?: 1f

            // Fire BattleEvent — MapSystem loads battle map, which fires BattleMapChangeEvent,
            // which triggers createBattleEnemy() and sets currentBattleEntity to the new entity.
            gameStage.fire(BattleEvent(enemy = entity))

            // Use the new battle entity (created by createBattleEnemy via BattleMapChangeEvent)
            val battleEnemy = currentBattleEntity ?: return
            fireHealthUpdate(currentPlayerEntity!!, battleEnemy)
            gameStage.fire(BattleLogEvent("A ${enemyDisplayName(battleEnemy)} appears!"))
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
        val enemyName = enemyDisplayName(enemyEntity)

        if (battleComponent.resolvingPlayer) {
            // Player chosen action
            when (battleComponent.pendingPlayerAction) {
                BattleAction.ATTACK -> {
                    val dmg = applyDamage(attacker = playerEntity, target = enemyEntity)
                    gameStage.fire(BattleLogEvent("You attack for ${dmg.toInt()} damage!"))
                }
                BattleAction.FLEE   -> {
                    gameStage.fire(BattleLogEvent("You escaped!"))
                    battleComponent.endReason = BattleEndReason.FLEE
                    transitionPhase(battleComponent, BattlePhase.BATTLE_END)
                    return
                }
                BattleAction.NONE   -> { /* no-op */ }
            }
            fireHealthUpdate(playerEntity, enemyEntity)
            val enemyStat = statComponents.getOrNull(enemyEntity)
            if (enemyStat != null && enemyStat.isDead) {
                gameStage.fire(BattleLogEvent("$enemyName is defeated!"))
                battleComponent.endReason = BattleEndReason.WIN
                transitionPhase(battleComponent, BattlePhase.BATTLE_END)
            } else {
                transitionPhase(battleComponent, BattlePhase.ENEMY_TURN)
            }

        } else {
            // Enemy action (always basic attack)
            val dmg = applyDamage(attacker = enemyEntity, target = playerEntity)
            gameStage.fire(BattleLogEvent("$enemyName attacks for ${dmg.toInt()} damage!"))
            fireHealthUpdate(playerEntity, enemyEntity)
            val playerStat = statComponents.getOrNull(playerEntity)
            if (playerStat != null && playerStat.isDead) {
                gameStage.fire(BattleLogEvent("You were defeated..."))
                battleComponent.endReason = BattleEndReason.LOSE
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
    // BATTLE_END - wait for delay so log messages are readable, then fire event
    // -------------------------------------------------------------------------

    private fun endBattle(entity: Entity, battleComponent: BattleComponent) {
        // Start the countdown on the first tick of BATTLE_END
        if (battleComponent.endDelayTimer < 0f) {
            battleComponent.endDelayTimer = END_DELAY_SECONDS
            return
        }

        battleComponent.endDelayTimer -= deltaTime
        if (battleComponent.endDelayTimer > 0f) return

        // Timer expired — apply per-outcome logic before leaving battle
        val playerEntity = currentPlayerEntity
        when (battleComponent.endReason) {
            BattleEndReason.WIN -> {
                // Enemy health is already 0; LifeSystem + DeathSystem will handle removal
                // Placeholder XP message (Step 9 will expand)
                gameStage.fire(BattleLogEvent("Victory!"))
            }
            BattleEndReason.LOSE -> {
                // Restore player HP so LifeSystem/DeathSystem don't kill them in the overworld
                if (playerEntity != null) {
                    val playerStat = statComponents.getOrNull(playerEntity)
                    if (playerStat != null) {
                        playerStat.currentHealth = playerStat.maxHealth
                    }
                }
            }
            BattleEndReason.FLEE -> {
                // Both combatants keep their current health — nothing to do
            }
        }

        // Reset state for next battle
        battleComponent.battleInProgress = false
        battleComponent.phase            = BattlePhase.PLAYER_TURN
        battleComponent.endDelayTimer    = -1f
        val reason                       = battleComponent.endReason
        currentBattleEntity              = null
        currentPlayerEntity              = null
        gameStage.fire(BattleEndEvent(reason))
    }
    // -------------------------------------------------------------------------
    // Battle enemy creation
    // -------------------------------------------------------------------------

    /** Create a lightweight enemy entity on the battle map using saved overworld data. */
    private fun createBattleEnemy(map: TiledMap) {
        val spawnerLayer = map.layer("spawners")
        val enemySpawner = spawnerLayer.objects.first { it.name != "player_spawner" }
        val stats = savedEnemyStats ?: return

        val newEnemy = world.entity {
            add<ImageComponent> {
                image = FlipImage().apply {
                    setPosition(
                        enemySpawner.x * UNIT_SCALE - savedEnemyImageWidth * 0.5f + enemySpawner.width * 0.5f * UNIT_SCALE,
                        enemySpawner.y * UNIT_SCALE - enemySpawner.height * 0.5f * UNIT_SCALE
                    )
                    setSize(savedEnemyImageWidth, savedEnemyImageHeight)
                    setScaling(Scaling.fill)
                }
            }
            add<AnimationComponent> {
                nextAnimation(savedEnemyModel, AnimationType.IDLE)
            }
            add<StatComponent> {
                prefsName     = stats.prefsName
                currentHealth = stats.currentHealth
                maxHealth     = stats.maxHealth
                currentMana   = stats.currentMana
                maxMana       = stats.maxMana
                attackDamage  = stats.attackDamage
                attackPercent = stats.attackPercent
                attackSpeed   = stats.attackSpeed
                defense       = stats.defense
                defensePercent = stats.defensePercent
                moveSpeed     = stats.moveSpeed
            }
            add<BattleComponent> {
                battleInProgress = true
                phase            = BattlePhase.PLAYER_TURN
            }
        }
        currentBattleEntity = newEnemy
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Set the new phase and notify all listeners (ViewModel, View). */
    private fun transitionPhase(battleComponent: BattleComponent, newPhase: BattlePhase) {
        battleComponent.phase = newPhase
        gameStage.fire(BattlePhaseChangedEvent(newPhase))
    }

    /** Apply damage formula: max(attackDamage - defense, 1). Returns damage dealt. */
    private fun applyDamage(attacker: Entity, target: Entity): Float {
        val attackerStat = statComponents.getOrNull(attacker) ?: return 0f
        val targetStat   = statComponents.getOrNull(target)   ?: return 0f
        val damage = (attackerStat.attackDamage - targetStat.defense).coerceAtLeast(1f)
        targetStat.currentHealth = (targetStat.currentHealth - damage).coerceAtLeast(0f)
        return damage
    }

    /** Get a display-friendly name from the enemy's AnimationModel (e.g. SLIME_GREEN → "Slime Green"). */
    private fun enemyDisplayName(entity: Entity): String {
        val model = animationComponents.getOrNull(entity)?.model ?: return "Enemy"
        return model.name.split("_").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
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
                currentBattleEntity?.let { battleEntity ->
                    battleComponents.getOrNull(battleEntity)?.let {
                        it.battleInProgress = false
                        it.endDelayTimer = -1f
                    }
                }
                currentBattleEntity = null
                currentPlayerEntity = null
                return true
            }

            // Battle map loaded — create the battle enemy entity
            is BattleMapChangeEvent -> {
                createBattleEnemy(event.map)
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

    companion object {
        private const val END_DELAY_SECONDS = 1.5f
    }
}
