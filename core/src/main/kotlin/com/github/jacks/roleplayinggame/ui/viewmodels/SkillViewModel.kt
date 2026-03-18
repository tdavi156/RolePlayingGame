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
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.World

class SkillViewModel(
    private val world: World,
    private val gameStage: Stage,
) : PropertyChangeSource(), EventListener {

    private val playerFamily by lazy { world.family(allOf = arrayOf(PlayerComponent::class)) }
    private val statMapper: ComponentMapper<StatComponent> by lazy { world.mapper() }

    var availableSkillPoints  by propertyNotify(0)
    var pendingAttackPoints   by propertyNotify(0)
    var pendingDefensePoints  by propertyNotify(0)
    var hasUnsavedChanges     by propertyNotify(false)
    var showCancelConfirm     by propertyNotify(false)
    var showSaveConfirm       by propertyNotify(false)
    var investedAttackPoints  by propertyNotify(0)
    var investedDefensePoints by propertyNotify(0)
    var baseAttackDamage      by propertyNotify(0f)
    var baseDefense           by propertyNotify(0f)

    init {
        gameStage.addListener(this)
    }

    // ── Actions called by SkillView ─────────────────────────────────────────

    fun addAttackPoint() {
        if (availableSkillPoints <= 0) return
        availableSkillPoints--
        pendingAttackPoints++
        hasUnsavedChanges = true
    }

    fun removeAttackPoint() {
        if (pendingAttackPoints <= 0) return
        pendingAttackPoints--
        availableSkillPoints++
        hasUnsavedChanges = pendingAttackPoints > 0 || pendingDefensePoints > 0
    }

    fun addDefensePoint() {
        if (availableSkillPoints <= 0) return
        availableSkillPoints--
        pendingDefensePoints++
        hasUnsavedChanges = true
    }

    fun removeDefensePoint() {
        if (pendingDefensePoints <= 0) return
        pendingDefensePoints--
        availableSkillPoints++
        hasUnsavedChanges = pendingAttackPoints > 0 || pendingDefensePoints > 0
    }

    fun requestSave() {
        showSaveConfirm = true
    }

    fun confirmSave() {
        val entity = playerFamily.firstOrNull() ?: return
        gameStage.fire(SkillPointsSaveEvent(entity, pendingAttackPoints, pendingDefensePoints))
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
        availableSkillPoints += pendingAttackPoints + pendingDefensePoints
        pendingAttackPoints  = 0
        pendingDefensePoints = 0
        hasUnsavedChanges    = false
        showCancelConfirm    = false
        gameStage.fire(SkillViewClosedEvent())
    }

    fun dismissCancelConfirm() {
        showCancelConfirm = false
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private fun populateFromStat() {
        val entity = playerFamily.firstOrNull() ?: return
        val stat   = statMapper.getOrNull(entity) ?: return
        availableSkillPoints  = stat.skillPoints
        investedAttackPoints  = stat.skillPointsInvestedAttack
        investedDefensePoints = stat.skillPointsInvestedDefense
        baseAttackDamage      = stat.attackDamage
        baseDefense           = stat.defense
        pendingAttackPoints   = 0
        pendingDefensePoints  = 0
        hasUnsavedChanges     = false
        showSaveConfirm       = false
        showCancelConfirm     = false
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
