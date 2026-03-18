package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.configurations.ABILITY_TREES
import com.github.jacks.roleplayinggame.configurations.AbilityNode
import com.github.jacks.roleplayinggame.configurations.CHARACTER_CONFIGS
import com.github.jacks.roleplayinggame.events.AbilityPointsSaveEvent
import com.github.jacks.roleplayinggame.events.AbilityViewClosedEvent
import com.github.jacks.roleplayinggame.events.AbilityViewOpenEvent
import com.github.jacks.roleplayinggame.events.PartyUpdatedEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.systems.CharacterData
import com.github.jacks.roleplayinggame.systems.PartySystem
import com.github.quillraven.fleks.World

data class AbilityNodeUiState(
    val node: AbilityNode,
    val isSkilled: Boolean,
    val isUnlockable: Boolean,
    val isPending: Boolean,
    val isLocked: Boolean,
)

class AbilityViewModel(
    private val world: World,
    private val gameStage: Stage,
) : PropertyChangeSource(), EventListener {

    private fun partySystem() = world.system<PartySystem>()

    // Observable properties
    var abilityPoints by propertyNotify(0)
    var activeCharacterIndex by propertyNotify(0)
    var characterName by propertyNotify("Character 1")
    var nodes by propertyNotify(emptyList<AbilityNodeUiState>())
    var pendingUnlockIds by propertyNotify(emptySet<Int>())
    var hasUnsavedChanges by propertyNotify(false)
    var showCancelConfirm by propertyNotify(false)
    var showSaveConfirm by propertyNotify(false)

    private var focusedCharacterId: Int = 1

    init {
        gameStage.addListener(this)
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is AbilityViewOpenEvent -> {
                val party = partySystem().getUnlockedCharacters()
                if (party.isEmpty()) return false

                // Default to the active overworld character
                val activeId = partySystem().activeOverworldCharacterId
                val idx = party.indexOfFirst { it.characterId == activeId }.coerceAtLeast(0)
                activeCharacterIndex = idx
                loadCharacter(party[idx])
                return true
            }
            is PartyUpdatedEvent -> {
                // Keep focus on same character if still unlocked, else reset to 0
                val party = partySystem().getUnlockedCharacters()
                val idx = party.indexOfFirst { it.characterId == focusedCharacterId }
                activeCharacterIndex = if (idx >= 0) idx else 0
                if (party.isNotEmpty()) loadCharacter(party[activeCharacterIndex])
                return true
            }
            else -> return false
        }
    }

    private fun loadCharacter(charData: CharacterData) {
        focusedCharacterId = charData.characterId
        abilityPoints = charData.abilityPoints
        characterName = charData.characterName
        pendingUnlockIds = emptySet()
        hasUnsavedChanges = false
        showCancelConfirm = false
        showSaveConfirm = false
        recomputeNodes(charData)
    }

    // -- Character switching --

    fun switchCharacter(delta: Int) {
        val party = partySystem().getUnlockedCharacters()
        if (party.size <= 1) return
        val newIndex = (activeCharacterIndex + delta + party.size) % party.size
        activeCharacterIndex = newIndex
        loadCharacter(party[newIndex])
    }

    // -- Node interaction methods --

    fun addPending(nodeId: Int) {
        val nodeState = nodes.find { it.node.id == nodeId } ?: return
        if (!nodeState.isUnlockable) return
        if (abilityPoints <= 0) return

        val newPending = pendingUnlockIds + nodeId
        pendingUnlockIds = newPending
        abilityPoints -= nodeState.node.abilityPointCost
        hasUnsavedChanges = true
        recomputeNodes(partySystem().getCharacterData(focusedCharacterId))
    }

    fun removePending(nodeId: Int) {
        val nodeState = nodes.find { it.node.id == nodeId } ?: return
        if (!nodeState.isPending) return

        val toRemove = collectDependentPending(nodeId, pendingUnlockIds)
        val newPending = pendingUnlockIds - toRemove
        val refundedPoints = toRemove.sumOf { id ->
            nodes.find { it.node.id == id }?.node?.abilityPointCost ?: 0
        }
        pendingUnlockIds = newPending
        abilityPoints += refundedPoints
        hasUnsavedChanges = newPending.isNotEmpty()
        recomputeNodes(partySystem().getCharacterData(focusedCharacterId))
    }

    private fun collectDependentPending(rootId: Int, pending: Set<Int>): Set<Int> {
        val result = mutableSetOf(rootId)
        val charConfig = CHARACTER_CONFIGS[focusedCharacterId] ?: return result
        val tree = ABILITY_TREES[charConfig.abilityTreeId] ?: return result
        var changed = true
        while (changed) {
            changed = false
            tree.nodes.forEach { node ->
                if (node.id in pending && node.id !in result &&
                    node.prerequisiteIds.any { it in result }) {
                    result.add(node.id)
                    changed = true
                }
            }
        }
        return result
    }

    // -- Save / cancel flow --

    fun requestSave() {
        if (!hasUnsavedChanges) return
        showSaveConfirm = true
    }

    fun confirmSave() {
        showSaveConfirm = false
        gameStage.fire(AbilityPointsSaveEvent(focusedCharacterId, pendingUnlockIds))
        // AbilitySystem fires AbilityViewClosedEvent after saving
    }

    fun cancelSave() {
        showSaveConfirm = false
    }

    fun requestCancel() {
        if (hasUnsavedChanges) {
            showCancelConfirm = true
        } else {
            gameStage.fire(AbilityViewClosedEvent())
        }
    }

    fun confirmCancel() {
        showCancelConfirm = false
        gameStage.fire(AbilityViewClosedEvent())
    }

    fun dismissCancelConfirm() {
        showCancelConfirm = false
    }

    // -- Internal --

    private fun recomputeNodes(charData: CharacterData) {
        val charConfig = CHARACTER_CONFIGS[charData.characterId] ?: return
        val tree = ABILITY_TREES[charConfig.abilityTreeId] ?: return
        val pending = pendingUnlockIds
        val pts = abilityPoints

        nodes = tree.nodes.map { node ->
            val isSkilled = charData.unlockedAbilityIds.contains(node.id)
            val isPending = node.id in pending
            val prereqsMet = node.prerequisiteIds.all {
                charData.unlockedAbilityIds.contains(it) || it in pending
            }
            val isUnlockable = !isSkilled && !isPending && prereqsMet && pts > 0
            val isLocked = !isSkilled && !isPending && !prereqsMet
            AbilityNodeUiState(node, isSkilled, isUnlockable, isPending, isLocked)
        }
    }
}
