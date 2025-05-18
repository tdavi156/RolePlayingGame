package com.github.jacks.roleplayinggame.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys.*
import com.github.jacks.roleplayinggame.components.AttackComponent
import com.github.jacks.roleplayinggame.components.MoveComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.systems.AnimationSystem
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.World
import ktx.app.KtxInputAdapter
import ktx.log.logger

class PlayerKeyboardInputProcessor(
    private val world : World,
    private val moveComponents : ComponentMapper<MoveComponent> = world.mapper(),
    private val attackComponents : ComponentMapper<AttackComponent> = world.mapper()
) : KtxInputAdapter {

    private var playerSin = 0f
    private var playerCos = 0f
    private var playerDirection = TO
    private val playerEntities = world.family(allOf = arrayOf(PlayerComponent::class))

    init {
        Gdx.input.inputProcessor = this
    }

    private fun updatePlayerMovement() {
        playerEntities.forEach { player ->
            with (moveComponents[player]) {
                cos = playerCos
                sin = playerSin
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
