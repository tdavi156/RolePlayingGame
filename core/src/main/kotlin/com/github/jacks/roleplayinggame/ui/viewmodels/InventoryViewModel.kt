package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.configurations.ConsumableItemData
import com.github.jacks.roleplayinggame.configurations.EquipmentItemData
import com.github.jacks.roleplayinggame.events.EquipItemEvent
import com.github.jacks.roleplayinggame.events.InventoryOpenEvent
import com.github.jacks.roleplayinggame.events.UseConsumableEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.systems.InventorySystem
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.World

enum class InventoryTab { EQUIPMENT, CONSUMABLES, QUEST_ITEMS, ENCHANTMENTS }
enum class InventoryContext { LEFT, RIGHT }

sealed class PendingAction {
    object None : PendingAction()
    data class Equipment(val item: EquipmentItemData) : PendingAction()
    data class Consumable(val item: ConsumableItemData) : PendingAction()
}

data class CharacterDisplayInfo(val name: String, val hpPct: Float)

class InventoryViewModel(
    private val world: World,
    private val gameStage: Stage,
) : PropertyChangeSource(), EventListener {

    private val playerFamily by lazy { world.family(allOf = arrayOf(PlayerComponent::class)) }
    private val statMapper: ComponentMapper<StatComponent> by lazy { world.mapper() }

    val partyCharacters: List<CharacterDisplayInfo>
        get() {
            val entity = playerFamily.firstOrNull() ?: return emptyList()
            val stat = statMapper[entity]
            val hpPct = if (stat.maxHealth > 0f) stat.currentHealth / stat.maxHealth else 0f
            return listOf(CharacterDisplayInfo("Player", hpPct))
        }

    var activeTab by propertyNotify(InventoryTab.EQUIPMENT)
    var focusedItemIndex by propertyNotify(0)
    var activeContext by propertyNotify(InventoryContext.RIGHT)
    var focusedCharacterIndex by propertyNotify(0)
    var pendingAction by propertyNotify<PendingAction>(PendingAction.None)
    var showingActionMenu by propertyNotify(false)
    var actionMenuFocusIndex by propertyNotify(0)  // 0 = primary action (Equip/Use), 1 = Cancel

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
            is PendingAction.Consumable ->
                gameStage.fire(UseConsumableEvent(action.item.id, characterIndex))
            PendingAction.None -> {}
        }
    }

    init {
        gameStage.addListener(this)
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is InventoryOpenEvent -> {
                activeTab = InventoryTab.EQUIPMENT
                focusedItemIndex = 0
                activeContext = InventoryContext.RIGHT
                showingActionMenu = false
                actionMenuFocusIndex = 0
                return true
            }
            is EquipItemEvent, is UseConsumableEvent -> {
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
