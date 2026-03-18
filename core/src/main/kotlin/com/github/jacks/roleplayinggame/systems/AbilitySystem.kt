package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.AbilityComponent
import com.github.jacks.roleplayinggame.events.AbilityPointsSaveEvent
import com.github.jacks.roleplayinggame.events.AbilitySkillChangedEvent
import com.github.jacks.roleplayinggame.events.AbilityViewClosedEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.quillraven.fleks.IntervalSystem
import ktx.preferences.flush
import ktx.preferences.set

class AbilitySystem(
    private val gameStage: Stage,
) : IntervalSystem(), EventListener {

    private val abilityMapper by lazy { world.mapper<AbilityComponent>() }
    private val preferences: Preferences by lazy { Gdx.app.getPreferences("rolePlayingGamePrefs") }

    override fun onTick() = Unit

    override fun handle(event: Event): Boolean {
        when (event) {
            is AbilityPointsSaveEvent -> {
                val abilityComp = abilityMapper.getOrNull(event.entity) ?: return false
                abilityComp.unlockedAbilityIds.addAll(event.pendingIds)
                preferences.flush {
                    this[AbilityComponent.KEY_UNLOCKED_ABILITY_IDS] =
                        abilityComp.unlockedAbilityIds.joinToString(",")
                }
                gameStage.fire(AbilitySkillChangedEvent(event.entity))
                gameStage.fire(AbilityViewClosedEvent())
                return true
            }
            else -> return false
        }
    }
}
