package com.github.jacks.roleplayinggame.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys.*
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.AttackComponent
import com.github.jacks.roleplayinggame.components.MoveComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.events.GamePauseEvent
import com.github.jacks.roleplayinggame.events.GameResumeEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.World
import ktx.app.KtxInputAdapter
import ktx.log.logger
import ktx.math.vec2

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
        } else if (keycode == SPACE) {
            playerEntities.forEach {
                with(attackComponents[it]) {
                    doAttack = true
                }
            }
            return true
        } else if (keycode == I) {
            paused = false
            pausedInventory = !pausedInventory
            gameStage.fire(if (pausedInventory) GamePauseEvent() else GameResumeEvent())
            // dont hardcode the actor position, when we change the order of UI actors, this will break
            uiStage.actors.get(3).isVisible = !uiStage.actors.get(3).isVisible
        } else if (keycode == P) {
            if (!pausedInventory) {
                paused = !paused
                gameStage.fire(if (paused) GamePauseEvent() else GameResumeEvent())
            }
        }
        return false
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

    companion object {
        private val log = logger<PlayerKeyboardInputProcessor>()
        const val TO = "to"
        const val AWAY = "away"
        const val SIDE = "side"
    }
}
