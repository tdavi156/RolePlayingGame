package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.AbilityComponent
import com.github.jacks.roleplayinggame.components.AnimationComponent
import com.github.jacks.roleplayinggame.components.BattleAction
import com.github.jacks.roleplayinggame.components.BattlePhase
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.configurations.AbilityNode
import com.github.jacks.roleplayinggame.configurations.ABILITY_TREES
import com.github.jacks.roleplayinggame.configurations.CHARACTER_CONFIGS
import com.github.jacks.roleplayinggame.events.AbilitySkillChangedEvent
import com.github.jacks.roleplayinggame.events.BattleActionSelectedEvent
import com.github.jacks.roleplayinggame.events.BattleEndEvent
import com.github.jacks.roleplayinggame.events.BattleEvent
import com.github.jacks.roleplayinggame.events.BattleHealthUpdateEvent
import com.github.jacks.roleplayinggame.events.BattleLogDismissedEvent
import com.github.jacks.roleplayinggame.events.BattleLogEvent
import com.github.jacks.roleplayinggame.events.BattlePhaseChangedEvent
import com.github.jacks.roleplayinggame.events.BattleReadyEvent
import com.github.jacks.roleplayinggame.events.CastSpellEvent
import com.github.jacks.roleplayinggame.events.CombatInventoryOpenEvent
import com.github.jacks.roleplayinggame.events.EnemySelectionCancelledEvent
import com.github.jacks.roleplayinggame.events.EnemySelectionConfirmedEvent
import com.github.jacks.roleplayinggame.events.PlayerTurnStartedEvent
import com.github.jacks.roleplayinggame.events.EnemySelectionIndexChangedEvent
import com.github.jacks.roleplayinggame.events.EnemySelectionModeEndedEvent
import com.github.jacks.roleplayinggame.events.EnemySelectionModeStartedEvent
import com.github.jacks.roleplayinggame.events.SpellCastDismissedEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.saveManager.CharacterData
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

data class EnemyStatInfo(val name: String, val level: Int, val lifePct: Float, val manaPct: Float)

class BattleViewModel(
    private val world: World,
    private val gameStage: Stage,
) : PropertyChangeSource(), EventListener {

    private val statComponents: ComponentMapper<StatComponent>           = world.mapper()
    private val animationComponents: ComponentMapper<AnimationComponent> = world.mapper()
    private val abilityComponents: ComponentMapper<AbilityComponent>     = world.mapper()
    private val playerMapper: ComponentMapper<PlayerComponent>           = world.mapper()

    // Observable UI state
    var playerLife  by propertyNotify(1f)
    var playerMana  by propertyNotify(1f)
    var playerName  by propertyNotify("Player")
    var playerLevel by propertyNotify(1)
    var battlePhase by propertyNotify(BattlePhase.PLAYER_TURN)
    var battleLog   by propertyNotify("")
    var spellsButtonEnabled by propertyNotify(false)
    var availableSpells by propertyNotify(emptyList<AbilityNode>())
    var enemyStatInfos by propertyNotify(emptyList<EnemyStatInfo>())
    var enemySelectionActive by propertyNotify(false)

    // Non-observable: actual mana values for spell affordability checks
    var playerCurrentMana: Float = 0f
        private set
    var playerMaxMana: Float = 0f
        private set

    // Internal session state
    var currentEnemy: Entity? = null
        private set
    private var currentPlayerEntity: Entity? = null
    private val lastEnemyHealthPcts = mutableListOf<Float>()
    var selectedEnemyIndex = 0
        private set
    private var pendingBattleAction = BattleAction.NONE
    private var pendingAbilityId = -1

    init {
        gameStage.addListener(this)
    }

    // -- Action callbacks invoked by BattleView button clicks -----------------

    fun onAttack() {
        val numLiving = lastEnemyHealthPcts.count { it > 0f }
        if (numLiving <= 1) {
            val targetIndex = lastEnemyHealthPcts.indexOfFirst { it > 0f }.coerceAtLeast(0)
            gameStage.fire(BattleActionSelectedEvent(BattleAction.ATTACK, targetIndex))
        } else {
            pendingBattleAction = BattleAction.ATTACK
            pendingAbilityId = -1
            enemySelectionActive = true
            gameStage.fire(EnemySelectionModeStartedEvent())
        }
    }

    fun onFlee()         = gameStage.fire(BattleActionSelectedEvent(BattleAction.FLEE))
    fun onItems()        = gameStage.fire(CombatInventoryOpenEvent())
    fun onLogDismissed() = gameStage.fire(BattleLogDismissedEvent())

    fun onSpellCast(abilityId: Int) {
        val caster = currentPlayerEntity ?: return
        val numLiving = lastEnemyHealthPcts.count { it > 0f }
        if (numLiving <= 1) {
            val targetIndex = lastEnemyHealthPcts.indexOfFirst { it > 0f }.coerceAtLeast(0)
            gameStage.fire(CastSpellEvent(abilityId, caster, targetIndex))
        } else {
            pendingBattleAction = BattleAction.NONE
            pendingAbilityId = abilityId
            enemySelectionActive = true
            gameStage.fire(EnemySelectionModeStartedEvent())
        }
    }

    fun onSpellCastDismissed() = gameStage.fire(SpellCastDismissedEvent())

    fun onSelectionCancel() {
        if (!enemySelectionActive) return
        enemySelectionActive = false
        pendingBattleAction = BattleAction.NONE
        pendingAbilityId = -1
        gameStage.fire(EnemySelectionModeEndedEvent())
    }

    /** Refresh non-observable mana fields from the live player StatComponent. */
    fun refreshPlayerMana() {
        val playerEntity = currentPlayerEntity ?: return
        val stat = statComponents.getOrNull(playerEntity) ?: return
        playerCurrentMana = stat.stats.currentMana
        playerMaxMana = stat.stats.maxMana
    }

    // -- Event handling -------------------------------------------------------

    override fun handle(event: Event): Boolean {
        when (event) {
            is BattleEvent -> {
                currentEnemy = event.enemy
                battlePhase  = BattlePhase.PLAYER_TURN
                // Spells button and player stats will be set by PlayerTurnStartedEvent
                // when the first player turn begins (fired from BattleSystem.setPhaseForCurrentTurn)
            }
            is PlayerTurnStartedEvent -> {
                currentPlayerEntity = event.playerEntity
                val playerStat = statComponents.getOrNull(event.playerEntity)
                if (playerStat != null) {
                    playerLevel = (playerStat.stats as? CharacterData)?.currentLevel ?: 1
                    playerMana  = if (playerStat.stats.maxMana > 0f) playerStat.stats.currentMana / playerStat.stats.maxMana else 0f
                    playerLife  = if (playerStat.stats.maxHealth > 0f) playerStat.stats.currentHealth / playerStat.stats.maxHealth else 1f
                    playerCurrentMana = playerStat.stats.currentMana
                    playerMaxMana = playerStat.stats.maxMana
                }
                val abilityComp = abilityComponents.getOrNull(event.playerEntity)
                spellsButtonEnabled = abilityComp?.unlockedAbilityIds?.isNotEmpty() == true
                updateAvailableSpells(event.playerEntity)
                return true
            }
            is BattleReadyEvent -> {
                lastEnemyHealthPcts.clear()
                selectedEnemyIndex = 0
                enemyStatInfos = event.enemies.map { enemy ->
                    val stat = statComponents[enemy]
                    val anim = animationComponents[enemy]
                    val name = anim.model.name
                        .split("_")
                        .joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
                    val lifePct = if (stat.stats.maxHealth > 0f) stat.stats.currentHealth / stat.stats.maxHealth else 1f
                    val manaPct = if (stat.stats.maxMana > 0f) stat.stats.currentMana / stat.stats.maxMana else 0f
                    lastEnemyHealthPcts.add(lifePct)
                    EnemyStatInfo(name, 1, lifePct, manaPct)
                }
            }
            is BattlePhaseChangedEvent -> {
                battlePhase = event.phase
            }
            is BattleHealthUpdateEvent -> {
                playerLife = event.playerHealthPct
                event.enemyHealthPcts.forEachIndexed { i, pct ->
                    if (i < lastEnemyHealthPcts.size) lastEnemyHealthPcts[i] = pct
                }
                enemyStatInfos = enemyStatInfos.mapIndexed { i, info ->
                    val newPct = event.enemyHealthPcts.getOrElse(i) { info.lifePct }
                    info.copy(lifePct = newPct)
                }
                // Auto-advance selected index if the selected enemy just died
                if (enemySelectionActive
                    && selectedEnemyIndex < lastEnemyHealthPcts.size
                    && lastEnemyHealthPcts[selectedEnemyIndex] <= 0f) {
                    val nextLiving = lastEnemyHealthPcts.indexOfFirst { it > 0f }
                    if (nextLiving >= 0) selectedEnemyIndex = nextLiving
                }
            }
            is BattleLogEvent -> {
                battleLog = event.message
            }
            is BattleEndEvent -> {
                currentEnemy = null
                currentPlayerEntity = null
                lastEnemyHealthPcts.clear()
                enemyStatInfos = emptyList()
                enemySelectionActive = false
                selectedEnemyIndex = 0
                pendingBattleAction = BattleAction.NONE
                pendingAbilityId = -1
                battlePhase = BattlePhase.PLAYER_TURN
                spellsButtonEnabled = false
                availableSpells = emptyList()
            }
            is AbilitySkillChangedEvent -> {
                // Use the changed entity (may differ from current turn entity if abilities were unlocked mid-battle)
                val targetEntity = event.entity
                val abilityComp = abilityComponents.getOrNull(targetEntity) ?: return false
                // Only update UI if the changed entity is the currently acting player
                if (targetEntity == currentPlayerEntity) {
                    spellsButtonEnabled = abilityComp.unlockedAbilityIds.isNotEmpty()
                    updateAvailableSpells(targetEntity)
                }
                return true
            }
            is EnemySelectionIndexChangedEvent -> {
                selectedEnemyIndex = event.newIndex
                return true
            }
            is EnemySelectionConfirmedEvent -> {
                if (!enemySelectionActive) return false
                enemySelectionActive = false
                gameStage.fire(EnemySelectionModeEndedEvent())
                if (pendingBattleAction != BattleAction.NONE) {
                    gameStage.fire(BattleActionSelectedEvent(pendingBattleAction, selectedEnemyIndex))
                } else if (pendingAbilityId >= 0) {
                    val caster = currentPlayerEntity ?: return false
                    gameStage.fire(CastSpellEvent(pendingAbilityId, caster, selectedEnemyIndex))
                }
                pendingBattleAction = BattleAction.NONE
                pendingAbilityId = -1
                return true
            }
            is EnemySelectionCancelledEvent -> {
                if (!enemySelectionActive) return false
                enemySelectionActive = false
                pendingBattleAction = BattleAction.NONE
                pendingAbilityId = -1
                gameStage.fire(EnemySelectionModeEndedEvent())
                return true
            }
            else -> return false
        }
        return true
    }

    private fun updateAvailableSpells(playerEntity: Entity) {
        val abilityComp = abilityComponents.getOrNull(playerEntity) ?: run {
            availableSpells = emptyList()
            return
        }
        val charId = playerMapper.getOrNull(playerEntity)?.characterId ?: 1
        val treeId = CHARACTER_CONFIGS[charId]?.abilityTreeId ?: 1
        val tree = ABILITY_TREES[treeId] ?: run { availableSpells = emptyList(); return }
        availableSpells = tree.nodes
            .filter { it.id in abilityComp.unlockedAbilityIds }
            .sortedBy { it.id }
    }
}
