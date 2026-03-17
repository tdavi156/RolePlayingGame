package com.github.jacks.roleplayinggame.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys.*
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.MoveComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.events.GamePauseEvent
import com.github.jacks.roleplayinggame.events.GameResumeEvent
import com.github.jacks.roleplayinggame.events.InteractionEvent
import com.github.jacks.roleplayinggame.events.EquipItemEvent
import com.github.jacks.roleplayinggame.events.InventoryClosedEvent
import com.github.jacks.roleplayinggame.events.InventoryOpenEvent
import com.github.jacks.roleplayinggame.events.UseConsumableEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.configurations.ConsumableItemData
import com.github.jacks.roleplayinggame.configurations.EquipmentItemData
import com.github.jacks.roleplayinggame.ui.viewmodels.InventoryContext
import com.github.jacks.roleplayinggame.ui.viewmodels.InventoryTab
import com.github.jacks.roleplayinggame.ui.viewmodels.PendingAction
import com.github.jacks.roleplayinggame.events.ShopBuyConfirmedEvent
import com.github.jacks.roleplayinggame.events.ShopClosedEvent
import com.github.jacks.roleplayinggame.events.ShopSellConfirmedEvent
import com.github.jacks.roleplayinggame.ui.viewmodels.ShopMode
import com.github.jacks.roleplayinggame.ui.viewmodels.ShopTab
import com.github.jacks.roleplayinggame.ui.viewmodels.ShopViewModel
import com.github.jacks.roleplayinggame.ui.views.BackgroundView
import com.github.jacks.roleplayinggame.ui.views.CharacterInfoView
import com.github.jacks.roleplayinggame.ui.views.InventoryView
import com.github.jacks.roleplayinggame.ui.views.MapView
import com.github.jacks.roleplayinggame.ui.views.MenuView
import com.github.jacks.roleplayinggame.ui.views.QuestView
import com.github.jacks.roleplayinggame.ui.views.SettingsView
import com.github.jacks.roleplayinggame.ui.views.ShopView
import com.github.jacks.roleplayinggame.ui.views.SkillView
import com.github.jacks.roleplayinggame.input.ViewType.*
import com.github.jacks.roleplayinggame.ui.viewmodels.SettingsViewModel
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.World
import ktx.app.KtxInputAdapter
import ktx.log.logger
import ktx.math.vec2

enum class ViewType {
    NO_VIEW, CHARACTER, INVENTORY, SKILL, QUEST, MAP, MAIN_MENU, SETTINGS, SHOP
}

fun gdxInputProcessor(processor : InputProcessor) {
    val currentProcessor = Gdx.input.inputProcessor
    if (currentProcessor == null) {
        Gdx.input.inputProcessor = processor
    } else {
        if (currentProcessor is InputMultiplexer) {
            if (processor !in currentProcessor.processors) {
                currentProcessor.addProcessor(processor)
            }
        } else {
            Gdx.input.inputProcessor = InputMultiplexer(currentProcessor, processor)
        }
    }
}

class PlayerKeyboardInputProcessor(
    private val world: World,
    private val gameStage: Stage,
    private val uiStage: Stage,
    private val settingsViewModel: SettingsViewModel? = null,
    private val moveComponents: ComponentMapper<MoveComponent> = world.mapper(),
) : KtxInputAdapter {

    private var playerSin = 0f
    private var playerCos = 0f
    private var normalizedVector = vec2()
    private var playerDirection = TO
    private val playerEntities = world.family(allOf = arrayOf(PlayerComponent::class))
    private var pausedInventory = false
    private var paused = false

    init {
        gdxInputProcessor(this)
    }

    private fun updatePlayerMovement() {
        normalizedVector.set(playerCos, playerSin).nor()
        playerEntities.forEach { player ->
            with (moveComponents[player]) {
                cos = normalizedVector.x
                sin = normalizedVector.y
            }
        }
    }

    private fun updatePlayerDirection() {
        playerEntities.forEach { player ->
            with (moveComponents[player]) {
                if (direction != playerDirection) {
                    direction = playerDirection
                    directionChanged = true
                }
            }
        }
    }

    private fun Int.isMovementKey() : Boolean {
        return this == UP || this == DOWN || this == LEFT || this == RIGHT || this == W || this == A || this == S || this == D
    }

//    private fun Int.isMenuKey() : Boolean {
//        return this == UP || this == DOWN || this == LEFT || this == RIGHT || this == W || this == A || this == S || this == D
//    }

    override fun keyDown(keycode: Int): Boolean {
        // Shop navigation takes priority when shop is open
        if (getActiveView() == SHOP) {
            handleShopKeyDown(keycode)
            return true
        }

        // Settings navigation takes priority over all other key handling
        if (getActiveView() == SETTINGS) {
            val model = settingsViewModel ?: return false
            when (keycode) {
                UP -> model.moveFocusedRow(-1)
                DOWN -> model.moveFocusedRow(1)
                LEFT -> model.adjustCurrentValue(-10)
                RIGHT -> model.adjustCurrentValue(10)
                ENTER -> model.confirmCurrentRow()
                ESCAPE -> model.cancel()
            }
            return true
        }

        // Inventory navigation takes priority when inventory is open
        if (getActiveView() == INVENTORY) {
            handleInventoryKeyDown(keycode)
            return true
        }

        if (keycode.isMovementKey()) {
            when (keycode) {
                UP -> {
                    playerSin = 1f
                    playerDirection = AWAY
                }
                W -> {
                    playerSin = 1f
                    playerDirection = AWAY
                }
                DOWN -> {
                    playerSin = -1f
                    playerDirection = TO
                }
                S -> {
                    playerSin = -1f
                    playerDirection = TO
                }
                LEFT -> {
                    playerCos = -1f
                    playerDirection = SIDE
                }
                A -> {
                    playerCos = -1f
                    playerDirection = SIDE
                }
                RIGHT -> {
                    playerCos = 1f
                    playerDirection = SIDE
                }
                D -> {
                    playerCos = 1f
                    playerDirection = SIDE
                }
            }
            updatePlayerMovement()
            updatePlayerDirection()
            log.debug { "key pressed: $keycode, cos: $playerCos, sin: $playerSin, direction: $playerDirection" }
            return true
        } else {
            val backgroundView = uiStage.actors.filterIsInstance<BackgroundView>().first()
            when (keycode) {
                ESCAPE -> {
                    val menuView = uiStage.actors.filterIsInstance<MenuView>().first()
                    if (getActiveView() == NO_VIEW) {
                        gameStage.fire(GamePauseEvent())
                        backgroundView.isVisible = true
                        menuView.isVisible = true
                    } else if (getActiveView() == MAIN_MENU) {
                        backgroundView.isVisible = false
                        menuView.isVisible = false
                        gameStage.fire(GameResumeEvent())
                    } else {
                        clearActiveView()
                        gameStage.fire(GameResumeEvent())
                    }
                }
                E -> {
                    gameStage.fire(InteractionEvent())
                }
                C -> {
                    val characterInfoView = uiStage.actors.filterIsInstance<CharacterInfoView>().first()
                    if (getActiveView() == NO_VIEW) {
                        gameStage.fire(GamePauseEvent())
                        backgroundView.isVisible = true
                        characterInfoView.isVisible = true
                    } else if (getActiveView() == CHARACTER) {
                        backgroundView.isVisible = false
                        characterInfoView.isVisible = false
                        gameStage.fire(GameResumeEvent())
                    } else {
                        clearActiveView()
                        backgroundView.isVisible = true
                        characterInfoView.isVisible = true
                    }
                }
                L -> {
                    val skillView = uiStage.actors.filterIsInstance<SkillView>().first()
                    if (getActiveView() == NO_VIEW) {
                        gameStage.fire(GamePauseEvent())
                        backgroundView.isVisible = true
                        skillView.isVisible = true
                    } else if (getActiveView() == SKILL) {
                        backgroundView.isVisible = false
                        skillView.isVisible = false
                        gameStage.fire(GameResumeEvent())
                    } else {
                        clearActiveView()
                        backgroundView.isVisible = true
                        skillView.isVisible = true
                    }
                }
                M -> {
                    val mapView = uiStage.actors.filterIsInstance<MapView>().first()
                    if (getActiveView() == NO_VIEW) {
                        gameStage.fire(GamePauseEvent())
                        backgroundView.isVisible = true
                        mapView.isVisible = true
                    } else if (getActiveView() == MAP) {
                        backgroundView.isVisible = false
                        mapView.isVisible = false
                        gameStage.fire(GameResumeEvent())
                    } else {
                        clearActiveView()
                        backgroundView.isVisible = true
                        mapView.isVisible = true
                    }
                }
                I -> {
                    val inventoryView = uiStage.actors.filterIsInstance<InventoryView>().first()
                    if (getActiveView() == NO_VIEW) {
                        gameStage.fire(GamePauseEvent())
                        gameStage.fire(InventoryOpenEvent())
                        backgroundView.isVisible = true
                        inventoryView.isVisible = true
                    } else if (getActiveView() == INVENTORY) {
                        backgroundView.isVisible = false
                        inventoryView.isVisible = false
                        gameStage.fire(GameResumeEvent())
                    } else {
                        clearActiveView()
                        gameStage.fire(InventoryOpenEvent())
                        backgroundView.isVisible = true
                        inventoryView.isVisible = true
                    }
                }
                Q -> {
                    val questView = uiStage.actors.filterIsInstance<QuestView>().first()
                    if (getActiveView() == NO_VIEW) {
                        gameStage.fire(GamePauseEvent())
                        backgroundView.isVisible = true
                        questView.isVisible = true
                    } else if (getActiveView() == QUEST) {
                        backgroundView.isVisible = false
                        questView.isVisible = false
                        gameStage.fire(GameResumeEvent())
                    } else {
                        clearActiveView()
                        backgroundView.isVisible = true
                        questView.isVisible = true
                    }
                }
                X -> {
                    // toggle the character selection, and set the context to this view
                    // arrow keys and WASD now select which character to set, enter to set
                }
                P -> {
                    if (!pausedInventory) {
                        paused = !paused
                        gameStage.fire(if (paused) GamePauseEvent() else GameResumeEvent())
                    }
                }
            }
            return true
        }
    }

    override fun keyUp(keycode: Int): Boolean {
        if (getActiveView() == SETTINGS) return true

        if (keycode.isMovementKey()) {
            when (keycode) {
                UP -> {
                    if (Gdx.input.isKeyPressed(DOWN)) {
                        playerSin = -1f
                        playerDirection = TO
                    } else playerSin = 0f
                }
                W -> {
                    if (Gdx.input.isKeyPressed(S)) {
                        playerSin = -1f
                        playerDirection = TO
                    } else playerSin = 0f
                }
                DOWN -> {
                    if (Gdx.input.isKeyPressed(UP)) {
                        playerSin = 1f
                        playerDirection = AWAY
                    } else playerSin = 0f
                }
                S -> {
                    if (Gdx.input.isKeyPressed(W)) {
                        playerSin = 1f
                        playerDirection = AWAY
                    } else playerSin = 0f
                }
                LEFT -> {
                    if (Gdx.input.isKeyPressed(RIGHT)) {
                        playerCos = 1f
                        playerDirection = SIDE
                    } else playerCos = 0f
                }
                A -> {
                   if (Gdx.input.isKeyPressed(D)) {
                        playerCos = 1f
                        playerDirection = SIDE
                    } else playerCos = 0f
                }
                RIGHT -> {
                    if (Gdx.input.isKeyPressed(LEFT)) {
                        playerCos = -1f
                        playerDirection = SIDE
                    } else playerCos = 0f
                }
                D -> {
                    if (Gdx.input.isKeyPressed(A)) {
                        playerCos = -1f
                        playerDirection = SIDE
                    } else playerCos = 0f
                }
            }
            updatePlayerMovement()
            updatePlayerDirection()
            log.debug { "key released: $keycode, cos: $playerCos, sin: $playerSin, direction: $playerDirection" }
            return true
        }
        return false
    }

    private fun handleShopKeyDown(keycode: Int) {
        val shopView = uiStage.actors.filterIsInstance<ShopView>().firstOrNull() ?: return
        val model    = shopView.model
        val tabs     = ShopTab.entries

        if (model.pendingItemId != ShopViewModel.NO_PENDING) {
            // ── Quantity selector is active ──
            when (keycode) {
                LEFT, A -> {
                    val newQty = (model.pendingQuantity - 1).coerceAtLeast(1)
                    model.pendingQuantity = newQty
                }
                RIGHT, D -> {
                    val newQty = (model.pendingQuantity + 1).coerceAtMost(model.maxQuantity)
                    model.pendingQuantity = newQty
                }
                ENTER -> {
                    shopView.confirmPending()
                }
                ESCAPE -> {
                    // Cancel quantity selector — return to item list
                    model.pendingItemId   = ShopViewModel.NO_PENDING
                    model.pendingQuantity = 1
                }
            }
            return
        }

        // ── Item list is active ──
        val list = if (model.shopMode == ShopMode.BUY) model.buyItemList else model.sellItemList
        val maxIndex = (list.size - 1).coerceAtLeast(0)

        when (keycode) {
            LEFT, A -> {
                val newIdx = (model.activeTab.ordinal - 1 + tabs.size) % tabs.size
                model.activeTab       = tabs[newIdx]
                model.focusedItemIndex = 0
                model.pendingItemId   = ShopViewModel.NO_PENDING
            }
            RIGHT, D -> {
                val newIdx = (model.activeTab.ordinal + 1) % tabs.size
                model.activeTab       = tabs[newIdx]
                model.focusedItemIndex = 0
                model.pendingItemId   = ShopViewModel.NO_PENDING
            }
            UP, W -> {
                model.focusedItemIndex = (model.focusedItemIndex - 1).coerceAtLeast(0)
            }
            DOWN, S -> {
                model.focusedItemIndex = (model.focusedItemIndex + 1).coerceAtMost(maxIndex)
            }
            TAB -> {
                // Toggle BUY / SELL mode
                model.shopMode        = if (model.shopMode == ShopMode.BUY) ShopMode.SELL else ShopMode.BUY
                model.focusedItemIndex = 0
                model.pendingItemId   = ShopViewModel.NO_PENDING
            }
            ENTER -> {
                val safeIdx = model.focusedItemIndex.coerceIn(0, maxIndex)
                when (model.shopMode) {
                    ShopMode.BUY -> {
                        val row = model.buyItemList.getOrNull(safeIdx) ?: return
                        when {
                            row.id == ShopViewModel.NO_PENDING -> gameStage.fire(ShopClosedEvent())
                            !row.canAfford -> model.insufficientGoldVisible = true
                            else -> {
                                model.pendingItemId   = row.id
                                model.pendingQuantity = 1
                            }
                        }
                    }
                    ShopMode.SELL -> {
                        val row = model.sellItemList.getOrNull(safeIdx) ?: return
                        when {
                            row.id == ShopViewModel.NO_PENDING -> gameStage.fire(ShopClosedEvent())
                            !row.sellable -> { /* no-op */ }
                            else -> {
                                model.pendingItemId   = row.id
                                model.pendingQuantity = 1
                            }
                        }
                    }
                }
            }
            ESCAPE -> {
                gameStage.fire(ShopClosedEvent())
            }
        }
    }

    private fun getActiveView(): ViewType {
        val settingsView = uiStage.actors.filterIsInstance<SettingsView>().firstOrNull()
        val characterInfoView = uiStage.actors.filterIsInstance<CharacterInfoView>().first()
        val inventoryView = uiStage.actors.filterIsInstance<InventoryView>().first()
        val skillView = uiStage.actors.filterIsInstance<SkillView>().first()
        val questView = uiStage.actors.filterIsInstance<QuestView>().first()
        val mapView = uiStage.actors.filterIsInstance<MapView>().first()
        val menuView = uiStage.actors.filterIsInstance<MenuView>().first()
        val shopView = uiStage.actors.filterIsInstance<ShopView>().firstOrNull()

        if (shopView?.isVisible == true) { return SHOP }
        if (settingsView?.isVisible == true) { return SETTINGS }
        if (characterInfoView.isVisible) { return CHARACTER }
        if (inventoryView.isVisible) { return INVENTORY }
        if (skillView.isVisible) { return SKILL }
        if (questView.isVisible) { return QUEST }
        if (mapView.isVisible) { return MAP }
        if (menuView.isVisible) { return MAIN_MENU }
        return NO_VIEW
    }

    private fun clearActiveView() {
        uiStage.actors.filterIsInstance<BackgroundView>().first().isVisible = false
        uiStage.actors.filterIsInstance<SettingsView>().firstOrNull()?.isVisible = false
        uiStage.actors.filterIsInstance<CharacterInfoView>().first().isVisible = false
        uiStage.actors.filterIsInstance<InventoryView>().first().isVisible = false
        uiStage.actors.filterIsInstance<SkillView>().first().isVisible = false
        uiStage.actors.filterIsInstance<QuestView>().first().isVisible = false
        uiStage.actors.filterIsInstance<MapView>().first().isVisible = false
        uiStage.actors.filterIsInstance<MenuView>().first().isVisible = false
        uiStage.actors.filterIsInstance<ShopView>().firstOrNull()?.isVisible = false
    }

    private fun handleInventoryKeyDown(keycode: Int) {
        val model = uiStage.actors.filterIsInstance<InventoryView>().first().model
        val tabs = InventoryTab.entries
        when (model.activeContext) {
            InventoryContext.RIGHT -> if (model.showingActionMenu) {
                // Action menu is open — navigate between Equip/Use and Cancel
                when (keycode) {
                    LEFT, A  -> model.actionMenuFocusIndex = 0
                    RIGHT, D -> model.actionMenuFocusIndex = 1
                    ENTER -> {
                        if (model.actionMenuFocusIndex == 0) {
                            val items = model.activeItemList
                            if (items.isNotEmpty()) {
                                val entry = items[model.focusedItemIndex.coerceAtMost(items.size - 1)]
                                when (model.activeTab) {
                                    InventoryTab.EQUIPMENT   -> model.pendingAction = PendingAction.Equipment(entry.item as EquipmentItemData)
                                    InventoryTab.CONSUMABLES -> model.pendingAction = PendingAction.Consumable(entry.item as ConsumableItemData)
                                    else -> {}
                                }
                                model.showingActionMenu = false
                                model.activeContext = InventoryContext.LEFT
                            }
                        } else {
                            model.showingActionMenu = false
                        }
                    }
                    ESCAPE -> model.showingActionMenu = false
                }
            } else when (keycode) {
                LEFT, A -> {
                    val newIdx = (model.activeTab.ordinal - 1 + tabs.size) % tabs.size
                    model.activeTab = tabs[newIdx]
                    model.focusedItemIndex = 0
                    model.showingActionMenu = false
                }
                RIGHT, D -> {
                    val newIdx = (model.activeTab.ordinal + 1) % tabs.size
                    model.activeTab = tabs[newIdx]
                    model.focusedItemIndex = 0
                    model.showingActionMenu = false
                }
                UP, W -> {
                    model.focusedItemIndex = (model.focusedItemIndex - 1).coerceAtLeast(0)
                }
                DOWN, S -> {
                    val max = (model.activeItemList.size - 1).coerceAtLeast(0)
                    model.focusedItemIndex = (model.focusedItemIndex + 1).coerceAtMost(max)
                }
                ENTER -> {
                    val items = model.activeItemList
                    if (items.isNotEmpty() &&
                        (model.activeTab == InventoryTab.EQUIPMENT || model.activeTab == InventoryTab.CONSUMABLES)) {
                        model.actionMenuFocusIndex = 0
                        model.showingActionMenu = true
                    }
                }
                ESCAPE -> {
                    val backgroundView = uiStage.actors.filterIsInstance<BackgroundView>().first()
                    backgroundView.isVisible = false
                    uiStage.actors.filterIsInstance<InventoryView>().first().isVisible = false
                    gameStage.fire(InventoryClosedEvent())
                    gameStage.fire(GameResumeEvent())
                }
            }
            InventoryContext.LEFT -> when (keycode) {
                UP, W -> {
                    model.focusedCharacterIndex = (model.focusedCharacterIndex - 1).coerceAtLeast(0)
                }
                DOWN, S -> {
                    val max = (model.partyCharacters.size - 1).coerceAtLeast(0)
                    model.focusedCharacterIndex = (model.focusedCharacterIndex + 1).coerceAtMost(max)
                }
                ENTER -> {
                    when (val action = model.pendingAction) {
                        is PendingAction.Equipment ->
                            gameStage.fire(EquipItemEvent(action.item.id, model.focusedCharacterIndex))
                        is PendingAction.Consumable ->
                            gameStage.fire(UseConsumableEvent(action.item.id, model.focusedCharacterIndex))
                        PendingAction.None -> {}
                    }
                }
                ESCAPE, B -> {
                    model.pendingAction = PendingAction.None
                    model.activeContext = InventoryContext.RIGHT
                }
            }
        }
    }

    companion object {
        private val log = logger<PlayerKeyboardInputProcessor>()
        const val TO = "to"
        const val AWAY = "away"
        const val SIDE = "side"
    }
}
