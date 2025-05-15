package com.github.jacks.roleplayinggame.ai

import com.badlogic.gdx.graphics.g2d.Animation
import com.github.jacks.roleplayinggame.components.AnimationType

enum class DefaultState : EntityState {
    IDLE {
        override fun enter(entity: AiEntity) {
            entity.animation(AnimationType.IDLE)
        }

        override fun update(entity: AiEntity) {
            when {
                entity.wantsToAttack -> entity.state(ATTACK)
                entity.wantsToMove -> entity.state(MOVE)
            }
        }
    },
    MOVE {
        override fun enter(entity: AiEntity) {
            entity.animation(AnimationType.MOVE)
        }

        override fun update(entity: AiEntity) {
            when {
                entity.wantsToAttack -> entity.state(ATTACK)
                !entity.wantsToMove -> entity.state(IDLE)
            }
        }
    },
    ATTACK {
        override fun enter(entity: AiEntity) {
            entity.animation(AnimationType.ATTACK, Animation.PlayMode.NORMAL)
            entity.root(true)
            entity.startAttack()
        }

        override fun exit(entity: AiEntity) {
            entity.root(false)
        }

        override fun update(entity: AiEntity) {
            val attackComponent = entity.attackComponent
            if (attackComponent.isReady && !attackComponent.doAttack) {
                entity.changeToPreviousState()
            } else if (attackComponent.isReady && attackComponent.doAttack) {
                entity.animation(AnimationType.ATTACK, Animation.PlayMode.NORMAL, true)
                entity.startAttack()
            }
        }
    },
    DEATH,
    RESPAWN
}

enum class DefaultGlobalState : EntityState {
    CHECK_ALIVE
}
