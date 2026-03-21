package com.github.jacks.roleplayinggame.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys.*
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.MoveComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.events.EnemySelectionCancelledEvent
import com.github.jacks.roleplayinggame.events.EnemySelectionConfirmedEvent
import com.github.jacks.roleplayinggame.events.EnemySelectionModeEndedEvent
import com.github.jacks.roleplayinggame.events.EnemySelectionModeStartedEvent
import com.github.jacks.roleplayinggame.events.EnemySelectNextEvent
import com.github.jacks.roleplayinggame.events.EnemySelectPrevEvent
import com.github.jacks.roleplayinggame.events.GamePauseEvent
import com.github.jacks.roleplayinggame.events.GameResumeEvent
import com.github.jacks.roleplayinggame.events.InteractionEvent
import com.github.jacks.roleplayinggame.events.EquipItemEvent
import com.github.jacks.roleplayinggame.events.CombatInventoryClosedEvent
import com.github.jacks.roleplayinggame.events.InventoryClosedEvent
import com.github.jacks.roleplayinggame.events.InventoryOpenEvent
import com.github.jacks.roleplayinggame.events.UseConsumableEvent
import com.github.jacks.roleplayinggame.events.SwitchActiveCharacterEvent
import com.github.jacks.roleplayinggame.events.SkillViewOpenEvent
import com.github.jacks.roleplayinggame.events.AbilityViewOpenEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.configurations.ConsumableItemData
import com.github.jacks.roleplayinggame.configurations.EquipmentItemData
import com.github.jacks.roleplayinggame.ui.viewmodels.InventoryContext
import com.github.jacks.roleplayinggame.ui.viewmodels.InventoryTab
import com.github.jacks.roleplayinggame.ui.viewmodels.PendingAction
import com.github.jacks.roleplayinggame.events.ShopBuyConfirmedEvent
import com.github.jacks.roleplayinggame.events.ShopClosedEvent
import com.github.jacks.roleplayinggame.events.ShopSellConfirmedEvent
import com.github.jacks.roleplayinggame.ui.viewmodels.AbilityViewModel
import com.github.jacks.roleplayinggame.ui.viewmodels.CharacterInfoViewModel
import com.github.jacks.roleplayinggame.ui.viewmodels.ShopMode
import com.github.jacks.roleplayinggame.ui.viewmodels.ShopTab
import com.github.jacks.roleplayinggame.ui.viewmodels.ShopViewModel
import com.github.jacks.roleplayinggame.ui.viewmodels.SettingsViewModel
import com.github.jacks.roleplayinggame.ui.viewmodels.SkillViewModel
import com.github.jacks.roleplayinggame.ui.views.AbilityView
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
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.World
import ktx.app.KtxInputAdapter
import ktx.log.logger
import ktx.math.vec2

enum class ViewType {
    NO_VIEW, CHARACTER, INVENTORY, SKILL, QUEST, MAP, MAIN_MENU, SETTINGS, SHOP, ABILITY
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
    private val characterInfoViewModel: CharacterInfoViewModel? = null,
    private val skillViewModel: SkillViewModel? = null,
    private val abilityViewModel: AbilityViewModel? = null,
    private val moveComponents: ComponentMapper<MoveComponent> = world.mapper(),
) : KtxInputAdapter, EventListener {

    private var playerSin = 0f
    private var playerCos = 0f
    private var normalizedVector = vec2()
    private var playerDirection = TO
    private val playerEntities = world.family(allOf = arrayOf(PlayerComponent::class))
    private var inBattleSelectionMode = false

    init {
        gdxInputProcessor(this)
        gameStage.addListener(this)
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is EnemySelectionModeStartedEvent -> { inBattleSelectionMode = true; return true }
            is EnemySelectionModeEndedEvent   -> { inBattleSelectionMode = false; return true }
        }
        return false
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

    // ──────────────────────────────────────────────────────────────────────────
    // View management helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns the Actor for the given ViewType, or null if not found.
     */
    private fun getViewActor(viewType: ViewType): Actor? = when (viewType) {
        CHARACTER -> uiStage.actors.filterIsInstance<CharacterInfoView>().firstOrNull()
        SKILL     -> uiStage.actors.filterIsInstance<SkillView>().firstOrNull()
        ABILITY   -> uiStage.actors.filterIsInstance<AbilityView>().firstOrNull()
        MAP       -> uiStage.actors.filterIsInstance<MapView>().firstOrNull()
        INVENTORY -> uiStage.actors.filterIsInstance<InventoryView>().firstOrNull()
        QUEST     -> uiStage.actors.filterIsInstance<QuestView>().firstOrNull()
        SETTINGS  -> uiStage.actors.filterIsInstance<SettingsView>().firstOrNull()
        MAIN_MENU -> uiStage.actors.filterIsInstance<MenuView>().firstOrNull()
        else      -> null
    }

    /**
     * Shows the actor for [viewType] and fires its open event if applicable.
     */
    private fun openViewActor(viewType: ViewType) {
        getViewActor(viewType)?.isVisible = true
        when (viewType) {
            SKILL     -> gameStage.fire(SkillViewOpenEvent())
            ABILITY   -> gameStage.fire(AbilityViewOpenEvent())
            INVENTORY -> gameStage.fire(InventoryOpenEvent())
            else      -> {}
        }
    }

    /**
     * Force-hides the current view during a view switch (no close events / no GameResumeEvent).
     * SETTINGS requires cancel() for ViewModel cleanup; MenuView handles hiding settingsView via
     * SettingsClosedEvent on uiStage.
     */
    private fun forceHideCurrentView(viewType: ViewType) {
        when (viewType) {
            SETTINGS -> settingsViewModel?.cancel()  // SettingsClosedEvent on uiStage → MenuView hides settingsView
            else     -> getViewActor(viewType)?.isVisible = false
        }
    }

    /**
     * Gracefully closes the current view in response to pressing its own toggle key or ESCAPE.
     * - SKILL / ABILITY: delegate to ViewModel (may show confirm dialog).
     * - SETTINGS: close via ViewModel + fix up background/resume since SettingsClosedEvent
     *   fires on uiStage (not gameStage), so GameScreen never receives it.
     * - Everything else: direct hide + GameResumeEvent.
     */
    private fun closeViewGracefully(viewType: ViewType) {
        val bg = uiStage.actors.filterIsInstance<BackgroundView>().first()
        when (viewType) {
            SKILL   -> skillViewModel?.requestCancel()   // → SkillViewClosedEvent → GameScreen hides + resumes
            ABILITY -> abilityViewModel?.requestCancel() // → AbilityViewClosedEvent → GameScreen hides + resumes
            SETTINGS -> {
                // SettingsClosedEvent fires on uiStage → MenuView hides settingsView (its own logic).
                // If the menu was NOT visible (settings opened from overworld), we also need to
                // hide the background and resume the game ourselves.
                val menuVisible = uiStage.actors.filterIsInstance<MenuView>().firstOrNull()?.isVisible == true
                settingsViewModel?.cancel()
                if (!menuVisible) {
                    bg.isVisible = false
                    gameStage.fire(GameResumeEvent())
                }
            }
            else -> {
                bg.isVisible = false
                getViewActor(viewType)?.isVisible = false
                if (viewType == INVENTORY) gameStage.fire(InventoryClosedEvent())
                gameStage.fire(GameResumeEvent())
            }
        }
    }

    /**
     * Handles a view toggle key (C/K/L/M/I/J/O) uniformly:
     *  - NO_VIEW      → pause + open target
     *  - Same view    → close gracefully
     *  - Other view   → force-hide current + open target (game stays paused)
     * Returns true if the key was a view toggle key (and was consumed), false otherwise.
     */
    private fun handleViewToggle(keycode: Int): Boolean {
        val targetType = when (keycode) {
            C -> CHARACTER; K -> SKILL;    L -> ABILITY
            M -> MAP;       I -> INVENTORY; J -> QUEST; O -> SETTINGS
            else -> return false
        }

        val currentType = getActiveView()
        val bg = uiStage.actors.filterIsInstance<BackgroundView>().first()

        when {
            currentType == NO_VIEW -> {
                gameStage.fire(GamePauseEvent())
                bg.isVisible = true
                openViewActor(targetType)
            }
            currentType == targetType -> {
                closeViewGracefully(targetType)
            }
            else -> {
                // Switching from one open view to another — force-close current, open target.
                // Game stays paused; background stays visible.
                forceHideCurrentView(currentType)
                bg.isVisible = true
                openViewActor(targetType)
            }
        }
        return true
    }

    /**
     * Unified main-menu toggle used only for the ESCAPE key.
     * - NO_VIEW   → pause game, show background + menu
     * - MAIN_MENU → hide background + menu, resume game
     * (All other views handle their own ESCAPE in their priority blocks.)
     */
    private fun toggleView(viewType: ViewType, view: Actor, onOpen: () -> Unit = {}) {
        val bg = uiStage.actors.filterIsInstance<BackgroundView>().first()
        when {
            getActiveView() == NO_VIEW -> {
                gameStage.fire(GamePauseEvent())
                bg.isVisible = true
                view.isVisible = true
                onOpen()
            }
            getActiveView() == viewType -> {
                bg.isVisible = false
                view.isVisible = false
                gameStage.fire(GameResumeEvent())
            }
            else -> {
                clearActiveView()
                bg.isVisible = true
                view.isVisible = true
                onOpen()
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // keyDown
    // ──────────────────────────────────────────────────────────────────────────

    override fun keyDown(keycode: Int): Boolean {
        // ── 1. Enemy selection mode (multi-enemy targeting) ──
        if (inBattleSelectionMode) {
            when (keycode) {
                RIGHT, DOWN, D, S -> gameStage.fire(EnemySelectNextEvent())
                LEFT,  UP,   A, W -> gameStage.fire(EnemySelectPrevEvent())
                ENTER              -> gameStage.fire(EnemySelectionConfirmedEvent())
                ESCAPE             -> gameStage.fire(EnemySelectionCancelledEvent())
            }
            return true
        }

        // ── 2. Shop (fully modal — no view switching allowed from shop) ──
        if (getActiveView() == SHOP) {
            handleShopKeyDown(keycode)
            return true
        }

        // ── 3. View toggle keys (C/K/L/M/I/J/O) — work from any non-shop context ──
        //    Handles open, close-same, and switch-between-views uniformly.
        if (handleViewToggle(keycode)) return true

        // ── 4. Settings navigation ──
        if (getActiveView() == SETTINGS) {
            val model = settingsViewModel ?: return false
            when (keycode) {
                UP    -> model.moveFocusedRow(-1)
                DOWN  -> model.moveFocusedRow(1)
                LEFT  -> model.adjustCurrentValue(-10)
                RIGHT -> model.adjustCurrentValue(10)
                ENTER -> model.confirmCurrentRow()
                ESCAPE -> {
                    // SettingsClosedEvent fires on uiStage → MenuView hides settingsView.
                    // If the menu was not visible we also need to hide bg and resume the game.
                    val menuVisible = uiStage.actors.filterIsInstance<MenuView>()
                        .firstOrNull()?.isVisible == true
                    model.cancel()
                    if (!menuVisible) {
                        uiStage.actors.filterIsInstance<BackgroundView>().first().isVisible = false
                        gameStage.fire(GameResumeEvent())
                    }
                }
            }
            return true
        }

        // ── 5. Character info navigation ──
        if (getActiveView() == CHARACTER) {
            val model = characterInfoViewModel ?: return true
            when (keycode) {
                UP, W   -> model.moveFocus(-1)
                DOWN, S -> model.moveFocus(1)
                ESCAPE  -> {
                    uiStage.actors.filterIsInstance<BackgroundView>().first().isVisible = false
                    uiStage.actors.filterIsInstance<CharacterInfoView>().first().isVisible = false
                    gameStage.fire(GameResumeEvent())
                }
            }
            return true
        }

        // ── 6. Skill view (no keyboard nav — just block movement and handle ESCAPE) ──
        if (getActiveView() == SKILL) {
            val vm = skillViewModel ?: return true
            if (keycode == ESCAPE) vm.requestCancel()
            return true
        }

        // ── 7. Ability view ──
        if (getActiveView() == ABILITY) {
            val vm = abilityViewModel ?: return true
            when (keycode) {
                LEFT, A  -> vm.switchCharacter(-1)
                RIGHT, D -> vm.switchCharacter(1)
                ESCAPE   -> vm.requestCancel()
            }
            return true
        }

        // ── 8. Map view (block all — ESCAPE closes) ──
        if (getActiveView() == MAP) {
            if (keycode == ESCAPE) {
                uiStage.actors.filterIsInstance<BackgroundView>().first().isVisible = false
                uiStage.actors.filterIsInstance<MapView>().first().isVisible = false
                gameStage.fire(GameResumeEvent())
            }
            return true
        }

        // ── 9. Quest view ──
        if (getActiveView() == QUEST) {
            handleQuestKeyDown(keycode)
            return true
        }

        // ── 10. Inventory ──
        if (getActiveView() == INVENTORY) {
            handleInventoryKeyDown(keycode)
            return true
        }

        // ── 11. Overworld movement (only reached when no view is open) ──
        if (keycode.isMovementKey()) {
            when (keycode) {
                UP, W    -> { playerSin = 1f;  playerDirection = AWAY }
                DOWN, S  -> { playerSin = -1f; playerDirection = TO   }
                LEFT, A  -> { playerCos = -1f; playerDirection = SIDE }
                RIGHT, D -> { playerCos = 1f;  playerDirection = SIDE }
            }
            updatePlayerMovement()
            updatePlayerDirection()
            log.debug { "key pressed: $keycode, cos: $playerCos, sin: $playerSin, direction: $playerDirection" }
            return true
        }

        // ── 12. Alt+1–6: switch active overworld character ──
        if (Gdx.input.isKeyPressed(ALT_LEFT) || Gdx.input.isKeyPressed(ALT_RIGHT)) {
            val charId = when (keycode) {
                NUM_1 -> 1; NUM_2 -> 2; NUM_3 -> 3
                NUM_4 -> 4; NUM_5 -> 5; NUM_6 -> 6
                else  -> -1
            }
            if (charId > 0) {
                gameStage.fire(SwitchActiveCharacterEvent(charId))
                return true
            }
        }

        // ── 13. ESCAPE (menu toggle) + E (interaction) ──
        when (keycode) {
            ESCAPE -> {
                if (getActiveView() == NO_VIEW || getActiveView() == MAIN_MENU) {
                    toggleView(MAIN_MENU, uiStage.actors.filterIsInstance<MenuView>().first())
                }
                // All other views handle their own ESCAPE in their priority blocks above.
            }
            E -> gameStage.fire(InteractionEvent())
        }
        return true
    }

    // ──────────────────────────────────────────────────────────────────────────
    // keyUp
    // ──────────────────────────────────────────────────────────────────────────

    override fun keyUp(keycode: Int): Boolean {
        // Always process movement key releases so vectors stay accurate even if a key
        // is released while a view is open — prevents lurching on view close.
        if (!keycode.isMovementKey()) return false

        when (keycode) {
            // ── Vertical: UP arrow ──
            UP -> when {
                Gdx.input.isKeyPressed(DOWN) -> { playerSin = -1f; playerDirection = TO }
                Gdx.input.isKeyPressed(W)    -> { /* W still held — keep sin=1f */ }
                else -> playerSin = 0f
            }
            // ── Vertical: W key ──
            W -> when {
                Gdx.input.isKeyPressed(S)    -> { playerSin = -1f; playerDirection = TO }
                Gdx.input.isKeyPressed(UP)   -> { /* UP still held — keep sin=1f */ }
                else -> playerSin = 0f
            }
            // ── Vertical: DOWN arrow ──
            DOWN -> when {
                Gdx.input.isKeyPressed(UP)   -> { playerSin = 1f;  playerDirection = AWAY }
                Gdx.input.isKeyPressed(S)    -> { /* S still held — keep sin=-1f */ }
                else -> playerSin = 0f
            }
            // ── Vertical: S key ──
            S -> when {
                Gdx.input.isKeyPressed(W)    -> { playerSin = 1f;  playerDirection = AWAY }
                Gdx.input.isKeyPressed(DOWN) -> { /* DOWN still held — keep sin=-1f */ }
                else -> playerSin = 0f
            }
            // ── Horizontal: LEFT arrow ──
            LEFT -> when {
                Gdx.input.isKeyPressed(RIGHT) || Gdx.input.isKeyPressed(D) -> { playerCos = 1f; playerDirection = SIDE }
                Gdx.input.isKeyPressed(A)    -> { /* A still held — keep cos=-1f */ }
                else -> playerCos = 0f
            }
            // ── Horizontal: A key ──
            A -> when {
                Gdx.input.isKeyPressed(D) || Gdx.input.isKeyPressed(RIGHT) -> { playerCos = 1f; playerDirection = SIDE }
                Gdx.input.isKeyPressed(LEFT) -> { /* LEFT still held — keep cos=-1f */ }
                else -> playerCos = 0f
            }
            // ── Horizontal: RIGHT arrow ──
            RIGHT -> when {
                Gdx.input.isKeyPressed(LEFT) || Gdx.input.isKeyPressed(A) -> { playerCos = -1f; playerDirection = SIDE }
                Gdx.input.isKeyPressed(D)    -> { /* D still held — keep cos=1f */ }
                else -> playerCos = 0f
            }
            // ── Horizontal: D key ──
            D -> when {
                Gdx.input.isKeyPressed(A) || Gdx.input.isKeyPressed(LEFT) -> { playerCos = -1f; playerDirection = SIDE }
                Gdx.input.isKeyPressed(RIGHT) -> { /* RIGHT still held — keep cos=1f */ }
                else -> playerCos = 0f
            }
        }

        // After updating components, derive playerDirection from remaining movement so the
        // character faces the right way when one of two held keys is released.
        // (Only corrects when exactly one axis is active; diagonal stays as the last keyDown set it.)
        when {
            playerCos != 0f && playerSin == 0f -> playerDirection = SIDE
            playerSin > 0f  && playerCos == 0f -> playerDirection = AWAY
            playerSin < 0f  && playerCos == 0f -> playerDirection = TO
        }

        updatePlayerMovement()
        updatePlayerDirection()
        log.debug { "key released: $keycode, cos: $playerCos, sin: $playerSin, direction: $playerDirection" }
        return true
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Per-view key handlers
    // ──────────────────────────────────────────────────────────────────────────

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
                model.activeTab        = tabs[newIdx]
                model.focusedItemIndex = 0
                model.pendingItemId    = ShopViewModel.NO_PENDING
            }
            RIGHT, D -> {
                val newIdx = (model.activeTab.ordinal + 1) % tabs.size
                model.activeTab        = tabs[newIdx]
                model.focusedItemIndex = 0
                model.pendingItemId    = ShopViewModel.NO_PENDING
            }
            UP, W -> {
                model.focusedItemIndex = (model.focusedItemIndex - 1).coerceAtLeast(0)
            }
            DOWN, S -> {
                model.focusedItemIndex = (model.focusedItemIndex + 1).coerceAtMost(maxIndex)
            }
            TAB -> {
                // Toggle BUY / SELL mode
                model.shopMode         = if (model.shopMode == ShopMode.BUY) ShopMode.SELL else ShopMode.BUY
                model.focusedItemIndex = 0
                model.pendingItemId    = ShopViewModel.NO_PENDING
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
        val settingsView      = uiStage.actors.filterIsInstance<SettingsView>().firstOrNull()
        val characterInfoView = uiStage.actors.filterIsInstance<CharacterInfoView>().first()
        val inventoryView     = uiStage.actors.filterIsInstance<InventoryView>().first()
        val skillView         = uiStage.actors.filterIsInstance<SkillView>().first()
        val abilityView       = uiStage.actors.filterIsInstance<AbilityView>().firstOrNull()
        val questView         = uiStage.actors.filterIsInstance<QuestView>().first()
        val mapView           = uiStage.actors.filterIsInstance<MapView>().first()
        val menuView          = uiStage.actors.filterIsInstance<MenuView>().first()
        val shopView          = uiStage.actors.filterIsInstance<ShopView>().firstOrNull()

        if (shopView?.isVisible == true)        return SHOP
        if (settingsView?.isVisible == true)    return SETTINGS
        if (characterInfoView.isVisible)        return CHARACTER
        if (inventoryView.isVisible)            return INVENTORY
        if (skillView.isVisible)                return SKILL
        if (abilityView?.isVisible == true)     return ABILITY
        if (questView.isVisible)                return QUEST
        if (mapView.isVisible)                  return MAP
        if (menuView.isVisible)                 return MAIN_MENU
        return NO_VIEW
    }

    private fun clearActiveView() {
        uiStage.actors.filterIsInstance<BackgroundView>().first().isVisible      = false
        uiStage.actors.filterIsInstance<SettingsView>().firstOrNull()?.isVisible = false
        uiStage.actors.filterIsInstance<CharacterInfoView>().first().isVisible   = false
        uiStage.actors.filterIsInstance<InventoryView>().first().isVisible       = false
        uiStage.actors.filterIsInstance<SkillView>().first().isVisible           = false
        uiStage.actors.filterIsInstance<AbilityView>().firstOrNull()?.isVisible  = false
        uiStage.actors.filterIsInstance<QuestView>().first().isVisible           = false
        uiStage.actors.filterIsInstance<MapView>().first().isVisible             = false
        uiStage.actors.filterIsInstance<MenuView>().first().isVisible            = false
        uiStage.actors.filterIsInstance<ShopView>().firstOrNull()?.isVisible     = false
    }

    private fun handleQuestKeyDown(keycode: Int) {
        val model = uiStage.actors.filterIsInstance<QuestView>().first().model
        when (keycode) {
            LEFT, A  -> model.movePanelFocus(-1)
            RIGHT, D -> model.movePanelFocus(1)
            UP, W    -> model.moveRowFocus(-1)
            DOWN, S  -> model.moveRowFocus(1)
            ESCAPE   -> {
                uiStage.actors.filterIsInstance<BackgroundView>().first().isVisible = false
                uiStage.actors.filterIsInstance<QuestView>().first().isVisible = false
                gameStage.fire(GameResumeEvent())
            }
        }
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
                    if (!model.isCombatMode) {
                        val newIdx = (model.activeTab.ordinal - 1 + tabs.size) % tabs.size
                        model.activeTab = tabs[newIdx]
                        model.focusedItemIndex = 0
                        model.showingActionMenu = false
                    }
                }
                RIGHT, D -> {
                    if (!model.isCombatMode) {
                        val newIdx = (model.activeTab.ordinal + 1) % tabs.size
                        model.activeTab = tabs[newIdx]
                        model.focusedItemIndex = 0
                        model.showingActionMenu = false
                    }
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
                    if (model.isCombatMode) {
                        gameStage.fire(CombatInventoryClosedEvent())
                    } else {
                        uiStage.actors.filterIsInstance<BackgroundView>().first().isVisible = false
                        uiStage.actors.filterIsInstance<InventoryView>().first().isVisible = false
                        gameStage.fire(InventoryClosedEvent())
                        gameStage.fire(GameResumeEvent())
                    }
                }
            }
            InventoryContext.LEFT -> when (keycode) {
                UP, W -> {
                    if (model.resultMessage.isEmpty())
                        model.focusedCharacterIndex = (model.focusedCharacterIndex - 1).coerceAtLeast(0)
                }
                DOWN, S -> {
                    if (model.resultMessage.isEmpty()) {
                        val max = (model.partyCharacters.size - 1).coerceAtLeast(0)
                        model.focusedCharacterIndex = (model.focusedCharacterIndex + 1).coerceAtMost(max)
                    }
                }
                ENTER -> {
                    if (model.resultMessage.isNotEmpty()) {
                        model.dismissResult()
                    } else {
                        model.confirmPendingAction(model.focusedCharacterIndex)
                    }
                }
                ESCAPE, B -> {
                    if (model.resultMessage.isNotEmpty()) {
                        model.dismissResult()
                    } else if (!model.isCombatMode) {
                        model.pendingAction = PendingAction.None
                        model.activeContext = InventoryContext.RIGHT
                    } else {
                        // In combat, ESC from character list closes the inventory entirely
                        gameStage.fire(CombatInventoryClosedEvent())
                    }
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
