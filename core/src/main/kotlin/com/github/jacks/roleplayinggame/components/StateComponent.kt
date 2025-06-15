package com.github.jacks.roleplayinggame.components

import com.badlogic.gdx.ai.fsm.DefaultStateMachine
import com.github.jacks.roleplayinggame.ai.AiEntity
import com.github.jacks.roleplayinggame.ai.DefaultGlobalState
import com.github.jacks.roleplayinggame.ai.DefaultState
import com.github.jacks.roleplayinggame.ai.EntityState
import com.github.quillraven.fleks.ComponentListener
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

data class StateComponent (
    var nextState : EntityState = DefaultState.IDLE,
    val stateMachine : DefaultStateMachine<AiEntity, EntityState> = DefaultStateMachine()
) {

    companion object {
        class StateComponentListener(
            private val world : World
        ) : ComponentListener<StateComponent> {
            override fun onComponentAdded(entity: Entity, component: StateComponent) {
                component.stateMachine.owner = AiEntity(entity, world)
                component.stateMachine.globalState = DefaultGlobalState.CHECK_ALIVE
            }

            override fun onComponentRemoved(entity: Entity, component: StateComponent) = Unit
        }
    }
}
