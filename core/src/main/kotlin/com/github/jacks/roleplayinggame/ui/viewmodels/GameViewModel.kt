package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.AnimationComponent
import com.github.jacks.roleplayinggame.components.ItemComponent
import com.github.jacks.roleplayinggame.components.LifeComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.events.EntityAddItemEvent
import com.github.jacks.roleplayinggame.events.EntityTakeDamageEvent
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.World

class GameViewModel(
    world : World,
    stage : Stage
) : PropertyChangeSource(), EventListener {

    private val playerComponents : ComponentMapper<PlayerComponent> = world.mapper()
    private val lifeComponents : ComponentMapper<LifeComponent> = world.mapper()
    private val itemComponents : ComponentMapper<ItemComponent> = world.mapper()
    private val animationComponents : ComponentMapper<AnimationComponent> = world.mapper()

    var playerLife by propertyNotify(1f)
    var enemyLife by propertyNotify(1f)
    var lootText by propertyNotify("")

    init {
        stage.addListener(this)
    }

    override fun handle(event: Event?): Boolean {
        when(event) {
            is EntityTakeDamageEvent -> {
                val isPlayer = event.entity in playerComponents
                val lifeComponent = lifeComponents[event.entity]
                if(isPlayer) {
                    playerLife = lifeComponent.health / lifeComponent.maxHealth
                } else {
                    enemyLife = lifeComponent.health / lifeComponent.maxHealth
                }
            }
            is EntityAddItemEvent -> {
                lootText = "New Item found: [#4e557d]${event.itemType.itemName}[]"
            }
            else -> return false
        }
        return true
    }
}
