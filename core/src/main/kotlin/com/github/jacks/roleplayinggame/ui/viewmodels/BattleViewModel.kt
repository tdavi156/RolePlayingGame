package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.AbilityComponent
import com.github.jacks.roleplayinggame.components.AnimationComponent
import com.github.jacks.roleplayinggame.components.BattleAction
import com.github.jacks.roleplayinggame.components.BattlePhase
import com.github.jacks.roleplayinggame.components.LifeComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.configurations.AbilityNode
import com.github.jacks.roleplayinggame.configurations.ABILITY_TREES
import com.github.jacks.roleplayinggame.events.AbilitySkillChangedEvent
import com.github.jacks.roleplayinggame.events.BattleActionSelectedEvent
import com.github.jacks.roleplayinggame.events.BattleEndEvent
import com.github.jacks.roleplayinggame.events.BattleEvent
import com.github.jacks.roleplayinggame.events.BattleHealthUpdateEvent
import com.github.jacks.roleplayinggame.events.BattleLogDismissedEvent
import com.github.jacks.roleplayinggame.events.BattleLogEvent
import com.github.jacks.roleplayinggame.events.BattlePhaseChangedEvent
import com.github.jacks.roleplayinggame.events.CastSpellEvent
import com.github.jacks.roleplayinggame.events.CombatInventoryOpenEvent
import com.github.jacks.roleplayinggame.events.SpellCastDismissedEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

class BattleViewModel(
    private val world: World,
    private val gameStage: Stage,
) : PropertyChangeSource(), EventListener {

    private val playerComponents: ComponentMapper<PlayerComponent> = world.mapper()
    private val lifeComponents: ComponentMapper<LifeComponent>     = world.mapper()
    private val statComponents: ComponentMapper<StatComponent>     = world.mapper()
    private val animationComponents: ComponentMapper<AnimationComponent> = world.mapper()
    private val abilityComponents: ComponentMapper<AbilityComponent> = world.mapper()

    // Observable UI state
    var playerLife  by propertyNotify(1f)
    var enemyLife   by propertyNotify(1f)
    var playerMana  by propertyNotify(1f)
    var enemyMana   by propertyNotify(1f)
    var playerName  by propertyNotify("Player")
    var enemyName   by propertyNotify("Enemy")
    var playerLevel by propertyNotify(1)
    var enemyLevel  by propertyNotify(1)
    var battlePhase by propertyNotify(BattlePhase.PLAYER_TURN)
    var battleLog   by propertyNotify("")
    var spellsButtonEnabled by propertyNotify(false)
    var availableSpells by propertyNotify(emptyList<AbilityNode>())

    // Non-observable: actual mana values for spell affordability checks
    var playerCurrentMana: Float = 0f
        private set
    var playerMaxMana: Float = 0f
        private set

    var currentEnemy: Entity? = null
        private set
    private var currentPlayerEntity: Entity? = null

    init {
        gameStage.addListener(this)
    }

    // -- Action callbacks invoked by BattleView button clicks -----------------

    fun onAttack()       = gameStage.fire(BattleActionSelectedEvent(BattleAction.ATTACK))
    fun onFlee()         = gameStage.fire(BattleActionSelectedEvent(BattleAction.FLEE))
    fun onItems()        = gameStage.fire(CombatInventoryOpenEvent())
    fun onLogDismissed() = gameStage.fire(BattleLogDismissedEvent())
    fun onSpellCast(abilityId: Int) {
        val caster = currentPlayerEntity ?: return
        gameStage.fire(CastSpellEvent(abilityId, caster))
    }
    fun onSpellCastDismissed() = gameStage.fire(SpellCastDismissedEvent())

    /** Refresh non-observable mana fields from the live player StatComponent. */
    fun refreshPlayerMana() {
        val playerEntity = currentPlayerEntity ?: return
        val stat = statComponents.getOrNull(playerEntity) ?: return
        playerCurrentMana = stat.currentMana
        playerMaxMana = stat.maxMana
    }

    // -- Event handling -------------------------------------------------------

    override fun handle(event: Event): Boolean {
        when (event) {
            is BattleEvent -> {
                currentEnemy = event.enemy
                battlePhase  = BattlePhase.PLAYER_TURN   // show action menu immediately

                // Populate enemy info
                val enemyStat = statComponents[event.enemy]
                val enemyAnim = animationComponents[event.enemy]
                enemyName  = enemyAnim.model.name
                    .split("_")
                    .joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
                enemyLevel = enemyStat.level
                enemyMana  = if (enemyStat.maxMana > 0f) enemyStat.currentMana / enemyStat.maxMana else 0f
                enemyLife  = if (enemyStat.maxHealth > 0f) enemyStat.currentHealth / enemyStat.maxHealth else 1f

                // Populate player info
                world.family(allOf = arrayOf(PlayerComponent::class)).forEach { playerEntity ->
                    currentPlayerEntity = playerEntity
                    val playerStat = statComponents[playerEntity]
                    playerLevel = playerStat.level
                    playerMana  = if (playerStat.maxMana > 0f) playerStat.currentMana / playerStat.maxMana else 0f
                    playerLife  = if (playerStat.maxHealth > 0f) playerStat.currentHealth / playerStat.maxHealth else 1f
                    playerCurrentMana = playerStat.currentMana
                    playerMaxMana = playerStat.maxMana

                    // Check for unlocked abilities to enable/disable Spells button
                    val abilityComp = abilityComponents.getOrNull(playerEntity)
                    spellsButtonEnabled = abilityComp?.unlockedAbilityIds?.isNotEmpty() == true
                    updateAvailableSpells(playerEntity)
                }
            }
            is BattlePhaseChangedEvent -> {
                battlePhase = event.phase
            }
            is BattleHealthUpdateEvent -> {
                playerLife = event.playerHealthPct
                enemyLife  = event.enemyHealthPct
            }
            is BattleLogEvent -> {
                battleLog = event.message
            }
            is BattleEndEvent -> {
                currentEnemy = null
                currentPlayerEntity = null
                battlePhase  = BattlePhase.PLAYER_TURN   // reset for next battle
                spellsButtonEnabled = false
                availableSpells = emptyList()
            }
            is AbilitySkillChangedEvent -> {
                val playerEntity = currentPlayerEntity ?: return false
                val abilityComp = abilityComponents.getOrNull(playerEntity) ?: return false
                spellsButtonEnabled = abilityComp.unlockedAbilityIds.isNotEmpty()
                updateAvailableSpells(playerEntity)
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
        val tree = ABILITY_TREES[1] ?: run { availableSpells = emptyList(); return }
        availableSpells = tree.nodes
            .filter { it.id in abilityComp.unlockedAbilityIds }
            .sortedBy { it.id }
    }
}
