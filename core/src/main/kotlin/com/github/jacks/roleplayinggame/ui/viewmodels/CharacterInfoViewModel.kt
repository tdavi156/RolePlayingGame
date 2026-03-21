package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.events.PartyUpdatedEvent
import com.github.jacks.roleplayinggame.events.RewardDismissedEvent
import com.github.jacks.roleplayinggame.saveManager.CharacterData
import com.github.jacks.roleplayinggame.systems.PartySystem
import com.github.jacks.roleplayinggame.systems.ResourceSystem
import com.github.quillraven.fleks.World

class CharacterInfoViewModel(
    private val world: World,
    stage: Stage,
) : PropertyChangeSource(), EventListener {

    private val partySystem: PartySystem = world.system()
    private val resourceSystem: ResourceSystem = world.system()

    // Left panel — ordered list of unlocked characters
    var partyList by propertyNotify(listOf<CharacterData>())
    var focusedIndex by propertyNotify(0)

    // Right panel — detail stats for the focused character
    var detailName        by propertyNotify("")
    var detailLevel       by propertyNotify(1)
    var detailExperience  by propertyNotify(0)
    var detailExpToNext   by propertyNotify(50)
    var detailCurrentHp   by propertyNotify(0f)
    var detailMaxHp       by propertyNotify(0f)
    var detailCurrentMana by propertyNotify(0f)
    var detailMaxMana     by propertyNotify(0f)
    var detailAttack      by propertyNotify(0f)
    var detailDefense     by propertyNotify(0f)
    var detailSpeed       by propertyNotify(0f)
    var detailGold        by propertyNotify(0)
    var activeOverworldId by propertyNotify(1)

    init {
        stage.addListener(this)
    }

    fun refresh() {
        partyList = partySystem.getUnlockedCharacters()
        activeOverworldId = partySystem.activeOverworldCharacterId
        // Clamp focused index to valid range
        if (partyList.isEmpty()) return
        focusedIndex = focusedIndex.coerceIn(0, partyList.lastIndex)
        refreshDetail()
        detailGold = resourceSystem.resources.gold
    }

    fun moveFocus(delta: Int) {
        if (partyList.isEmpty()) return
        focusedIndex = (focusedIndex + delta).coerceIn(0, partyList.lastIndex)
        refreshDetail()
    }

    private fun refreshDetail() {
        val char = partyList.getOrNull(focusedIndex) ?: return
        detailName        = char.characterName
        detailLevel       = char.currentLevel
        detailExperience  = char.currentEXP
        detailExpToNext   = char.experienceToNextLevel
        detailCurrentHp   = char.currentHealth
        detailMaxHp       = char.maxHealth
        detailCurrentMana = char.currentMana
        detailMaxMana     = char.maxMana
        detailAttack      = char.attackDamage
        detailDefense     = char.defense
        detailSpeed       = char.attackSpeed
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is PartyUpdatedEvent  -> { refresh(); return true }
            is RewardDismissedEvent -> { detailGold = resourceSystem.resources.gold; return true }
            else -> return false
        }
    }
}
