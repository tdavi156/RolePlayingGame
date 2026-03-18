package com.github.jacks.roleplayinggame.configurations

import com.github.jacks.roleplayinggame.components.AnimationModel

enum class EnemyType(val animationModel: AnimationModel) {
    BLUE_SLIME(AnimationModel.SLIME_BLUE),
    GREEN_SLIME(AnimationModel.SLIME_GREEN),
    RED_SLIME(AnimationModel.SLIME_RED);
}

sealed class QuestCondition {
    data class KillEnemy(val enemyType: EnemyType, val requiredCount: Int) : QuestCondition()
    // Future: CollectItem, ReachLocation, TalkToNPC
}

data class QuestReward(val goldAmount: Int)

data class QuestConfig(
    val questId: Int,
    val questName: String,
    val questDescription: String,
    val condition: QuestCondition,
    val reward: QuestReward
)

val QUESTS: Map<Int, QuestConfig> = mapOf(
    1 to QuestConfig(
        questId = 1,
        questName = "Slime Exterminator",
        questDescription = "Kill 1 blue slime for the questman.",
        condition = QuestCondition.KillEnemy(EnemyType.BLUE_SLIME, requiredCount = 1),
        reward = QuestReward(goldAmount = 100)
    ),
    // QuestConfig(
    //     questId = 2,
    //     questName = "Quest Name",
    //     questDescription = "Quest description.",
    //     condition = QuestCondition.KillEnemy(EnemyType.GREEN_SLIME, requiredCount = 5),
    //     reward = QuestReward(goldAmount = 50)
    // ),
)
