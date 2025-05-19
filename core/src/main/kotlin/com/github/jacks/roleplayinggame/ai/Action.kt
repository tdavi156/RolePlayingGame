package com.github.jacks.roleplayinggame.ai

import com.badlogic.gdx.ai.GdxAI
import com.badlogic.gdx.ai.btree.LeafTask
import com.badlogic.gdx.ai.btree.Task
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute
import com.badlogic.gdx.ai.utils.random.FloatDistribution
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.math.MathUtils
import com.github.jacks.roleplayinggame.components.AnimationType
import ktx.math.vec2

abstract class Action : LeafTask<AiEntity>() {
    val entity : AiEntity
        get() = `object`

    override fun copyTo(task: Task<AiEntity>) = task
}

class IdleTask(
    @JvmField
    @TaskAttribute(required = true)
    var duration : FloatDistribution? = null
) : Action() {
    private var currentDuration = 0f

    override fun execute(): Status {
        if (status != Status.RUNNING) {
            entity.animation(AnimationType.IDLE)
            currentDuration = duration?.nextFloat() ?: 1f
            return Status.RUNNING
        }

        currentDuration -= GdxAI.getTimepiece().deltaTime
        if (currentDuration <= 0) {
            return Status.SUCCEEDED
        }

        if (entity.canAttack() && entity.hasEnemyNearby()) {
            return Status.SUCCEEDED
        }

        return Status.RUNNING
    }

    override fun copyTo(task: Task<AiEntity>): Task<AiEntity> {
        (task as IdleTask).duration = duration
        return task
    }
}

class WanderTask(
    @JvmField
    @TaskAttribute(required = true)
    var duration : FloatDistribution? = null
) : Action() {
    private var currentDuration = 0f
    private val startPosition = vec2()
    private val targetPosition = vec2()

    override fun execute(): Status {
        if (status != Status.RUNNING) {
            entity.animation(AnimationType.MOVE)
            currentDuration = duration?.nextFloat() ?: 1f
            if (startPosition.isZero) {
                startPosition.set(entity.position)
            }
            targetPosition.set(startPosition)
            targetPosition.x += MathUtils.random(-3f, 3f)
            targetPosition.y += MathUtils.random(-3f, 3f)
            entity.moveTo(targetPosition)
            return Status.RUNNING
        }

        if (entity.inRange(0.5f, targetPosition)) {
            entity.stopMove()
            return Status.SUCCEEDED
        }

        if (entity.canAttack() && entity.hasEnemyNearby()) {
            entity.stopMove()
            return Status.SUCCEEDED
        }

        currentDuration -= GdxAI.getTimepiece().deltaTime
        if (currentDuration <= 0) {
            return Status.SUCCEEDED
        }

        return Status.RUNNING
    }

    override fun copyTo(task: Task<AiEntity>): Task<AiEntity> {
        return super.copyTo(task)
    }
}

class AttackTask : Action() {
    override fun execute(): Status {
        if (status != Status.RUNNING) {
            entity.animation(AnimationType.ATTACK, Animation.PlayMode.NORMAL, true)
            entity.doAndStartAttack()
            return Status.RUNNING
        }

        if (entity.isAnimationDone) {
            entity.animation(AnimationType.IDLE)
            entity.stopMove()
            return Status.SUCCEEDED
        }

        return Status.RUNNING
    }

    override fun copyTo(task: Task<AiEntity>): Task<AiEntity> {
        return super.copyTo(task)
    }
}
