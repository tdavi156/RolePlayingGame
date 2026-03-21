package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.events.SkillPointsChangedEvent
import com.github.jacks.roleplayinggame.events.SkillPointsSaveEvent
import com.github.jacks.roleplayinggame.events.SkillViewClosedEvent
import com.github.jacks.roleplayinggame.events.SkillViewOpenEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.saveManager.CharacterData
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.World

class SkillViewModel(
    private val world: World,
    private val gameStage: Stage,
) : PropertyChangeSource(), EventListener {

    private val playerFamily by lazy { world.family(allOf = arrayOf(PlayerComponent::class)) }
    private val statMapper: ComponentMapper<StatComponent> by lazy { world.mapper() }

    var availableSkillPoints  by propertyNotify(0)

    // Pending investment deltas (not yet saved)
    var pendingStamina        by propertyNotify(0)
    var pendingStrength       by propertyNotify(0)
    var pendingAgility        by propertyNotify(0)
    var pendingIntelligence   by propertyNotify(0)
    var pendingWisdom         by propertyNotify(0)

    // Current invested totals (read from CharacterData on open)
    var investedStamina       by propertyNotify(0)
    var investedStrength      by propertyNotify(0)
    var investedAgility       by propertyNotify(0)
    var investedIntelligence  by propertyNotify(0)
    var investedWisdom        by propertyNotify(0)

    var hasUnsavedChanges     by propertyNotify(false)
    var showCancelConfirm     by propertyNotify(false)
    var showSaveConfirm       by propertyNotify(false)

    init {
        gameStage.addListener(this)
    }

    // ── Actions called by SkillView ─────────────────────────────────────────

    fun addStamina()          = addPoint { pendingStamina++ }
    fun removeStamina()       = removePoint(pendingStamina) { pendingStamina-- }
    fun addStrength()         = addPoint { pendingStrength++ }
    fun removeStrength()      = removePoint(pendingStrength) { pendingStrength-- }
    fun addAgility()          = addPoint { pendingAgility++ }
    fun removeAgility()       = removePoint(pendingAgility) { pendingAgility-- }
    fun addIntelligence()     = addPoint { pendingIntelligence++ }
    fun removeIntelligence()  = removePoint(pendingIntelligence) { pendingIntelligence-- }
    fun addWisdom()           = addPoint { pendingWisdom++ }
    fun removeWisdom()        = removePoint(pendingWisdom) { pendingWisdom-- }

    private inline fun addPoint(block: () -> Unit) {
        if (availableSkillPoints <= 0) return
        availableSkillPoints--
        block()
        hasUnsavedChanges = true
    }

    private inline fun removePoint(pending: Int, block: () -> Unit) {
        if (pending <= 0) return
        block()
        availableSkillPoints++
        hasUnsavedChanges = totalPending() > 0
    }

    private fun totalPending() =
        pendingStamina + pendingStrength + pendingAgility + pendingIntelligence + pendingWisdom

    fun requestSave() {
        showSaveConfirm = true
    }

    fun confirmSave() {
        val entity = playerFamily.firstOrNull() ?: return
        gameStage.fire(SkillPointsSaveEvent(
            entity           = entity,
            pendingStamina   = pendingStamina,
            pendingStrength  = pendingStrength,
            pendingAgility   = pendingAgility,
            pendingIntelligence = pendingIntelligence,
            pendingWisdom    = pendingWisdom,
        ))
        showSaveConfirm = false
    }

    fun cancelSave() {
        showSaveConfirm = false
    }

    fun requestCancel() {
        if (hasUnsavedChanges) {
            showCancelConfirm = true
        } else {
            gameStage.fire(SkillViewClosedEvent())
        }
    }

    fun confirmCancel() {
        availableSkillPoints += totalPending()
        pendingStamina      = 0
        pendingStrength     = 0
        pendingAgility      = 0
        pendingIntelligence = 0
        pendingWisdom       = 0
        hasUnsavedChanges   = false
        showCancelConfirm   = false
        gameStage.fire(SkillViewClosedEvent())
    }

    fun dismissCancelConfirm() {
        showCancelConfirm = false
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private fun populateFromStat() {
        val entity   = playerFamily.firstOrNull() ?: return
        val stat     = statMapper.getOrNull(entity) ?: return
        val charData = stat.stats as? CharacterData ?: return

        availableSkillPoints = charData.currentSkillPoints
        investedStamina      = charData.stamina
        investedStrength     = charData.strength
        investedAgility      = charData.agility
        investedIntelligence = charData.intelligence
        investedWisdom       = charData.wisdom
        pendingStamina       = 0
        pendingStrength      = 0
        pendingAgility       = 0
        pendingIntelligence  = 0
        pendingWisdom        = 0
        hasUnsavedChanges    = false
        showSaveConfirm      = false
        showCancelConfirm    = false
    }

    // ── Event handling ───────────────────────────────────────────────────────

    override fun handle(event: Event): Boolean {
        when (event) {
            is SkillViewOpenEvent -> {
                populateFromStat()
                return true
            }
            is SkillPointsChangedEvent -> {
                populateFromStat()
                return true
            }
            else -> return false
        }
    }
}
