package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.AbilityComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.events.AbilityPointsSaveEvent
import com.github.jacks.roleplayinggame.events.AbilitySkillChangedEvent
import com.github.jacks.roleplayinggame.events.AbilityViewClosedEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.quillraven.fleks.IntervalSystem

class AbilitySystem(
    private val gameStage: Stage,
) : IntervalSystem(), EventListener {

    private val abilityMapper by lazy { world.mapper<AbilityComponent>() }
    private val statMapper by lazy { world.mapper<StatComponent>() }
    private val playerMapper by lazy { world.mapper<PlayerComponent>() }
    private val playerFamily by lazy { world.family(allOf = arrayOf(PlayerComponent::class)) }

    private fun partySystem() = world.system<PartySystem>()

    override fun onTick() = Unit

    override fun handle(event: Event): Boolean {
        when (event) {
            is AbilityPointsSaveEvent -> {
                val charId = event.characterId

                // 1. Update CharacterData via PartySystem (source of truth)
                partySystem().updateCharacterData(charId) {
                    unlockedAbilityIds.addAll(event.pendingIds)
                    abilityPoints -= event.pendingIds.size
                }

                // 2. If a live entity exists for this character, sync their components
                var liveEntity: com.github.quillraven.fleks.Entity? = null
                playerFamily.forEach { e -> if (playerMapper[e].characterId == charId) liveEntity = e }
                liveEntity?.let { e ->
                    abilityMapper.getOrNull(e)?.unlockedAbilityIds?.addAll(event.pendingIds)
                    statMapper.getOrNull(e)?.let { stat ->
                        stat.abilityPoints -= event.pendingIds.size
                    }
                    gameStage.fire(AbilitySkillChangedEvent(e))
                }

                gameStage.fire(AbilityViewClosedEvent())
                return true
            }
            else -> return false
        }
    }
}
