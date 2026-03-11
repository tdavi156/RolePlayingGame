package com.github.jacks.roleplayinggame.systems

import com.github.jacks.roleplayinggame.components.AiComponent
import com.github.jacks.roleplayinggame.components.DeathComponent
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.NoneOf

/**
 * Steps the behavior tree AI each tick for overworld entities.
 *
 * This system is automatically suspended during turn-based battles because it is
 * NOT in [com.github.jacks.roleplayinggame.screens.GameScreen]'s battleModeSystems set.
 * enterBattleMode() disables it, and exitBattleMode() re-enables it. Battle enemies
 * don't have AiComponent, so even if this system were enabled during battle it would
 * have no effect on them.
 */
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
