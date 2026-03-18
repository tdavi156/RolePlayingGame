package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.configurations.QUESTS
import com.github.jacks.roleplayinggame.configurations.QuestCondition
import com.github.jacks.roleplayinggame.events.QuestStateChangedEvent
import com.github.jacks.roleplayinggame.systems.QuestStatus
import com.github.jacks.roleplayinggame.systems.QuestSystem
import com.github.quillraven.fleks.World

enum class QuestPanelFocus { ACTIVE, COMPLETED }

data class QuestRowUiState(
    val questId: Int,
    val questName: String,
    val description: String,
    val progressText: String,
    val rewardText: String,
    val isReadyToDeliver: Boolean,
)

class QuestViewModel(
    private val world: World,
    stage: Stage,
) : PropertyChangeSource(), EventListener {

    private val questSystem by lazy { world.system<QuestSystem>() }

    var activeQuests by propertyNotify(emptyList<QuestRowUiState>())
    var completedQuests by propertyNotify(emptyList<QuestRowUiState>())
    var activePanelFocus by propertyNotify(QuestPanelFocus.ACTIVE)
    var focusedQuestIndex by propertyNotify(0)

    init {
        stage.addListener(this)
    }

    override fun handle(event: Event): Boolean {
        return when (event) {
            is QuestStateChangedEvent -> {
                rebuildLists()
                true
            }
            else -> false
        }
    }

    fun rebuildLists() {
        val active = mutableListOf<QuestRowUiState>()
        val completed = mutableListOf<QuestRowUiState>()

        questSystem.questStates.values.forEach { state ->
            val config = QUESTS[state.questId] ?: return@forEach
            val progressText = when (val condition = config.condition) {
                is QuestCondition.KillEnemy -> {
                    val typeName = condition.enemyType.name.split("_")
                        .joinToString(" ") { word -> word.lowercase().replaceFirstChar { it.uppercase() } }
                    "$typeName Killed: ${state.progress}/${condition.requiredCount}"
                }
            }
            val row = QuestRowUiState(
                questId = state.questId,
                questName = config.questName,
                description = config.questDescription,
                progressText = progressText,
                rewardText = "Reward: ${config.reward.goldAmount} gold",
                isReadyToDeliver = state.status == QuestStatus.CONDITIONS_MET,
            )
            when (state.status) {
                QuestStatus.NOT_STARTED -> { /* not shown */ }
                QuestStatus.ACTIVE, QuestStatus.CONDITIONS_MET -> active.add(row)
                QuestStatus.COMPLETED -> completed.add(row)
            }
        }

        activeQuests = active
        completedQuests = completed
        clampFocusedIndex()
    }

    val focusedQuest: QuestRowUiState?
        get() {
            val list = currentList()
            return list.getOrNull(focusedQuestIndex)
        }

    fun movePanelFocus(delta: Int) {
        val values = QuestPanelFocus.entries
        activePanelFocus = values[(activePanelFocus.ordinal + delta + values.size) % values.size]
        focusedQuestIndex = 0
    }

    fun moveRowFocus(delta: Int) {
        val max = (currentList().size - 1).coerceAtLeast(0)
        focusedQuestIndex = (focusedQuestIndex + delta).coerceIn(0, max)
    }

    private fun currentList() = if (activePanelFocus == QuestPanelFocus.ACTIVE) activeQuests else completedQuests

    private fun clampFocusedIndex() {
        val max = (currentList().size - 1).coerceAtLeast(0)
        focusedQuestIndex = focusedQuestIndex.coerceIn(0, max)
    }
}
