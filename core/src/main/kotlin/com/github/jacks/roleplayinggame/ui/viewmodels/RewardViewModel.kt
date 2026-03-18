package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.configurations.EquipmentItemData
import com.github.jacks.roleplayinggame.events.BattleRewardEvent
import com.github.jacks.roleplayinggame.events.RewardDismissedEvent
import com.github.jacks.roleplayinggame.events.fire

data class ItemDropInfo(val name: String, val uiAtlasKey: String)

class RewardViewModel(
    private val gameStage: Stage,
) : PropertyChangeSource(), EventListener {

    var expGained  by propertyNotify(0)
    var goldGained by propertyNotify(0)
    var itemDrops  by propertyNotify(emptyList<ItemDropInfo>())

    init {
        gameStage.addListener(this)
    }

    fun onConfirm() = gameStage.fire(RewardDismissedEvent())

    override fun handle(event: Event): Boolean {
        when (event) {
            is BattleRewardEvent -> {
                val data = event.rewardData
                expGained  = data.expGained
                goldGained = data.goldGained
                itemDrops  = data.items.map { ItemDropInfo(it.name, it.uiAtlasKey) }
                // Gold is released to ResourceSystem by BattleSystem — no double-add here
                return true
            }
            else -> return false
        }
    }
}
