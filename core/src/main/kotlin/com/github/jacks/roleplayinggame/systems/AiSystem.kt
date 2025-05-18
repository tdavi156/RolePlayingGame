package com.github.jacks.roleplayinggame.systems

import com.github.jacks.roleplayinggame.components.AiComponent
import com.github.jacks.roleplayinggame.components.DeathComponent
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.NoneOf

@AllOf([AiComponent::class])
@NoneOf([DeathComponent::class])
class AiSystem(
    private val aiComponents : ComponentMapper<AiComponent>
) : IteratingSystem() {


    override fun onTickEntity(entity: Entity) {
        with(aiComponents[entity]) {
            behaviorTree.step()
        }
    }
}
