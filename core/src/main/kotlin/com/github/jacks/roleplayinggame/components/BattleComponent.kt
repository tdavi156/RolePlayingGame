package com.github.jacks.roleplayinggame.components

import com.github.quillraven.fleks.Entity

enum class BattlePhase {
    PLAYER_TURN, RESOLVING, ENEMY_TURN, BATTLE_END
}

enum class BattleAction {
    NONE, ATTACK, FLEE
}

data class BattleComponent(
    var toMap: String = "",
    var triggerEntities: MutableSet<Entity> = mutableSetOf(),
    var battleInProgress: Boolean = false,
    // State-machine fields
    var phase: BattlePhase = BattlePhase.PLAYER_TURN,
    var pendingPlayerAction: BattleAction = BattleAction.NONE,
    var resolvingPlayer: Boolean = true,   // true = resolving player action, false = resolving enemy action
)
