package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.utils.Scaling
import com.github.jacks.roleplayinggame.RolePlayingGame.Companion.UNIT_SCALE
import com.github.jacks.roleplayinggame.actors.FlipImage
import com.github.jacks.roleplayinggame.components.AnimationComponent
import com.github.jacks.roleplayinggame.components.AnimationModel
import com.github.jacks.roleplayinggame.components.AnimationDirection
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
import com.github.jacks.roleplayinggame.events.BattleLogDismissedEvent
import com.github.jacks.roleplayinggame.events.BattleEndTransitionStartEvent
import com.github.jacks.roleplayinggame.events.BattleEvent
import com.github.jacks.roleplayinggame.events.BattleHealthUpdateEvent
import com.github.jacks.roleplayinggame.events.BattleLogEvent
import com.github.jacks.roleplayinggame.events.BattleMapChangeEvent
import com.github.jacks.roleplayinggame.events.BattlePhaseChangedEvent
import com.github.jacks.roleplayinggame.events.BattleTransitionStartEvent
import com.github.jacks.roleplayinggame.events.MapChangeEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import ktx.app.gdxError
import ktx.preferences.flush
import ktx.preferences.set
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

    private val preferences: Preferences by lazy { Gdx.app.getPreferences("rolePlayingGamePrefs") }

    private var currentBattleEntity: Entity? = null
    private var currentPlayerEntity: Entity? = null

    // Saved overworld enemy data (preserved across the map transition)
    private var savedEnemyModel: AnimationModel = AnimationModel.UNDEFINED
    private var savedEnemyStats: StatComponent? = null
    private var savedEnemyImageWidth: Float = 0f
    private var savedEnemyImageHeight: Float = 0f
    private var savedSpawnerId: Int = -1
    private var savedSpawnerMapId: Int = -1

    // Home positions — both entities return here after attacking
    private var playerOriginX = 0f
    private var playerOriginY = 0f
    private var enemyOriginX = 0f
    private var enemyOriginY = 0f

    // True while a Scene2D action sequence is running on a combatant's FlipImage
    private var sequenceRunning = false

    // -------------------------------------------------------------------------
    // Main tick — drives the state machine each ECS frame
    // -------------------------------------------------------------------------

    override fun onTickEntity(entity: Entity) {
        val battleComponent = battleComponents[entity]

        // Start a new battle when the player walks into this enemy
        if (battleComponent.triggerEntities.isNotEmpty() && !battleComponent.battleInProgress) {
            battleComponent.battleInProgress    = true
            currentPlayerEntity                 = battleComponent.triggerEntities.first()
            battleComponent.triggerEntities.clear()

            // Save overworld enemy data before the map transition destroys the entity
            savedEnemyModel  = animationComponents.getOrNull(entity)?.model ?: AnimationModel.UNDEFINED
            savedEnemyStats  = statComponents.getOrNull(entity)?.copy()
            val img = imageComponents.getOrNull(entity)?.image
            savedEnemyImageWidth  = img?.width  ?: 1f
            savedEnemyImageHeight = img?.height ?: 1f
            savedSpawnerId    = battleComponent.spawnerId
            savedSpawnerMapId = battleComponent.spawnerMapId

            gameStage.fire(BattleTransitionStartEvent(entity))
            return
        }

        if (!battleComponent.battleInProgress) return

        // State machine
        when (battleComponent.phase) {
            BattlePhase.PLAYER_TURN -> { /* idle — waiting for BattleActionSelectedEvent */ }
            BattlePhase.RESOLVING   -> {
                if (sequenceRunning) return  // animation sequence in progress, wait
                resolveAction(entity, battleComponent)
            }
            BattlePhase.ENEMY_TURN  -> executeEnemyTurn(entity, battleComponent)
            BattlePhase.BATTLE_END  -> endBattle(entity, battleComponent)
        }
    }

    // -------------------------------------------------------------------------
    // RESOLVING — kicks off the appropriate attack sequence
    // -------------------------------------------------------------------------

    private fun resolveAction(enemyEntity: Entity, battleComponent: BattleComponent) {
        val playerEntity = currentPlayerEntity ?: return

        if (battleComponent.resolvingPlayer) {
            when (battleComponent.pendingPlayerAction) {
                BattleAction.ATTACK -> startPlayerAttackSequence(playerEntity, enemyEntity, battleComponent)
                BattleAction.FLEE   -> {
                    gameStage.fire(BattleLogEvent("You escaped!"))
                    battleComponent.endReason = BattleEndReason.FLEE
                    transitionPhase(battleComponent, BattlePhase.BATTLE_END)
                }
                BattleAction.NONE   -> { /* no-op */ }
            }
        } else {
            startEnemyAttackSequence(playerEntity, enemyEntity, battleComponent)
        }
    }

    // -------------------------------------------------------------------------
    // ENEMY_TURN — immediately starts the enemy's attack sequence
    // -------------------------------------------------------------------------

    private fun executeEnemyTurn(enemyEntity: Entity, battleComponent: BattleComponent) {
        if (sequenceRunning) return  // sequence already started, wait for it to finish
        val playerEntity = currentPlayerEntity ?: return
        battleComponent.resolvingPlayer = false
        startEnemyAttackSequence(playerEntity, enemyEntity, battleComponent)
    }

    // -------------------------------------------------------------------------
    // BATTLE_END — wait for delay so log messages are readable, then fire event
    // -------------------------------------------------------------------------

    private fun endBattle(entity: Entity, battleComponent: BattleComponent) {
        // Phase 1: Start delay on first tick so the triggering log is readable
        if (battleComponent.endDelayTimer < 0f) {
            battleComponent.endDelayTimer = END_DELAY_SECONDS
            return
        }

        // Phase 2: Count down — when it expires, process outcome exactly once
        if (battleComponent.endDelayTimer > 0f) {
            battleComponent.endDelayTimer -= deltaTime
            if (battleComponent.endDelayTimer > 0f) return
            battleComponent.endDelayTimer = 0f

            val playerEntity = currentPlayerEntity
            when (battleComponent.endReason) {
                BattleEndReason.WIN -> {
                    val message = buildString {
                        append("Victory!")
                        if (playerEntity != null) {
                            val playerStat = statComponents.getOrNull(playerEntity)
                            val enemyStat = statComponents.getOrNull(entity)
                            if (playerStat != null && enemyStat != null) {
                                val xpGained = enemyStat.xpReward
                                if (xpGained > 0) {
                                    append("\nGained $xpGained XP!")
                                    val levelsGained = playerStat.gainExperience(xpGained)
                                    if (levelsGained > 0) {
                                        append("\nLevel up! Now level ${playerStat.level}!")
                                    }
                                }
                            }
                        }
                    }
                    gameStage.fire(BattleLogEvent(message))
                }
                BattleEndReason.LOSE -> {
                    if (playerEntity != null) {
                        val playerStat = statComponents.getOrNull(playerEntity)
                        playerStat?.currentHealth = playerStat?.maxHealth ?: 0f
                    }
                    gameStage.fire(BattleLogEvent("You were defeated...\nReturning to last save point."))
                }
                BattleEndReason.FLEE -> {
                    gameStage.fire(BattleLogEvent("You successfully escaped!"))
                }
            }
            battleComponent.waitingForEndDismiss = true
            return
        }

        // Phase 3: Wait for player to dismiss the result message
        if (battleComponent.waitingForEndDismiss) return

        // Phase 4: Player dismissed — cleanup and transition to overworld
        if (savedSpawnerId >= 0 && savedSpawnerMapId >= 0) {
            preferences.flush {
                this["spawner_${savedSpawnerId}_map_${savedSpawnerMapId}_is_Spawned"] = false
                this["spawner_${savedSpawnerId}_map_${savedSpawnerMapId}_current_time"] = 0f
            }
        }

        battleComponent.battleInProgress     = false
        battleComponent.phase                = BattlePhase.PLAYER_TURN
        battleComponent.endDelayTimer        = -1f
        battleComponent.waitingForEndDismiss = false
        val reason                           = battleComponent.endReason
        currentBattleEntity                  = null
        currentPlayerEntity                  = null
        savedSpawnerId                       = -1
        savedSpawnerMapId                    = -1
        gameStage.fire(BattleEndTransitionStartEvent(reason))
    }

    // -------------------------------------------------------------------------
    // Attack sequences — Scene2D Actions on FlipImage
    // -------------------------------------------------------------------------

    /**
     * Player slides to enemy, plays ATTACK, flashes enemy white, applies damage,
     * slides back, then waits for the player to dismiss before advancing.
     */
    private fun startPlayerAttackSequence(
        playerEntity: Entity,
        enemyEntity: Entity,
        battleComponent: BattleComponent,
    ) {
        val playerImg      = imageComponents.getOrNull(playerEntity)?.image ?: return
        val enemyImg       = imageComponents.getOrNull(enemyEntity)?.image  ?: return
        val playerAnimComp = animationComponents.getOrNull(playerEntity)    ?: return
        val enemyName      = enemyDisplayName(enemyEntity)

        sequenceRunning = true
        var intendedNextPhase = BattlePhase.ENEMY_TURN

        playerImg.addAction(Actions.sequence(
            // 1. Slide toward enemy
            Actions.run { playerAnimComp.nextAnimation(AnimationType.MOVE, AnimationDirection.SIDE) },
            Actions.moveTo(enemyOriginX - ATTACK_OFFSET, playerOriginY, SLIDE_DURATION),

            // 2. Switch to ATTACK (play-once)
            Actions.run {
                playerAnimComp.nextAnimation(AnimationType.ATTACK, AnimationDirection.SIDE)
                playerAnimComp.playMode = Animation.PlayMode.NORMAL
                playerAnimComp.stateTime = 0f
            },

            // 3. Mid-animation: flash the enemy white
            Actions.delay(HIT_FLASH_DELAY),
            Actions.run { enemyImg.useWhiteShader = true },
            Actions.delay(FLASH_DURATION),
            Actions.run { enemyImg.useWhiteShader = false },

            // 4. Wait for ATTACK animation to finish, then deal damage
            waitUntil { playerAnimComp.isAnimationDone },
            Actions.run {
                val dmg = applyDamage(attacker = playerEntity, target = enemyEntity)
                fireHealthUpdate(playerEntity, enemyEntity)
                gameStage.fire(BattleLogEvent("You attack for ${dmg.toInt()} damage!"))
            },

            // 5. Slide back to origin
            Actions.run { playerAnimComp.nextAnimation(AnimationType.MOVE, AnimationDirection.SIDE) },
            Actions.moveTo(playerOriginX, playerOriginY, SLIDE_DURATION),
            Actions.run { playerAnimComp.nextAnimation(AnimationType.IDLE, AnimationDirection.SIDE) },

            // 6. Decide next phase, wait for dismiss
            Actions.run {
                val enemyStat = statComponents.getOrNull(enemyEntity)
                if (enemyStat != null && enemyStat.isDead) {
                    gameStage.fire(BattleLogEvent("$enemyName is defeated!"))
                    battleComponent.endReason = BattleEndReason.WIN
                    intendedNextPhase = BattlePhase.BATTLE_END
                }
                battleComponent.waitingForActionDismiss = true
            },
            waitUntil { !battleComponent.waitingForActionDismiss },

            // 7. Advance state machine
            Actions.run {
                sequenceRunning = false
                transitionPhase(battleComponent, intendedNextPhase)
            }
        ))
    }

    /**
     * Enemy slides to player, plays ATTACK, flashes player white, applies damage,
     * slides back, then waits for the player to dismiss before returning to PLAYER_TURN.
     */
    private fun startEnemyAttackSequence(
        playerEntity: Entity,
        enemyEntity: Entity,
        battleComponent: BattleComponent,
    ) {
        val enemyImg      = imageComponents.getOrNull(enemyEntity)?.image  ?: return
        val playerImg     = imageComponents.getOrNull(playerEntity)?.image ?: return
        val enemyAnimComp = animationComponents.getOrNull(enemyEntity)     ?: return
        val enemyName     = enemyDisplayName(enemyEntity)

        sequenceRunning = true
        var intendedNextPhase = BattlePhase.PLAYER_TURN

        enemyImg.addAction(Actions.sequence(
            // 1. Slide toward player
            Actions.run {
                if (enemyAnimComp.model.hasDirection) {
                    enemyAnimComp.nextAnimation(AnimationType.MOVE, AnimationDirection.SIDE)
                } else {
                    enemyAnimComp.nextAnimation(AnimationType.MOVE)
                }
            },
            Actions.moveTo(playerOriginX + ATTACK_OFFSET, enemyOriginY, SLIDE_DURATION),

            // 2. Switch to ATTACK (play-once)
            Actions.run {
                if (enemyAnimComp.model.hasDirection) {
                    enemyAnimComp.nextAnimation(AnimationType.ATTACK, AnimationDirection.SIDE)
                } else {
                    enemyAnimComp.nextAnimation(AnimationType.ATTACK)
                }
                enemyAnimComp.playMode = Animation.PlayMode.NORMAL
                enemyAnimComp.stateTime = 0f
            },

            // 3. Mid-animation: flash the player white
            Actions.delay(HIT_FLASH_DELAY),
            Actions.run { playerImg.useWhiteShader = true },
            Actions.delay(FLASH_DURATION),
            Actions.run { playerImg.useWhiteShader = false },

            // 4. Wait for ATTACK animation to finish, then deal damage
            waitUntil { enemyAnimComp.isAnimationDone },
            Actions.run {
                val dmg = applyDamage(attacker = enemyEntity, target = playerEntity)
                fireHealthUpdate(playerEntity, enemyEntity)
                gameStage.fire(BattleLogEvent("$enemyName attacks for ${dmg.toInt()} damage!"))
            },

            // 5. Slide back to origin
            Actions.run {
                if (enemyAnimComp.model.hasDirection) {
                    enemyAnimComp.nextAnimation(AnimationType.MOVE, AnimationDirection.SIDE)
                } else {
                    enemyAnimComp.nextAnimation(AnimationType.MOVE)
                }
            },
            Actions.moveTo(enemyOriginX, enemyOriginY, SLIDE_DURATION),
            Actions.run {
                if (enemyAnimComp.model.hasDirection) {
                    enemyAnimComp.nextAnimation(AnimationType.IDLE, AnimationDirection.SIDE)
                } else {
                    enemyAnimComp.nextAnimation(AnimationType.IDLE)
                }
            },

            // 6. Decide next phase, wait for dismiss
            Actions.run {
                val playerStat = statComponents.getOrNull(playerEntity)
                if (playerStat != null && playerStat.isDead) {
                    gameStage.fire(BattleLogEvent("You were defeated..."))
                    battleComponent.endReason = BattleEndReason.LOSE
                    intendedNextPhase = BattlePhase.BATTLE_END
                }
                battleComponent.waitingForActionDismiss = true
            },
            waitUntil { !battleComponent.waitingForActionDismiss },

            // 7. Advance state machine
            Actions.run {
                sequenceRunning = false
                transitionPhase(battleComponent, intendedNextPhase)
            }
        ))
    }

    // -------------------------------------------------------------------------
    // Battle enemy creation
    // -------------------------------------------------------------------------

    /** Create a lightweight enemy entity on the battle map using saved overworld data. */
    private fun createBattleEnemy(map: TiledMap) {
        val spawnerLayer = map.layer("spawners")
        val enemySpawner = spawnerLayer.objects.first { it.name != "player_spawner" }
        val stats = savedEnemyStats ?: return

        // Compute enemy spawn position for origin tracking
        val newEnemyX = enemySpawner.x * UNIT_SCALE - savedEnemyImageWidth * 0.5f + enemySpawner.width * 0.5f * UNIT_SCALE
        val newEnemyY = enemySpawner.y * UNIT_SCALE - enemySpawner.height * 0.5f * UNIT_SCALE

        val newEnemy = world.entity {
            add<ImageComponent> {
                image = FlipImage().apply {
                    setPosition(newEnemyX, newEnemyY)
                    setSize(savedEnemyImageWidth, savedEnemyImageHeight)
                    setScaling(Scaling.fill)
                    flipX = true  // enemy is on the right side — face left toward player
                }
            }
            add<AnimationComponent> {
                nextAnimation(savedEnemyModel, AnimationType.IDLE)
            }
            add<StatComponent> {
                prefsName      = stats.prefsName
                currentHealth  = stats.currentHealth
                maxHealth      = stats.maxHealth
                currentMana    = stats.currentMana
                maxMana        = stats.maxMana
                attackDamage   = stats.attackDamage
                attackPercent  = stats.attackPercent
                attackSpeed    = stats.attackSpeed
                defense        = stats.defense
                defensePercent = stats.defensePercent
                moveSpeed      = stats.moveSpeed
                xpReward       = stats.xpReward
            }
            add<BattleComponent> {
                battleInProgress = true
                phase            = BattlePhase.PLAYER_TURN
            }
        }
        currentBattleEntity = newEnemy
        enemyOriginX = newEnemyX
        enemyOriginY = newEnemyY

        // Reset player to side-facing idle and save player origin
        val playerEntity = currentPlayerEntity ?: return
        animationComponents.getOrNull(playerEntity)?.nextAnimation(AnimationType.IDLE, AnimationDirection.SIDE)
        imageComponents.getOrNull(playerEntity)?.image?.let { playerImg ->
            if (playerImg is FlipImage) playerImg.flipX = false
            playerOriginX = playerImg.x
            playerOriginY = playerImg.y
        }

        fireHealthUpdate(playerEntity, newEnemy)
        gameStage.fire(BattleLogEvent("A ${enemyDisplayName(newEnemy)} appears!"))
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Returns an Action that polls [condition] each frame, completing when it returns true. */
    private fun waitUntil(condition: () -> Boolean): Action = object : Action() {
        override fun act(delta: Float) = condition()
    }

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

            // Player clicked the battle log — dismiss action turn or end-battle message
            is BattleLogDismissedEvent -> {
                val battleEntity    = currentBattleEntity ?: return false
                val battleComponent = battleComponents.getOrNull(battleEntity) ?: return false
                if (battleComponent.waitingForActionDismiss) {
                    battleComponent.waitingForActionDismiss = false
                }
                if (battleComponent.phase == BattlePhase.BATTLE_END && battleComponent.waitingForEndDismiss) {
                    battleComponent.waitingForEndDismiss = false
                }
                return true
            }

            // External BattleEndEvent (safety / future-proofing)
            is BattleEndEvent -> {
                sequenceRunning = false
                currentBattleEntity?.let { battleEntity ->
                    battleComponents.getOrNull(battleEntity)?.let {
                        it.battleInProgress         = false
                        it.endDelayTimer            = -1f
                        it.waitingForActionDismiss  = false
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
        private const val SLIDE_DURATION    = 0.4f
        private const val FLASH_DURATION    = 0.1f
        private const val HIT_FLASH_DELAY   = 0.25f  // seconds after ATTACK starts before the flash
        private const val ATTACK_OFFSET     = 1.5f   // world units — stop this far from target center
    }
}
