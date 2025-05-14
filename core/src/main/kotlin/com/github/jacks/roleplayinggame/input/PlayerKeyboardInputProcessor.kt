package com.github.jacks.roleplayinggame.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys.*
import com.github.jacks.roleplayinggame.components.AttackComponent
import com.github.jacks.roleplayinggame.components.MoveComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.World
import ktx.app.KtxInputAdapter

class PlayerKeyboardInputProcessor(
    private val world : World,
    private val moveComponents : ComponentMapper<MoveComponent> = world.mapper(),
    private val attackComponents : ComponentMapper<AttackComponent> = world.mapper()
) : KtxInputAdapter {

    private var playerSin = 0f
    private var playerCos = 0f
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

    private fun Int.isMovementKey() : Boolean {
        return this == UP || this == DOWN || this == LEFT || this == RIGHT || this == W || this == A || this == S || this == D
    }

    override fun keyDown(keycode: Int): Boolean {
        if (keycode.isMovementKey()) {
            when (keycode) {
                UP -> playerSin = 1f
                W -> playerSin = 1f
                DOWN -> playerSin = -1f
                S -> playerSin = -1f
                LEFT -> playerCos = -1f
                A -> playerCos = -1f
                RIGHT -> playerCos = 1f
                D -> playerCos = 1f
            }
            updatePlayerMovement()
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
                UP -> playerSin = if (Gdx.input.isKeyPressed(DOWN)) -1f else 0f
                W -> playerSin = if (Gdx.input.isKeyPressed(S)) -1f else 0f
                DOWN -> playerSin = if (Gdx.input.isKeyPressed(UP)) 1f else 0f
                S -> playerSin = if (Gdx.input.isKeyPressed(W)) 1f else 0f
                LEFT -> playerCos = if (Gdx.input.isKeyPressed(RIGHT)) 1f else 0f
                A -> playerCos = if (Gdx.input.isKeyPressed(D)) 1f else 0f
                RIGHT -> playerCos = if (Gdx.input.isKeyPressed(LEFT)) -1f else 0f
                D -> playerCos = if (Gdx.input.isKeyPressed(A)) -1f else 0f
            }
            updatePlayerMovement()
            return true
        }
        return false
    }
}
