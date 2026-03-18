package com.github.jacks.roleplayinggame.configurations

/**
 * A battle composition defines which enemy types appear in a single encounter and their
 * positional slots. unit1 is always required; unit2 and unit3 are optional.
 * Slot order determines which spawner position each enemy occupies on the battle map.
 */
data class BattleComp(
    val id: Int,
    val unit1: EnemyType,
    val unit2: EnemyType? = null,
    val unit3: EnemyType? = null,
)

// ---------------------------------------------------------------------------
// Fixed composition registry — looked up by id when battleCompId is non-null
// ---------------------------------------------------------------------------

val BATTLE_COMPS: Map<Int, BattleComp> = listOf(
    BattleComp(id = 1, unit1 = EnemyType.GREEN_SLIME),
    BattleComp(id = 2, unit1 = EnemyType.GREEN_SLIME, unit2 = EnemyType.GREEN_SLIME),
    BattleComp(id = 3, unit1 = EnemyType.GREEN_SLIME, unit2 = EnemyType.GREEN_SLIME, unit3 = EnemyType.GREEN_SLIME),
    BattleComp(id = 4, unit1 = EnemyType.GREEN_SLIME, unit2 = EnemyType.GREEN_SLIME, unit3 = EnemyType.BLUE_SLIME),
    BattleComp(id = 5, unit1 = EnemyType.BLUE_SLIME),   // boss comp placeholder
    // Template for adding new comps:
    // BattleComp(
    //     id = 6,
    //     unit1 = EnemyType.GREEN_SLIME,
    //     unit2 = EnemyType.BLUE_SLIME,
    //     unit3 = null
    // ),
).associateBy { it.id }

// ---------------------------------------------------------------------------
// Random roll tables — used when battleCompId is null on the triggering spawner
// ---------------------------------------------------------------------------

val RANDOM_COMPS: Map<EnemyType, List<BattleComp>> = mapOf(
    EnemyType.GREEN_SLIME to listOf(
        BattleComp(id = 1, unit1 = EnemyType.GREEN_SLIME),
        BattleComp(id = 2, unit1 = EnemyType.GREEN_SLIME, unit2 = EnemyType.GREEN_SLIME),
        BattleComp(id = 3, unit1 = EnemyType.GREEN_SLIME, unit2 = EnemyType.GREEN_SLIME, unit3 = EnemyType.GREEN_SLIME),
    ),
    EnemyType.BLUE_SLIME to listOf(
        BattleComp(id = 5, unit1 = EnemyType.BLUE_SLIME),
        BattleComp(id = 101, unit1 = EnemyType.BLUE_SLIME, unit2 = EnemyType.GREEN_SLIME),
        BattleComp(id = 102, unit1 = EnemyType.BLUE_SLIME, unit2 = EnemyType.BLUE_SLIME),
    ),
    EnemyType.RED_SLIME to listOf(
        BattleComp(id = 103, unit1 = EnemyType.RED_SLIME),
        BattleComp(id = 104, unit1 = EnemyType.RED_SLIME, unit2 = EnemyType.GREEN_SLIME),
    ),
)
