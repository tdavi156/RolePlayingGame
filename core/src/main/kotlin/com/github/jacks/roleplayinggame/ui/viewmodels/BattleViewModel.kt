package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.AnimationComponent
import com.github.jacks.roleplayinggame.components.ItemComponent
import com.github.jacks.roleplayinggame.components.LifeComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.events.BattleEvent
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.World

class BattleViewModel(
    world : World,
    stage : Stage
) : PropertyChangeSource(), EventListener {

    private val playerComponents : ComponentMapper<PlayerComponent> = world.mapper()
    private val lifeComponents : ComponentMapper<LifeComponent> = world.mapper()
    private val statComponents : ComponentMapper<StatComponent> = world.mapper()
    private val itemComponents : ComponentMapper<ItemComponent> = world.mapper()
    private val animationComponents : ComponentMapper<AnimationComponent> = world.mapper()

    var playerLife by propertyNotify(1f)
    var enemyLife by propertyNotify(1f)
    var lootText by propertyNotify("")

    init {
        stage.addListener(this)
    }

    override fun handle(event: Event): Boolean {
        when(event) {
            is BattleEvent -> {

            }
            else -> return false
        }
        return true
    }
}
