package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.configurations.EnemyType
import com.github.jacks.roleplayinggame.configurations.QUESTS
import com.github.jacks.roleplayinggame.configurations.QuestCondition
import com.github.jacks.roleplayinggame.events.AcceptQuestEvent
import com.github.jacks.roleplayinggame.events.CompleteQuestEvent
import com.github.jacks.roleplayinggame.events.EnemyKilledEvent
import com.github.jacks.roleplayinggame.events.QuestStateChangedEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.saveManager.SaveManager
import com.github.quillraven.fleks.IntervalSystem

enum class QuestStatus { NOT_STARTED, ACTIVE, CONDITIONS_MET, COMPLETED }

data class QuestState(
    val questId: Int,
    var progress: Int = 0,
    var status: QuestStatus = QuestStatus.NOT_STARTED
)

class QuestSystem(
    private val gameStage: Stage,
    private val saveManager: SaveManager,
) : IntervalSystem(), EventListener {

    val questStates: MutableMap<Int, QuestState> = mutableMapOf()
    private val resourceSystem by lazy { world.system<ResourceSystem>() }

    override fun onTick() { /* driven entirely by events */ }

    override fun handle(event: Event): Boolean {
        when (event) {
            is AcceptQuestEvent -> {
                val state = questStates.getOrPut(event.questId) { QuestState(event.questId) }
                state.status = QuestStatus.ACTIVE
                state.progress = 0
                saveManager.gatherAndSave(world)
                gameStage.fire(QuestStateChangedEvent(event.questId))
                return true
            }
            is EnemyKilledEvent -> {
                var anyUpdated = false
                questStates.values.filter { it.status == QuestStatus.ACTIVE }.forEach { state ->
                    val config = QUESTS[state.questId] ?: return@forEach
                    val condition = config.condition
                    if (condition is QuestCondition.KillEnemy && condition.enemyType == event.enemyType) {
                        state.progress++
                        if (state.progress >= condition.requiredCount) {
                            state.status = QuestStatus.CONDITIONS_MET
                        }
                        anyUpdated = true
                        saveManager.gatherAndSave(world)
                        gameStage.fire(QuestStateChangedEvent(state.questId))
                    }
                }
                return anyUpdated
            }
            is CompleteQuestEvent -> {
                val state = questStates[event.questId] ?: return false
                state.status = QuestStatus.COMPLETED
                val config = QUESTS[event.questId]
                if (config != null) {
                    resourceSystem.resources.gold += config.reward.goldAmount
                }
                saveManager.gatherAndSave(world)
                gameStage.fire(QuestStateChangedEvent(event.questId))
                return true
            }
            else -> return false
        }
    }

    fun getState(questId: Int): QuestState {
        return questStates.getOrPut(questId) { QuestState(questId) }
    }
}
