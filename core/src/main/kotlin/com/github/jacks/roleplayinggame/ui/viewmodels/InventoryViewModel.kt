package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.configurations.ConsumableItemData
import com.github.jacks.roleplayinggame.configurations.ConsumableStatType
import com.github.jacks.roleplayinggame.configurations.EquipmentItemData
import com.github.jacks.roleplayinggame.events.CombatInventoryClosedEvent
import com.github.jacks.roleplayinggame.events.CombatInventoryOpenEvent
import com.github.jacks.roleplayinggame.events.CombatItemUseDismissedEvent
import com.github.jacks.roleplayinggame.events.EquipItemEvent
import com.github.jacks.roleplayinggame.events.InventoryOpenEvent
import com.github.jacks.roleplayinggame.events.ItemUseFlashEvent
import com.github.jacks.roleplayinggame.events.UseConsumableEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.systems.InventorySystem
import com.github.jacks.roleplayinggame.systems.PartySystem
import com.github.jacks.roleplayinggame.systems.StatSystem
import com.github.quillraven.fleks.World

enum class InventoryTab { EQUIPMENT, CONSUMABLES, QUEST_ITEMS, ENCHANTMENTS }
enum class InventoryContext { LEFT, RIGHT }

sealed class PendingAction {
    object None : PendingAction()
    data class Equipment(val item: EquipmentItemData) : PendingAction()
    data class Consumable(val item: ConsumableItemData) : PendingAction()
}

data class CharacterDisplayInfo(val name: String, val hpPct: Float)

private enum class ResultType { NONE, WARNING, OVERWORLD_SUCCESS, COMBAT_SUCCESS }

class InventoryViewModel(
    private val world: World,
    private val gameStage: Stage,
) : PropertyChangeSource(), EventListener {

    val partyCharacters: List<CharacterDisplayInfo>
        get() {
            val partySystem = world.system<PartySystem>()
            return partySystem.getUnlockedCharacters().map { char ->
                val hpPct = if (char.maxHealth > 0f) char.currentHealth / char.maxHealth else 0f
                CharacterDisplayInfo(char.characterName, hpPct)
            }
        }

    var activeTab by propertyNotify(InventoryTab.EQUIPMENT)
    var focusedItemIndex by propertyNotify(0)
    var activeContext by propertyNotify(InventoryContext.RIGHT)
    var focusedCharacterIndex by propertyNotify(0)
    var pendingAction by propertyNotify<PendingAction>(PendingAction.None)
    var showingActionMenu by propertyNotify(false)
    var actionMenuFocusIndex by propertyNotify(0)  // 0 = primary action (Equip/Use), 1 = Cancel
    var isCombatMode by propertyNotify(false)
    // Non-empty while a result or warning message is shown in the left panel; empty = no message
    var resultMessage by propertyNotify("")

    private var currentResultType = ResultType.NONE

    val activeItemList: List<InventorySystem.InventoryEntry<*>>
        get() {
            val inv = world.system<InventorySystem>()
            return when (activeTab) {
                InventoryTab.EQUIPMENT    -> inv.equipment
                InventoryTab.CONSUMABLES  -> inv.consumables
                InventoryTab.QUEST_ITEMS  -> inv.questItems
                InventoryTab.ENCHANTMENTS -> inv.enchantments
            }
        }

    fun confirmPendingAction(characterIndex: Int) {
        when (val action = pendingAction) {
            is PendingAction.Equipment ->
                gameStage.fire(EquipItemEvent(action.item.id, characterIndex))
            is PendingAction.Consumable -> handleConsumableUse(action.item, characterIndex)
            PendingAction.None -> {}
        }
    }

    private fun handleConsumableUse(item: ConsumableItemData, characterIndex: Int) {
        val statSystem = world.system<StatSystem>()
        val statLabel = if (item.statType == ConsumableStatType.HEALTH) "HP" else "MP"
        val charName = partyCharacters.getOrNull(characterIndex)?.name ?: "Character"

        if (statSystem.isStatFull(characterIndex, item.statType)) {
            resultMessage = "$charName is already at full $statLabel!"
            currentResultType = ResultType.WARNING
            return
        }

        val recovery = statSystem.computeActualRecovery(characterIndex, item)
        gameStage.fire(UseConsumableEvent(item.id, characterIndex, isCombatItemUse = isCombatMode))

        // Reset action-selection state
        pendingAction = PendingAction.None
        showingActionMenu = false

        // Fire flash effect on the character entity
        gameStage.fire(ItemUseFlashEvent(characterIndex, item.flashColor))

        // Show result message
        resultMessage = "$charName recovered $recovery $statLabel!"
        currentResultType = if (isCombatMode) ResultType.COMBAT_SUCCESS else ResultType.OVERWORLD_SUCCESS
    }

    /** Called when the player presses Enter or clicks to dismiss the current result/warning message. */
    fun dismissResult() {
        val type = currentResultType
        resultMessage = ""
        currentResultType = ResultType.NONE

        when (type) {
            ResultType.WARNING -> {
                // Keep focus on character list so the player can pick a different character
            }
            ResultType.OVERWORLD_SUCCESS -> {
                pendingAction = PendingAction.None
                activeContext = InventoryContext.RIGHT
            }
            ResultType.COMBAT_SUCCESS -> {
                gameStage.fire(CombatItemUseDismissedEvent())
                // GameScreen closes the inventory overlay after this event
            }
            ResultType.NONE -> {}
        }
    }

    init {
        gameStage.addListener(this)
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is InventoryOpenEvent -> {
                isCombatMode = false
                activeTab = InventoryTab.EQUIPMENT
                focusedItemIndex = 0
                activeContext = InventoryContext.RIGHT
                showingActionMenu = false
                actionMenuFocusIndex = 0
                resultMessage = ""
                currentResultType = ResultType.NONE
                return true
            }
            is CombatInventoryOpenEvent -> {
                isCombatMode = true
                activeTab = InventoryTab.CONSUMABLES
                focusedItemIndex = 0
                focusedCharacterIndex = 0
                activeContext = InventoryContext.RIGHT
                showingActionMenu = false
                actionMenuFocusIndex = 0
                resultMessage = ""
                currentResultType = ResultType.NONE
                return true
            }
            is CombatInventoryClosedEvent -> {
                isCombatMode = false
                pendingAction = PendingAction.None
                showingActionMenu = false
                resultMessage = ""
                currentResultType = ResultType.NONE
                return true
            }
            is EquipItemEvent -> {
                pendingAction = PendingAction.None
                showingActionMenu = false
                actionMenuFocusIndex = 0
                activeContext = InventoryContext.RIGHT
                return true
            }
            else -> return false
        }
    }
}
