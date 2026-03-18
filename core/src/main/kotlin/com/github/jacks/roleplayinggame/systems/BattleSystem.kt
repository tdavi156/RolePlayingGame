package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
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
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.PortalComponent
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.components.NonPlayerConfiguration
import com.github.jacks.roleplayinggame.configurations.BattleComp
import com.github.jacks.roleplayinggame.configurations.BATTLE_COMPS
import com.github.jacks.roleplayinggame.configurations.Configurations
import com.github.jacks.roleplayinggame.configurations.EnemyType
import com.github.jacks.roleplayinggame.configurations.RANDOM_COMPS
import com.github.jacks.roleplayinggame.configurations.rollForDrop
import com.github.jacks.roleplayinggame.configurations.rollRandomItem
import com.github.jacks.roleplayinggame.configurations.EquipmentItemData
import com.github.jacks.roleplayinggame.events.BattleActionSelectedEvent
import com.github.jacks.roleplayinggame.events.BattleEndEvent
import com.github.jacks.roleplayinggame.events.BattleLogDismissedEvent
import com.github.jacks.roleplayinggame.events.BattleEndTransitionStartEvent
import com.github.jacks.roleplayinggame.events.BattleEvent
import com.github.jacks.roleplayinggame.events.BattleHealthUpdateEvent
import com.github.jacks.roleplayinggame.events.BattleLogEvent
import com.github.jacks.roleplayinggame.events.BattleMapChangeEvent
import com.github.jacks.roleplayinggame.events.BattlePhaseChangedEvent
import com.github.jacks.roleplayinggame.events.BattleReadyEvent
import com.github.jacks.roleplayinggame.events.BattleRewardData
import com.github.jacks.roleplayinggame.events.BattleRewardEvent
import com.github.jacks.roleplayinggame.events.BattleTransitionStartEvent
import com.github.jacks.roleplayinggame.configurations.AbilityEffect
import com.github.jacks.roleplayinggame.configurations.ABILITY_TREES
import com.github.jacks.roleplayinggame.events.CastSpellEvent
import com.github.jacks.roleplayinggame.events.CombatItemUseDismissedEvent
import com.github.jacks.roleplayinggame.events.CombatSpeedChangedEvent
import com.github.jacks.roleplayinggame.events.EnemyKilledEvent
import com.github.jacks.roleplayinggame.events.EnemySelectNextEvent
import com.github.jacks.roleplayinggame.events.EnemySelectPrevEvent
import com.github.jacks.roleplayinggame.events.EnemySelectionIndexChangedEvent
import com.github.jacks.roleplayinggame.events.EnemySelectionModeEndedEvent
import com.github.jacks.roleplayinggame.events.EnemySelectionModeStartedEvent
import com.github.jacks.roleplayinggame.events.FloatingTextEvent
import com.github.jacks.roleplayinggame.events.GainAbilityPointEvent
import com.github.jacks.roleplayinggame.events.GainSkillPointEvent
import com.github.jacks.roleplayinggame.events.ItemUseFlashEvent
import com.github.jacks.roleplayinggame.events.LevelUpEvent
import com.github.jacks.roleplayinggame.events.MapChangeEvent
import com.github.jacks.roleplayinggame.events.SpellCastDismissedEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.ui.Fonts
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
import kotlin.random.Random

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
    private val playerFamily by lazy { world.family(allOf = arrayOf(PlayerComponent::class)) }
    private val resourceSystem by lazy { world.system<ResourceSystem>() }
    private val inventorySystem by lazy { world.system<InventorySystem>() }

    // -------------------------------------------------------------------------
    // Overworld trigger data (saved before map transition)
    // -------------------------------------------------------------------------

    private var currentBattleEntity: Entity? = null    // primary enemy (drives state machine)
    private var currentPlayerEntity: Entity? = null

    private var savedEnemyModel: AnimationModel = AnimationModel.UNDEFINED
    private var savedEnemyStats: StatComponent? = null
    private var savedEnemyImageWidth: Float = 0f
    private var savedEnemyImageHeight: Float = 0f
    private var savedSpawnerId: Int = -1
    private var savedSpawnerMapId: Int = -1
    private var savedNonPlayerConfig: NonPlayerConfiguration? = null

    // -------------------------------------------------------------------------
    // Multi-enemy session state
    // -------------------------------------------------------------------------

    private val allEnemyEntities = mutableListOf<Entity>()
    private val enemyConfigs = mutableMapOf<Entity, NonPlayerConfiguration>()
    /** Bottom-left image position of each enemy (same coordinate system as Image.x/y). */
    private val enemyOrigins = mutableListOf<Vector2>()
    /** Enemies whose kill rewards have already been awarded this battle. */
    private val killedEnemiesRewarded = mutableSetOf<Entity>()

    // Speed-based turn order (player + all living enemies, sorted by attackSpeed desc)
    private val turnOrder = mutableListOf<Entity>()
    private var currentTurnIndex = 0
    private var activeAttackingEnemy: Entity? = null

    // Enemy selection
    private var selectedEnemyIndex = 0
    private var inSelectionMode = false

    // Escrow
    private var pendingGold = 0
    private val pendingLoot = mutableListOf<EquipmentItemData?>()
    private var totalXpGained = 0
    private var totalLevelsGained = 0
    private var finalPlayerLevel = 0

    // Home positions for animation
    private var playerOriginX = 0f
    private var playerOriginY = 0f

    private var sequenceRunning = false

    // World-space selection indicator
    private var selectionIndicator: Image? = null

    // -------------------------------------------------------------------------
    // Main tick — primary entity only drives the state machine
    // -------------------------------------------------------------------------

    override fun onTickEntity(entity: Entity) {
        val battleComponent = battleComponents[entity]

        // Overworld trigger detection
        if (battleComponent.triggerEntities.isNotEmpty() && !battleComponent.battleInProgress) {
            battleComponent.battleInProgress = true
            currentPlayerEntity              = battleComponent.triggerEntities.first()
            battleComponent.triggerEntities.clear()

            savedEnemyModel       = animationComponents.getOrNull(entity)?.model ?: AnimationModel.UNDEFINED
            savedEnemyStats       = statComponents.getOrNull(entity)?.copy()
            val img               = imageComponents.getOrNull(entity)?.image
            savedEnemyImageWidth  = img?.width  ?: 1f
            savedEnemyImageHeight = img?.height ?: 1f
            savedSpawnerId        = battleComponent.spawnerId
            savedSpawnerMapId     = battleComponent.spawnerMapId
            savedNonPlayerConfig  = Configurations.getNonPlayerConfig(savedEnemyModel)

            gameStage.fire(BattleTransitionStartEvent(entity))
            return
        }

        // Only the primary entity drives the state machine
        if (entity != currentBattleEntity) return
        if (!battleComponent.battleInProgress) return

        when (battleComponent.phase) {
            BattlePhase.PLAYER_TURN -> { /* waiting for BattleActionSelectedEvent */ }
            BattlePhase.RESOLVING   -> {
                if (sequenceRunning) return
                resolveAction(battleComponent)
            }
            BattlePhase.ENEMY_TURN  -> executeEnemyTurn(battleComponent)
            BattlePhase.BATTLE_END  -> endBattle(entity, battleComponent)
        }
    }

    // -------------------------------------------------------------------------
    // RESOLVING
    // -------------------------------------------------------------------------

    private fun resolveAction(battleComponent: BattleComponent) {
        val playerEntity = currentPlayerEntity ?: return

        if (battleComponent.resolvingPlayer) {
            when (battleComponent.pendingPlayerAction) {
                BattleAction.ATTACK -> {
                    val target = getTargetEntity(battleComponent.pendingTargetIndex) ?: return
                    startPlayerAttackSequence(playerEntity, target, battleComponent)
                }
                BattleAction.FLEE -> {
                    gameStage.fire(BattleLogEvent("You escaped!"))
                    battleComponent.endReason = BattleEndReason.FLEE
                    transitionPhase(battleComponent, BattlePhase.BATTLE_END)
                }
                BattleAction.NONE -> { /* no-op */ }
            }
        } else {
            val enemyEntity = activeAttackingEnemy ?: return
            startEnemyAttackSequence(playerEntity, enemyEntity, battleComponent)
        }
    }

    // -------------------------------------------------------------------------
    // ENEMY_TURN
    // -------------------------------------------------------------------------

    private fun executeEnemyTurn(battleComponent: BattleComponent) {
        if (sequenceRunning) return
        val playerEntity = currentPlayerEntity ?: return
        val enemyEntity  = activeAttackingEnemy ?: return
        battleComponent.resolvingPlayer = false
        startEnemyAttackSequence(playerEntity, enemyEntity, battleComponent)
    }

    // -------------------------------------------------------------------------
    // BATTLE_END
    // -------------------------------------------------------------------------

    private fun endBattle(entity: Entity, battleComponent: BattleComponent) {
        if (battleComponent.endDelayTimer < 0f) {
            battleComponent.endDelayTimer = END_DELAY_SECONDS
            return
        }
        if (battleComponent.endDelayTimer > 0f) {
            battleComponent.endDelayTimer -= deltaTime
            if (battleComponent.endDelayTimer > 0f) return
            battleComponent.endDelayTimer = 0f

            when (battleComponent.endReason) {
                BattleEndReason.WIN -> {
                    val message = buildString {
                        append("Victory!")
                        if (totalXpGained > 0) {
                            append("\nGained $totalXpGained XP!")
                            if (totalLevelsGained > 0) {
                                append("\nLevel up! Now level $finalPlayerLevel!")
                            }
                        }
                    }
                    gameStage.fire(BattleLogEvent(message))
                }
                BattleEndReason.LOSE -> {
                    currentPlayerEntity?.let { pe ->
                        val pStat = statComponents.getOrNull(pe)
                        pStat?.currentHealth = pStat?.maxHealth ?: 0f
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

        if (battleComponent.waitingForEndDismiss) return

        // Cleanup spawner prefs
        if (savedSpawnerId >= 0 && savedSpawnerMapId >= 0) {
            preferences.flush {
                this["spawner_${savedSpawnerId}_map_${savedSpawnerMapId}_is_Spawned"] = false
                this["spawner_${savedSpawnerId}_map_${savedSpawnerMapId}_current_time"] = 0f
            }
        }

        val reason = battleComponent.endReason
        battleComponent.battleInProgress     = false
        battleComponent.phase                = BattlePhase.PLAYER_TURN
        battleComponent.endDelayTimer        = -1f
        battleComponent.waitingForEndDismiss = false
        currentBattleEntity                  = null
        currentPlayerEntity                  = null
        savedSpawnerId                       = -1
        savedSpawnerMapId                    = -1
        savedNonPlayerConfig                 = null

        hideSelectionIndicator()

        if (reason == BattleEndReason.WIN) {
            // Release escrow to systems
            resourceSystem.resources.gold += pendingGold
            resourceSystem.saveResources()
            pendingLoot.filterNotNull().forEach { item -> inventorySystem.addItem(item) }
            val rewardData = BattleRewardData(totalXpGained, pendingGold, pendingLoot.filterNotNull())
            clearEscrow()
            clearSessionState()
            gameStage.fire(BattleRewardEvent(rewardData))
        } else {
            // Discard escrow on defeat / flee
            clearEscrow()
            clearSessionState()
            gameStage.fire(BattleEndTransitionStartEvent(reason))
        }
    }

    // -------------------------------------------------------------------------
    // Turn order (Parts 3 & 6)
    // -------------------------------------------------------------------------

    private fun buildTurnOrder() {
        val playerEntity = currentPlayerEntity ?: return
        turnOrder.clear()
        turnOrder.add(playerEntity)
        allEnemyEntities.forEach { e ->
            if (!(statComponents.getOrNull(e)?.isDead ?: true)) turnOrder.add(e)
        }
        sortTurnOrder(preserveActive = false)
        currentTurnIndex = 0
    }

    private fun sortTurnOrder(preserveActive: Boolean) {
        val activeEntity = if (preserveActive) turnOrder.getOrNull(currentTurnIndex) else null
        turnOrder.sortWith { a, b ->
            val sA = statComponents.getOrNull(a)?.attackSpeed ?: 0f
            val sB = statComponents.getOrNull(b)?.attackSpeed ?: 0f
            when {
                sB > sA -> 1
                sA > sB -> -1
                else    -> if (Random.nextBoolean()) 1 else -1
            }
        }
        // Restore currently active entity to its slot so it isn't displaced
        if (activeEntity != null) {
            val newPos = turnOrder.indexOf(activeEntity)
            if (newPos >= 0 && newPos != currentTurnIndex) {
                turnOrder.removeAt(newPos)
                turnOrder.add(currentTurnIndex.coerceAtMost(turnOrder.size), activeEntity)
            }
        }
    }

    private fun advanceTurn(battleComponent: BattleComponent) {
        val playerEntity  = currentPlayerEntity ?: return
        val playerDead    = statComponents.getOrNull(playerEntity)?.isDead ?: false
        val livingEnemies = allEnemyEntities.filter { !(statComponents.getOrNull(it)?.isDead ?: true) }

        if (livingEnemies.isEmpty()) {
            battleComponent.endReason = BattleEndReason.WIN
            transitionPhase(battleComponent, BattlePhase.BATTLE_END)
            return
        }
        if (playerDead) {
            battleComponent.endReason = BattleEndReason.LOSE
            transitionPhase(battleComponent, BattlePhase.BATTLE_END)
            return
        }

        turnOrder.removeAll { e -> e != playerEntity && (statComponents.getOrNull(e)?.isDead ?: true) }

        if (turnOrder.isEmpty()) {
            battleComponent.endReason = BattleEndReason.WIN
            transitionPhase(battleComponent, BattlePhase.BATTLE_END)
            return
        }

        currentTurnIndex = (currentTurnIndex + 1) % turnOrder.size
        setPhaseForCurrentTurn(battleComponent)
    }

    private fun setPhaseForCurrentTurn(battleComponent: BattleComponent) {
        val currentEntity = turnOrder.getOrNull(currentTurnIndex)
        if (currentEntity == currentPlayerEntity) {
            autoAdvanceSelectedEnemy()
            transitionPhase(battleComponent, BattlePhase.PLAYER_TURN)
        } else {
            activeAttackingEnemy = currentEntity
            transitionPhase(battleComponent, BattlePhase.ENEMY_TURN)
        }
    }

    private fun autoAdvanceSelectedEnemy() {
        val livingIndices = allEnemyEntities.indices.filter { idx ->
            !(statComponents.getOrNull(allEnemyEntities[idx])?.isDead ?: true)
        }
        if (livingIndices.isEmpty()) return
        if (selectedEnemyIndex !in livingIndices) {
            selectedEnemyIndex = livingIndices.firstOrNull { it >= selectedEnemyIndex }
                ?: livingIndices.first()
        }
    }

    // -------------------------------------------------------------------------
    // Enemy selection indicator (Part 7)
    // -------------------------------------------------------------------------

    private fun getOrCreateIndicator(): Image {
        return selectionIndicator ?: run {
            // Create a simple yellow downward-pointing arrow using a filled pixmap
            val size = 16
            val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)
            pixmap.setColor(Color.YELLOW)
            // Draw a filled triangle: apex at bottom-center, base at top
            for (row in 0 until size) {
                val halfWidth = (size / 2 - row / 2).coerceAtLeast(1)
                val startX = size / 2 - halfWidth
                val endX   = size / 2 + halfWidth
                for (col in startX..endX) {
                    pixmap.drawPixel(col, row)
                }
            }
            val tex = Texture(pixmap)
            pixmap.dispose()
            val img = Image(TextureRegionDrawable(TextureRegion(tex))).apply {
                setSize(INDICATOR_SIZE, INDICATOR_SIZE)
                isVisible = false
            }
            gameStage.addActor(img)
            selectionIndicator = img
            img
        }
    }

    private fun showSelectionIndicator(index: Int) {
        val origin = enemyOrigins.getOrNull(index) ?: return
        val img    = getOrCreateIndicator()
        // Position: centred horizontally above enemy, pointing down at the enemy
        img.setPosition(
            origin.x + savedEnemyImageWidth * 0.5f - INDICATOR_SIZE * 0.5f,
            origin.y + savedEnemyImageHeight + INDICATOR_PADDING
        )
        img.isVisible = true
    }

    private fun hideSelectionIndicator() {
        selectionIndicator?.isVisible = false
    }

    private fun cycleSelectionIndex(delta: Int) {
        val livingIndices = allEnemyEntities.indices.filter { idx ->
            !(statComponents.getOrNull(allEnemyEntities[idx])?.isDead ?: true)
        }
        if (livingIndices.size < 2) return
        val currentPos = livingIndices.indexOf(selectedEnemyIndex).coerceAtLeast(0)
        val newPos     = ((currentPos + delta) + livingIndices.size) % livingIndices.size
        selectedEnemyIndex = livingIndices[newPos]
        showSelectionIndicator(selectedEnemyIndex)
        gameStage.fire(EnemySelectionIndexChangedEvent(selectedEnemyIndex))
    }

    // -------------------------------------------------------------------------
    // Battle enemy creation (Parts 2 & 3)
    // -------------------------------------------------------------------------

    private fun createBattleEnemies(map: TiledMap) {
        val spawnerLayer  = map.layer("spawners")
        val enemySpawners = spawnerLayer.objects
            .filter { it.name != "player_spawner" }
            .sortedBy { it.name }

        val comp = resolveBattleComp() ?: return

        val playerEntity = currentPlayerEntity ?: return
        animationComponents.getOrNull(playerEntity)?.nextAnimation(AnimationType.IDLE, AnimationDirection.SIDE)
        imageComponents.getOrNull(playerEntity)?.image?.let { playerImg ->
            if (playerImg is FlipImage) playerImg.flipX = false
            playerOriginX = playerImg.x
            playerOriginY = playerImg.y
        }

        val units = listOfNotNull(comp.unit1, comp.unit2, comp.unit3)
        units.forEachIndexed { index, enemyType ->
            val spawner = enemySpawners.getOrNull(index) ?: return@forEachIndexed
            val config  = Configurations.getNonPlayerConfig(enemyType.animationModel) ?: return@forEachIndexed
            val stats   = config.stats

            // Bottom-left image position (same as original single-enemy code)
            val imgX = spawner.x * UNIT_SCALE - savedEnemyImageWidth * 0.5f + spawner.width * 0.5f * UNIT_SCALE
            val imgY = spawner.y * UNIT_SCALE - spawner.height * 0.5f * UNIT_SCALE

            val newEnemy = world.entity {
                add<ImageComponent> {
                    image = FlipImage().apply {
                        setPosition(imgX, imgY)
                        setSize(savedEnemyImageWidth, savedEnemyImageHeight)
                        setScaling(Scaling.fill)
                        flipX = true
                    }
                }
                add<AnimationComponent> {
                    nextAnimation(enemyType.animationModel, AnimationType.IDLE)
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
                    xpReward       = config.xpReward
                }
                add<BattleComponent> {
                    battleInProgress = true
                    phase            = BattlePhase.PLAYER_TURN
                }
            }

            allEnemyEntities.add(newEnemy)
            enemyConfigs[newEnemy] = config
            // Store image bottom-left position for animation/indicator use
            enemyOrigins.add(Vector2(imgX, imgY))
        }

        if (allEnemyEntities.isEmpty()) return

        currentBattleEntity = allEnemyEntities.first()
        buildTurnOrder()
        setPhaseForCurrentTurn(battleComponents[currentBattleEntity!!])

        fireHealthUpdate()
        val names = allEnemyEntities.joinToString(" and ") { enemyDisplayName(it) }
        val intro = if (allEnemyEntities.size == 1) "A $names appears!" else "$names appear!"
        gameStage.fire(BattleLogEvent(intro))
        gameStage.fire(BattleReadyEvent(allEnemyEntities.toList()))
    }

    private fun resolveBattleComp(): BattleComp? {
        val compId = savedNonPlayerConfig?.battleCompId
        if (compId != null) {
            return BATTLE_COMPS[compId]
                ?: gdxError("battleCompId=$compId not found in BATTLE_COMPS")
        }
        val enemyType = EnemyType.entries.find { it.animationModel == savedEnemyModel }
            ?: return BattleComp(id = 0, unit1 = EnemyType.GREEN_SLIME)
        val pool = RANDOM_COMPS[enemyType]
        if (pool.isNullOrEmpty()) return BattleComp(id = 0, unit1 = enemyType)
        return pool[Random.nextInt(pool.size)]
    }

    // -------------------------------------------------------------------------
    // Per-kill rewards (Parts 4 & 5)
    // -------------------------------------------------------------------------

    /** Awards XP immediately, fires EnemyKilledEvent, accrues gold/loot to escrow. */
    private fun awardKillRewards(playerEntity: Entity, enemyEntity: Entity) {
        if (enemyEntity in killedEnemiesRewarded) return
        killedEnemiesRewarded.add(enemyEntity)

        val playerStat = statComponents.getOrNull(playerEntity)
        val enemyStat  = statComponents.getOrNull(enemyEntity)

        if (playerStat != null && enemyStat != null && enemyStat.xpReward > 0) {
            val xp     = enemyStat.xpReward
            totalXpGained += xp
            val levels = playerStat.gainExperience(xp)
            if (levels > 0) {
                totalLevelsGained += levels
                finalPlayerLevel   = playerStat.level
                repeat(levels) {
                    gameStage.fire(LevelUpEvent(playerEntity, playerStat.level))
                    gameStage.fire(GainSkillPointEvent(playerEntity))
                    gameStage.fire(GainAbilityPointEvent(playerEntity))
                }
                preferences.flush {
                    this["player_level"]      = playerStat.level
                    this["player_experience"] = playerStat.experience
                }
            }
        }

        val model = animationComponents.getOrNull(enemyEntity)?.model
        EnemyType.entries.find { it.animationModel == model }?.let { gameStage.fire(EnemyKilledEvent(it)) }

        val config = enemyConfigs[enemyEntity]
        pendingGold += config?.goldReward ?: 0
        val drop = config?.lootPool?.let { pool ->
            if (rollForDrop(config.lootChance)) rollRandomItem(pool) else null
        }
        pendingLoot.add(drop)
    }

    private fun clearEscrow() {
        pendingGold       = 0
        pendingLoot.clear()
        totalXpGained     = 0
        totalLevelsGained = 0
        finalPlayerLevel  = 0
    }

    private fun clearSessionState() {
        allEnemyEntities.clear()
        enemyConfigs.clear()
        enemyOrigins.clear()
        killedEnemiesRewarded.clear()
        turnOrder.clear()
        currentTurnIndex     = 0
        activeAttackingEnemy = null
        selectedEnemyIndex   = 0
        inSelectionMode      = false
    }

    // -------------------------------------------------------------------------
    // Attack sequences
    // -------------------------------------------------------------------------

    private fun startPlayerAttackSequence(
        playerEntity: Entity,
        enemyEntity: Entity,
        battleComponent: BattleComponent,
    ) {
        val playerImg      = imageComponents.getOrNull(playerEntity)?.image ?: return
        val enemyImg       = imageComponents.getOrNull(enemyEntity)?.image  ?: return
        val playerAnimComp = animationComponents.getOrNull(playerEntity)    ?: return
        val enemyAnimComp  = animationComponents.getOrNull(enemyEntity)     ?: return
        val enemyName      = enemyDisplayName(enemyEntity)
        val eOrigin        = getEnemyOrigin(enemyEntity)   // bottom-left of enemy image

        val peekDmg  = peekDamage(playerEntity, enemyEntity)
        val isLethal = (statComponents.getOrNull(enemyEntity)?.currentHealth ?: 0f) - peekDmg <= 0f
        val dispDmg  = peekDmg.toInt()

        sequenceRunning = true

        playerImg.addAction(Actions.sequence(
            Actions.run { playerAnimComp.nextAnimation(AnimationType.MOVE, AnimationDirection.SIDE) },
            Actions.moveTo(eOrigin.x - ATTACK_OFFSET, playerOriginY, SLIDE_DURATION),

            Actions.run {
                playerAnimComp.nextAnimation(AnimationType.ATTACK, AnimationDirection.SIDE)
                playerAnimComp.playMode = Animation.PlayMode.NORMAL
                playerAnimComp.stateTime = 0f
            },

            Actions.delay(HIT_FLASH_DELAY),
            Actions.run {
                enemyImg.useWhiteShader = true
                // Floating text at enemy centre
                gameStage.fire(FloatingTextEvent(
                    Vector2(eOrigin.x + savedEnemyImageWidth * 0.5f, eOrigin.y + savedEnemyImageHeight * 0.5f),
                    dispDmg.toString(), Fonts.DAMAGE
                ))
                if (isLethal) {
                    enemyAnimComp.nextAnimation(AnimationType.DEATH)
                    enemyAnimComp.playMode = Animation.PlayMode.NORMAL
                    enemyAnimComp.stateTime = 0f
                }
            },
            Actions.delay(FLASH_DURATION),
            Actions.run { enemyImg.useWhiteShader = false },

            waitUntil { playerAnimComp.isAnimationDone },
            Actions.run {
                val dmg = applyDamage(attacker = playerEntity, target = enemyEntity)
                fireHealthUpdate()
                gameStage.fire(BattleLogEvent("You attack for ${dmg.toInt()} damage!"))
            },

            Actions.run { playerAnimComp.nextAnimation(AnimationType.MOVE, AnimationDirection.SIDE) },
            Actions.moveTo(playerOriginX, playerOriginY, SLIDE_DURATION),
            Actions.run { playerAnimComp.nextAnimation(AnimationType.IDLE, AnimationDirection.SIDE) },

            waitUntil { !isLethal || enemyAnimComp.isAnimationDone },

            Actions.run {
                if (isLethal) {
                    awardKillRewards(playerEntity, enemyEntity)
                    gameStage.fire(BattleLogEvent("$enemyName is defeated!"))
                }
                battleComponent.waitingForActionDismiss = true
            },
            waitUntil { !battleComponent.waitingForActionDismiss },

            Actions.run {
                sequenceRunning = false
                advanceTurn(battleComponent)
            }
        ))
    }

    private fun startEnemyAttackSequence(
        playerEntity: Entity,
        enemyEntity: Entity,
        battleComponent: BattleComponent,
    ) {
        val enemyImg       = imageComponents.getOrNull(enemyEntity)?.image  ?: return
        val playerImg      = imageComponents.getOrNull(playerEntity)?.image ?: return
        val enemyAnimComp  = animationComponents.getOrNull(enemyEntity)     ?: return
        val playerAnimComp = animationComponents.getOrNull(playerEntity)    ?: return
        val enemyName      = enemyDisplayName(enemyEntity)
        val eOrigin        = getEnemyOrigin(enemyEntity)

        val peekDmg  = peekDamage(enemyEntity, playerEntity)
        val isLethal = (statComponents.getOrNull(playerEntity)?.currentHealth ?: 0f) - peekDmg <= 0f
        val dispDmg  = peekDmg.toInt()

        sequenceRunning = true

        enemyImg.addAction(Actions.sequence(
            Actions.run {
                if (enemyAnimComp.model.hasDirection)
                    enemyAnimComp.nextAnimation(AnimationType.MOVE, AnimationDirection.SIDE)
                else
                    enemyAnimComp.nextAnimation(AnimationType.MOVE)
            },
            Actions.moveTo(playerOriginX + ATTACK_OFFSET, eOrigin.y, SLIDE_DURATION),

            Actions.run {
                if (enemyAnimComp.model.hasDirection)
                    enemyAnimComp.nextAnimation(AnimationType.ATTACK, AnimationDirection.SIDE)
                else
                    enemyAnimComp.nextAnimation(AnimationType.ATTACK)
                enemyAnimComp.playMode = Animation.PlayMode.NORMAL
                enemyAnimComp.stateTime = 0f
            },

            Actions.delay(HIT_FLASH_DELAY),
            Actions.run {
                playerImg.useWhiteShader = true
                gameStage.fire(FloatingTextEvent(Vector2(playerOriginX, playerOriginY), dispDmg.toString(), Fonts.DAMAGE))
                if (isLethal) {
                    playerAnimComp.nextAnimation(AnimationType.DEATH)
                    playerAnimComp.playMode = Animation.PlayMode.NORMAL
                    playerAnimComp.stateTime = 0f
                }
            },
            Actions.delay(FLASH_DURATION),
            Actions.run { playerImg.useWhiteShader = false },

            waitUntil { enemyAnimComp.isAnimationDone },
            Actions.run {
                val dmg = applyDamage(attacker = enemyEntity, target = playerEntity)
                fireHealthUpdate()
                gameStage.fire(BattleLogEvent("$enemyName attacks for ${dmg.toInt()} damage!"))
            },

            Actions.run {
                if (enemyAnimComp.model.hasDirection)
                    enemyAnimComp.nextAnimation(AnimationType.MOVE, AnimationDirection.SIDE)
                else
                    enemyAnimComp.nextAnimation(AnimationType.MOVE)
            },
            Actions.moveTo(eOrigin.x, eOrigin.y, SLIDE_DURATION),
            Actions.run {
                if (enemyAnimComp.model.hasDirection)
                    enemyAnimComp.nextAnimation(AnimationType.IDLE, AnimationDirection.SIDE)
                else
                    enemyAnimComp.nextAnimation(AnimationType.IDLE)
            },

            waitUntil { !isLethal || playerAnimComp.isAnimationDone },

            Actions.run {
                if (isLethal) {
                    gameStage.fire(BattleLogEvent("You were defeated..."))
                    battleComponent.endReason = BattleEndReason.LOSE
                }
                battleComponent.waitingForActionDismiss = true
            },
            waitUntil { !battleComponent.waitingForActionDismiss },

            Actions.run {
                sequenceRunning = false
                advanceTurn(battleComponent)
            }
        ))
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun waitUntil(condition: () -> Boolean): Action = object : Action() {
        override fun act(delta: Float) = condition()
    }

    private fun transitionPhase(battleComponent: BattleComponent, newPhase: BattlePhase) {
        battleComponent.phase = newPhase
        gameStage.fire(BattlePhaseChangedEvent(newPhase))
    }

    private fun applyDamage(attacker: Entity, target: Entity): Float {
        val aS  = statComponents.getOrNull(attacker) ?: return 0f
        val tS  = statComponents.getOrNull(target)   ?: return 0f
        val dmg = (aS.attackDamage - tS.defense).coerceAtLeast(1f)
        tS.currentHealth = (tS.currentHealth - dmg).coerceAtLeast(0f)
        return dmg
    }

    private fun peekDamage(attacker: Entity, target: Entity): Float {
        val aS = statComponents.getOrNull(attacker) ?: return 0f
        val tS = statComponents.getOrNull(target)   ?: return 0f
        return (aS.attackDamage - tS.defense).coerceAtLeast(1f)
    }

    private fun fireHealthUpdate() {
        val playerEntity = currentPlayerEntity ?: return
        val pStat        = statComponents.getOrNull(playerEntity)
        val playerPct    = if (pStat != null && pStat.maxHealth > 0f)
            (pStat.currentHealth / pStat.maxHealth).coerceIn(0f, 1f) else 1f
        val enemyPcts = allEnemyEntities.map { e ->
            val eStat = statComponents.getOrNull(e)
            if (eStat != null && eStat.maxHealth > 0f)
                (eStat.currentHealth / eStat.maxHealth).coerceIn(0f, 1f) else 0f
        }
        gameStage.fire(BattleHealthUpdateEvent(playerPct, enemyPcts))
    }

    private fun enemyDisplayName(entity: Entity): String {
        val model = animationComponents.getOrNull(entity)?.model ?: return "Enemy"
        return model.name.split("_").joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercase() } }
    }

    /** Returns the stored bottom-left image position for the given enemy entity. */
    private fun getEnemyOrigin(entity: Entity): Vector2 {
        val idx = allEnemyEntities.indexOf(entity)
        return if (idx >= 0 && idx < enemyOrigins.size) enemyOrigins[idx] else Vector2.Zero
    }

    /** Returns the first living enemy at targetIndex, or the first living enemy overall. */
    private fun getTargetEntity(targetIndex: Int): Entity? {
        val living = allEnemyEntities.filter { !(statComponents.getOrNull(it)?.isDead ?: true) }
        return living.getOrNull(targetIndex) ?: living.firstOrNull()
    }

    // -------------------------------------------------------------------------
    // Event handling
    // -------------------------------------------------------------------------

    override fun handle(event: Event): Boolean {
        when (event) {
            is BattleActionSelectedEvent -> {
                val battleEntity    = currentBattleEntity ?: return false
                val battleComponent = battleComponents.getOrNull(battleEntity) ?: return false
                if (battleComponent.phase != BattlePhase.PLAYER_TURN) return false
                battleComponent.pendingPlayerAction = event.action
                battleComponent.pendingTargetIndex  = event.targetIndex
                battleComponent.resolvingPlayer      = true
                transitionPhase(battleComponent, BattlePhase.RESOLVING)
                return true
            }

            is BattleLogDismissedEvent -> {
                val battleEntity    = currentBattleEntity ?: return false
                val battleComponent = battleComponents.getOrNull(battleEntity) ?: return false
                if (battleComponent.waitingForActionDismiss)   battleComponent.waitingForActionDismiss = false
                if (battleComponent.phase == BattlePhase.BATTLE_END && battleComponent.waitingForEndDismiss)
                    battleComponent.waitingForEndDismiss = false
                if (battleComponent.waitingForSpellDismiss) gameStage.fire(SpellCastDismissedEvent())
                return true
            }

            is CombatItemUseDismissedEvent -> {
                val battleEntity    = currentBattleEntity ?: return false
                val battleComponent = battleComponents.getOrNull(battleEntity) ?: return false
                if (battleComponent.phase == BattlePhase.PLAYER_TURN) {
                    // Advance turn (item use consumes player's turn)
                    advanceTurn(battleComponent)
                }
                return true
            }

            is CastSpellEvent -> {
                val battleEntity    = currentBattleEntity ?: return false
                val battleComponent = battleComponents.getOrNull(battleEntity) ?: return false
                val playerEntity    = currentPlayerEntity ?: return false
                if (battleComponent.phase != BattlePhase.PLAYER_TURN) return false

                val tree  = ABILITY_TREES[1] ?: return false
                val node  = tree.nodes.find { it.id == event.abilityId } ?: return false
                val cStat = statComponents.getOrNull(event.casterEntity) ?: return false

                cStat.currentMana = (cStat.currentMana - node.manaCost).coerceAtLeast(0f)
                fireHealthUpdate()

                val effectLine: String
                when (val effect = node.effect) {
                    is AbilityEffect.DamageEnemy -> {
                        val target  = getTargetEntity(event.targetIndex) ?: return false
                        val tStat   = statComponents.getOrNull(target) ?: return false
                        val dmg     = effect.amount.toFloat()
                        tStat.currentHealth = (tStat.currentHealth - dmg).coerceAtLeast(0f)
                        fireHealthUpdate()
                        val tOrigin = getEnemyOrigin(target)
                        gameStage.fire(FloatingTextEvent(
                            Vector2(tOrigin.x + savedEnemyImageWidth * 0.5f, tOrigin.y + savedEnemyImageHeight * 0.5f),
                            dmg.toInt().toString(), Fonts.DAMAGE
                        ))
                        imageComponents.getOrNull(target)?.image?.let { img ->
                            img.addAction(Actions.sequence(
                                Actions.run { img.useWhiteShader = true },
                                Actions.delay(FLASH_DURATION),
                                Actions.run { img.useWhiteShader = false }
                            ))
                        }
                        effectLine = "Dealt ${dmg.toInt()} damage!"
                    }
                    is AbilityEffect.HealSelf -> {
                        val heal = effect.amount.toFloat()
                        cStat.currentHealth = (cStat.currentHealth + heal).coerceAtMost(cStat.maxHealth)
                        fireHealthUpdate()
                        gameStage.fire(FloatingTextEvent(
                            Vector2(playerOriginX, playerOriginY), "+${heal.toInt()} HP", Fonts.DAMAGE
                        ))
                        imageComponents.getOrNull(playerEntity)?.image?.let { img ->
                            img.addAction(Actions.sequence(
                                Actions.run { img.color.set(Color.GREEN) },
                                Actions.delay(ITEM_FLASH_DURATION),
                                Actions.run { img.color.set(Color.WHITE) }
                            ))
                        }
                        effectLine = "Restored ${heal.toInt()} HP!"
                    }
                }

                gameStage.fire(BattleLogEvent("Player cast ${node.name}!\n$effectLine"))
                battleComponent.waitingForSpellDismiss = true
                transitionPhase(battleComponent, BattlePhase.RESOLVING)
                return true
            }

            is SpellCastDismissedEvent -> {
                val battleEntity    = currentBattleEntity ?: return false
                val battleComponent = battleComponents.getOrNull(battleEntity) ?: return false
                if (!battleComponent.waitingForSpellDismiss) return false
                battleComponent.waitingForSpellDismiss = false

                // Award kill rewards for any enemies that died from the spell
                val playerEntity = currentPlayerEntity
                if (playerEntity != null) {
                    allEnemyEntities.forEach { enemy ->
                        if ((statComponents.getOrNull(enemy)?.isDead ?: false) && enemy !in killedEnemiesRewarded) {
                            awardKillRewards(playerEntity, enemy)
                        }
                    }
                }
                advanceTurn(battleComponent)
                return true
            }

            is ItemUseFlashEvent -> {
                val targetEntity = playerFamily.firstOrNull() ?: return false
                val targetImg    = imageComponents.getOrNull(targetEntity)?.image ?: return false
                targetImg.addAction(Actions.sequence(
                    Actions.run { targetImg.color.set(event.flashColor) },
                    Actions.delay(ITEM_FLASH_DURATION),
                    Actions.run { targetImg.color.set(Color.WHITE) }
                ))
                return true
            }

            is BattleEndEvent -> {
                sequenceRunning = false
                currentBattleEntity?.let { e ->
                    battleComponents.getOrNull(e)?.let {
                        it.battleInProgress        = false
                        it.endDelayTimer           = -1f
                        it.waitingForActionDismiss = false
                        it.waitingForSpellDismiss  = false
                    }
                }
                currentBattleEntity  = null
                currentPlayerEntity  = null
                savedNonPlayerConfig = null
                hideSelectionIndicator()
                clearEscrow()
                clearSessionState()
                return true
            }

            is BattleMapChangeEvent -> {
                createBattleEnemies(event.map)
                return true
            }

            is MapChangeEvent -> {
                val portalLayer = event.map.layer("portals")
                portalLayer.objects.forEach { mapObject ->
                    val toMap    = mapObject.property("toMap", "")
                    val toPortal = mapObject.property("toPortal", -1)
                    if (toMap.isBlank()) return@forEach
                    if (toPortal == -1) gdxError("Portal " + mapObject.id + " does not have toPortal property.")
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

            is CombatSpeedChangedEvent -> {
                val battleEntity = currentBattleEntity ?: return false
                val battleComp   = battleComponents.getOrNull(battleEntity) ?: return false
                if (!battleComp.battleInProgress) return false
                sortTurnOrder(preserveActive = true)
                return true
            }

            is EnemySelectionModeStartedEvent -> {
                inSelectionMode = true
                autoAdvanceSelectedEnemy()
                showSelectionIndicator(selectedEnemyIndex)
                gameStage.fire(EnemySelectionIndexChangedEvent(selectedEnemyIndex))
                return true
            }

            is EnemySelectionModeEndedEvent -> {
                inSelectionMode = false
                hideSelectionIndicator()
                return true
            }

            is EnemySelectNextEvent -> {
                if (!inSelectionMode) return false
                cycleSelectionIndex(1)
                return true
            }

            is EnemySelectPrevEvent -> {
                if (!inSelectionMode) return false
                cycleSelectionIndex(-1)
                return true
            }

            else -> return false
        }
    }

    companion object {
        private const val END_DELAY_SECONDS   = 1.5f
        private const val SLIDE_DURATION      = 0.4f
        private const val FLASH_DURATION      = 0.1f
        private const val ITEM_FLASH_DURATION = 0.2f
        private const val HIT_FLASH_DELAY     = 0.25f
        private const val ATTACK_OFFSET       = 1.5f
        private const val INDICATOR_SIZE      = 0.4f    // world units
        private const val INDICATOR_PADDING   = 0.05f   // gap above enemy image top
    }
}
