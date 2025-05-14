package com.github.jacks.roleplayinggame.ai

import com.badlogic.gdx.graphics.g2d.Animation.PlayMode
import com.github.jacks.roleplayinggame.components.AnimationComponent
import com.github.jacks.roleplayinggame.components.AnimationType
import com.github.jacks.roleplayinggame.components.AttackComponent
import com.github.jacks.roleplayinggame.components.MoveComponent
import com.github.jacks.roleplayinggame.components.StateComponent
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

data class AiEntity(
    private val entity : Entity,
    private val world : World,
    private val animationComponents : ComponentMapper<AnimationComponent> = world.mapper(),
    private val moveComponents : ComponentMapper<MoveComponent> = world.mapper(),
    private val attackComponents : ComponentMapper<AttackComponent> = world.mapper(),
    private val stateComponents : ComponentMapper<StateComponent> = world.mapper(),
) {

    val wantsToMove : Boolean
        get() {
            val moveComponent = moveComponents[entity]
            return moveComponent.cos != 0f || moveComponent.sin != 0f
        }

    val wantsToAttack : Boolean
        get() = attackComponents.getOrNull(entity)?.doAttack ?: false

    val attackComponent : AttackComponent
        get() = attackComponents[entity]

    fun animation(type : AnimationType, mode : PlayMode = PlayMode.LOOP, resetAnimation : Boolean = false) {
        with(animationComponents[entity]) {
            nextAnimation(type)
            playMode = mode
            if (resetAnimation) {
                stateTime = 0f
            }
        }
    }

    fun state(next : EntityState) {
        with(stateComponents[entity]) { nextState = next }
    }

    fun changeToPreviousState() {
        with(stateComponents[entity]) { nextState = stateMachine.previousState}
    }

    fun root(status : Boolean) {
        with(moveComponents[entity]) { isRooted = status }
    }

    fun startAttack() {
        with(attackComponents[entity]) { startAttack() }
    }
}
