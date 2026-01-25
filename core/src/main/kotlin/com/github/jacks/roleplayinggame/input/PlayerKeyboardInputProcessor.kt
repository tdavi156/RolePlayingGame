package com.github.jacks.roleplayinggame.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys.*
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.AttackComponent
import com.github.jacks.roleplayinggame.components.MoveComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.events.GamePauseEvent
import com.github.jacks.roleplayinggame.events.GameResumeEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.ui.views.BackgroundView
import com.github.jacks.roleplayinggame.ui.views.CharacterInfoView
import com.github.jacks.roleplayinggame.ui.views.InventoryView
import com.github.jacks.roleplayinggame.ui.views.MapView
import com.github.jacks.roleplayinggame.ui.views.MenuView
import com.github.jacks.roleplayinggame.ui.views.PauseView
import com.github.jacks.roleplayinggame.ui.views.QuestView
import com.github.jacks.roleplayinggame.ui.views.SkillView
import com.github.jacks.roleplayinggame.ui.views.characterInfoView
import com.github.jacks.roleplayinggame.ui.views.inventoryView
import com.github.jacks.roleplayinggame.ui.views.mapView
import com.github.jacks.roleplayinggame.ui.views.menuView
import com.github.jacks.roleplayinggame.ui.views.questView
import com.github.jacks.roleplayinggame.ui.views.skillView
import com.github.jacks.roleplayinggame.input.ViewType.*
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.World
import ktx.app.KtxInputAdapter
import ktx.log.logger
import ktx.math.vec2

enum class ViewType {
    NO_VIEW, CHARACTER, INVENTORY, SKILL, QUEST, MAP, MAIN_MENU
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
    private val world : World,
    private val gameStage : Stage,
    private val uiStage : Stage,
    private val moveComponents : ComponentMapper<MoveComponent> = world.mapper(),
    private val attackComponents : ComponentMapper<AttackComponent> = world.mapper(),
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
        } else if (!keycode.isMovementKey()) {
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
                SPACE -> {
                    playerEntities.forEach {
                        with(attackComponents[it]) {
                            doAttack = true
                        }
                    }
                }
                E -> {
                    // interact with other entities
                    // initiate dialog with a sign or another entity
                    // open a chest on the ground or pick something up from the ground (for quests later)
                    // use an item on the overworld, like opening a locked door with a key
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
                        backgroundView.isVisible = true
                        inventoryView.isVisible = true
                    } else if (getActiveView() == INVENTORY) {
                        backgroundView.isVisible = false
                        inventoryView.isVisible = false
                        gameStage.fire(GameResumeEvent())
                    } else {
                        clearActiveView()
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
        } else {
            return false
        }
    }

    override fun keyUp(keycode: Int): Boolean {
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

    private fun getActiveView() : ViewType {
        val characterInfoView = uiStage.actors.filterIsInstance<CharacterInfoView>().first()
        val inventoryView = uiStage.actors.filterIsInstance<InventoryView>().first()
        val skillView = uiStage.actors.filterIsInstance<SkillView>().first()
        val questView = uiStage.actors.filterIsInstance<QuestView>().first()
        val mapView = uiStage.actors.filterIsInstance<MapView>().first()
        val menuView = uiStage.actors.filterIsInstance<MenuView>().first()

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
        uiStage.actors.filterIsInstance<CharacterInfoView>().first().isVisible = false
        uiStage.actors.filterIsInstance<InventoryView>().first().isVisible = false
        uiStage.actors.filterIsInstance<SkillView>().first().isVisible = false
        uiStage.actors.filterIsInstance<QuestView>().first().isVisible = false
        uiStage.actors.filterIsInstance<MapView>().first().isVisible = false
        uiStage.actors.filterIsInstance<MenuView>().first().isVisible = false
    }

    companion object {
        private val log = logger<PlayerKeyboardInputProcessor>()
        const val TO = "to"
        const val AWAY = "away"
        const val SIDE = "side"
    }
}
