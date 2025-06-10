package com.github.jacks.roleplayinggame.ai

import com.badlogic.gdx.graphics.g2d.Animation.PlayMode
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.github.jacks.roleplayinggame.components.AiComponent
import com.github.jacks.roleplayinggame.components.AnimationComponent
import com.github.jacks.roleplayinggame.components.AnimationDirection
import com.github.jacks.roleplayinggame.components.AnimationType
import com.github.jacks.roleplayinggame.components.AttackComponent
import com.github.jacks.roleplayinggame.components.DeathComponent
import com.github.jacks.roleplayinggame.components.LifeComponent
import com.github.jacks.roleplayinggame.components.MoveComponent
import com.github.jacks.roleplayinggame.components.PhysicsComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.components.StateComponent
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import ktx.math.component1
import ktx.math.component2
import ktx.math.vec2

private val TEMP_RECTANGLE = Rectangle()

data class AiEntity(
    private val entity : Entity,
    private val world : World,
    private val animationComponents : ComponentMapper<AnimationComponent> = world.mapper(),
    private val moveComponents : ComponentMapper<MoveComponent> = world.mapper(),
    private val attackComponents : ComponentMapper<AttackComponent> = world.mapper(),
    private val stateComponents : ComponentMapper<StateComponent> = world.mapper(),
    private val deathComponents : ComponentMapper<DeathComponent> = world.mapper(),
    private val lifeComponents : ComponentMapper<LifeComponent> = world.mapper(),
    private val statComponents : ComponentMapper<StatComponent> = world.mapper(),
    private val physicsComponents : ComponentMapper<PhysicsComponent> = world.mapper(),
    private val aiComponents : ComponentMapper<AiComponent> = world.mapper(),
    private val playerComponents : ComponentMapper<PlayerComponent> = world.mapper()
) {

    val position : Vector2
        get() = physicsComponents[entity].body.position

    val wantsToMove : Boolean
        get() {
            val moveComponent = moveComponents[entity]
            return moveComponent.cos != 0f || moveComponent.sin != 0f
        }

    val wantsToAttack : Boolean
        get() = attackComponents.getOrNull(entity)?.doAttack ?: false

    val attackComponent : AttackComponent
        get() = attackComponents[entity]

    val moveComponent : MoveComponent
        get() = moveComponents[entity]

    val isAnimationDone : Boolean
        get() = animationComponents[entity].isAnimationDone

    val isDead : Boolean
        get() = statComponents[entity].isDead

    val directionChanged : Boolean
        get() = moveComponents[entity].directionChanged

    val direction : AnimationDirection
        get() {
            val direction = moveComponents[entity].direction
            return if (direction == "away") {
                AnimationDirection.AWAY
            } else if (direction == "side") {
                AnimationDirection.SIDE
            } else {
                AnimationDirection.TO
            }
        }

    fun animation(type : AnimationType, mode : PlayMode = PlayMode.LOOP, resetAnimation : Boolean = false) {
        // directionChanged will be reset when the input is changed to a new direction, retriggering a state change
        moveComponents[entity].directionChanged = false
        with(animationComponents[entity]) {
            nextAnimation(type, direction)
            playMode = mode
            if (resetAnimation) {
                stateTime = 0f
            }
        }
    }

    fun state(next : EntityState, changeStateImmediately : Boolean = false) {
        with(stateComponents[entity]) {
            nextState = next
            if (changeStateImmediately) {
                stateMachine.changeState(nextState)
            }
        }
    }

    fun enableGlobalState(enable : Boolean) {
        with(stateComponents[entity]) {
            if (enable) {
                stateMachine.globalState = DefaultGlobalState.CHECK_ALIVE
            } else {
                stateMachine.globalState = null
            }
        }
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

    fun doAndStartAttack() {
        with(attackComponents[entity]) {
            doAttack = true
            startAttack()
        }
    }

    fun moveTo(targetPosition : Vector2) {
        val physicsComponent = physicsComponents[entity]
        val (sourceX, sourceY) = physicsComponent.body.position
        val (targetX, targetY) = targetPosition
        with(moveComponents[entity]) {
            val angleRadians = MathUtils.atan2(targetY - sourceY, targetX - sourceX)
            cos = MathUtils.cos(angleRadians)
            sin = MathUtils.sin(angleRadians)
        }

    }

    fun inRange(range : Float, targetPosition : Vector2) : Boolean {
        val physicsComponent = physicsComponents[entity]
        val (sourceX, sourceY) = physicsComponent.body.position
        val (offsetX, offsetY) = physicsComponent.offset
        var (sizeX, sizeY) = physicsComponent.size
        sizeX += range
        sizeY += range

        TEMP_RECTANGLE.set(
            sourceX + offsetX - sizeX * 0.5f,
            sourceY + offsetY - sizeY * 0.5f,
            sizeX,
            sizeY
        )

        return TEMP_RECTANGLE.contains(targetPosition)
    }

    fun stopMove() {
        with(moveComponents[entity]) {
            cos = 0f
            sin = 0f
        }
    }

    fun canAttack(): Boolean {
        val attackComponent = attackComponents[entity]
        if (!attackComponent.isReady) {
            return false
        }

        val enemy = getNearbyEnemies().firstOrNull() ?: return false
        val enemyPhysicsComponent = physicsComponents[enemy]
        val (sourceX, sourceY) = enemyPhysicsComponent.body.position
        val (offsetX, offsetY) = enemyPhysicsComponent.offset
        return inRange(1.75f, vec2(sourceX + offsetX, sourceY + offsetY))
    }

    fun hasEnemyNearby() = getNearbyEnemies().isNotEmpty()

    private fun getNearbyEnemies() : List<Entity>{
        val aiComponent = aiComponents[entity]
        return aiComponent.nearbyEntities
            .filter { it in playerComponents && !statComponents[it].isDead }
    }
}
