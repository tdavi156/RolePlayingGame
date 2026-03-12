package com.github.jacks.roleplayinggame.components

import com.github.quillraven.fleks.Entity

enum class BattlePhase {
    PLAYER_TURN, RESOLVING, ENEMY_TURN, BATTLE_END
}

enum class BattleAction {
    NONE, ATTACK, FLEE
}

enum class BattleEndReason {
    WIN, LOSE, FLEE
}

data class BattleComponent(
    var toMap: String = "",
    var triggerEntities: MutableSet<Entity> = mutableSetOf(),
    var battleInProgress: Boolean = false,
    // State-machine fields
    var phase: BattlePhase = BattlePhase.PLAYER_TURN,
    var pendingPlayerAction: BattleAction = BattleAction.NONE,
    var resolvingPlayer: Boolean = true,   // true = resolving player action, false = resolving enemy action
    // Battle-end fields
    var endReason: BattleEndReason = BattleEndReason.WIN,
    var endDelayTimer: Float = -1f,        // countdown before firing BattleEndEvent; < 0 = not started
    var enemyTurnDelayTimer: Float = -1f,  // countdown before enemy acts; < 0 = not started
    var postEnemyResolve: Boolean = false, // true = delay is for showing enemy attack result, then go to PLAYER_TURN
    var waitingForEndDismiss: Boolean = false, // true = victory/XP message shown, waiting for player click to end battle
    // Spawner identity — links this enemy back to its overworld spawner
    var spawnerId: Int = -1,
    var spawnerMapId: Int = -1,
)
