package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.AbilityComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.configurations.AbilityNode
import com.github.jacks.roleplayinggame.configurations.ABILITY_TREES
import com.github.jacks.roleplayinggame.events.AbilityPointsSaveEvent
import com.github.jacks.roleplayinggame.events.AbilityViewClosedEvent
import com.github.jacks.roleplayinggame.events.AbilityViewOpenEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
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

    private val playerFamily by lazy { world.family(allOf = arrayOf(PlayerComponent::class)) }
    private val statMapper: ComponentMapper<StatComponent> by lazy { world.mapper() }
    private val abilityMapper: ComponentMapper<AbilityComponent> by lazy { world.mapper() }

    // Observable properties
    var abilityPoints by propertyNotify(0)
    var activeCharacterIndex by propertyNotify(0)
    var nodes by propertyNotify(emptyList<AbilityNodeUiState>())
    var pendingUnlockIds by propertyNotify(emptySet<Int>())
    var hasUnsavedChanges by propertyNotify(false)
    var showCancelConfirm by propertyNotify(false)
    var showSaveConfirm by propertyNotify(false)

    private var playerEntity: Entity? = null

    init {
        gameStage.addListener(this)
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is AbilityViewOpenEvent -> {
                val entity = playerFamily.firstOrNull() ?: return false
                playerEntity = entity
                val stat = statMapper.getOrNull(entity) ?: return false

                abilityPoints = stat.abilityPoints
                pendingUnlockIds = emptySet()
                hasUnsavedChanges = false
                showCancelConfirm = false
                showSaveConfirm = false
                recomputeNodes(entity)
                return true
            }
            else -> return false
        }
    }

    // -- Node interaction methods --

    /** Add a node to pending unlocks. Only valid if the node is unlockable. */
    fun addPending(nodeId: Int) {
        val entity = playerEntity ?: return
        val nodeState = nodes.find { it.node.id == nodeId } ?: return
        if (!nodeState.isUnlockable) return
        if (abilityPoints <= 0) return

        val newPending = pendingUnlockIds + nodeId
        pendingUnlockIds = newPending
        abilityPoints -= nodeState.node.abilityPointCost
        hasUnsavedChanges = true
        recomputeNodes(entity)
    }

    /** Remove a node from pending unlocks. Only valid if the node is pending (not yet skilled). */
    fun removePending(nodeId: Int) {
        val entity = playerEntity ?: return
        val nodeState = nodes.find { it.node.id == nodeId } ?: return
        if (!nodeState.isPending) return

        // Cascade: remove any pending nodes that transitively require this node
        val toRemove = collectDependentPending(nodeId, pendingUnlockIds)
        val newPending = pendingUnlockIds - toRemove
        val refundedPoints = toRemove.sumOf { id ->
            nodes.find { it.node.id == id }?.node?.abilityPointCost ?: 0
        }
        pendingUnlockIds = newPending
        abilityPoints += refundedPoints
        hasUnsavedChanges = newPending.isNotEmpty()
        recomputeNodes(entity)
    }

    private fun collectDependentPending(rootId: Int, pending: Set<Int>): Set<Int> {
        val result = mutableSetOf(rootId)
        val tree = ABILITY_TREES[1] ?: return result
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
        val entity = playerEntity ?: return
        showSaveConfirm = false
        gameStage.fire(AbilityPointsSaveEvent(entity, pendingUnlockIds))
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

    private fun recomputeNodes(entity: Entity) {
        val abilityComp = abilityMapper.getOrNull(entity) ?: return
        val tree = ABILITY_TREES[1] ?: return
        val pending = pendingUnlockIds
        val pts = abilityPoints

        nodes = tree.nodes.map { node ->
            val isSkilled = abilityComp.isSkilled(node.id)
            val isPending = node.id in pending
            val prereqsMet = node.prerequisiteIds.all {
                abilityComp.isSkilled(it) || it in pending
            }
            val isUnlockable = !isSkilled && !isPending && prereqsMet && pts > 0
            val isLocked = !isSkilled && !isPending && !prereqsMet
            AbilityNodeUiState(node, isSkilled, isUnlockable, isPending, isLocked)
        }
    }
}
