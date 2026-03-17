package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.events.BattleRewardEvent
import com.github.jacks.roleplayinggame.events.RewardDismissedEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.systems.ResourceSystem
import com.github.quillraven.fleks.World

class RewardViewModel(
    private val world: World,
    private val gameStage: Stage,
) : PropertyChangeSource(), EventListener {

    private val resourceSystem: ResourceSystem = world.system()

    var expGained        by propertyNotify(0)
    var goldGained       by propertyNotify(0)
    var hasItemDrop      by propertyNotify(false)
    var itemDropName     by propertyNotify("")
    var itemDropAtlasKey by propertyNotify("")

    init {
        gameStage.addListener(this)
    }

    fun onConfirm() = gameStage.fire(RewardDismissedEvent())

    override fun handle(event: Event): Boolean {
        when (event) {
            is BattleRewardEvent -> {
                val data = event.rewardData
                expGained        = data.expGained
                goldGained       = data.goldGained
                val item         = data.itemDropped
                hasItemDrop      = item != null
                itemDropName     = item?.name     ?: ""
                itemDropAtlasKey = item?.uiAtlasKey ?: ""
                resourceSystem.resources.gold += data.goldGained
                resourceSystem.saveResources()
                return true
            }
            else -> return false
        }
    }
}
