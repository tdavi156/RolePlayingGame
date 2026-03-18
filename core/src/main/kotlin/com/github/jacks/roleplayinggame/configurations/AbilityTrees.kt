package com.github.jacks.roleplayinggame.configurations

sealed class AbilityEffect {
    data class DamageEnemy(val amount: Int) : AbilityEffect()
    data class HealSelf(val amount: Int) : AbilityEffect()
}

data class AbilityNode(
    val id: Int,
    val name: String,
    val description: String,
    val manaCost: Int,
    val abilityPointCost: Int = 1,
    val prerequisiteIds: List<Int>,
    val atlasKey: String,
    val effect: AbilityEffect,
)

data class AbilityTree(
    val characterId: Int,
    val nodes: List<AbilityNode>,
)

val ABILITY_TREE_CHARACTER_1 = AbilityTree(
    characterId = 1,
    nodes = listOf(
        AbilityNode(
            id = 1,
            name = "Ability 1",
            description = "Strike the enemy for bonus damage.",
            manaCost = 2,
            prerequisiteIds = emptyList(),
            atlasKey = "ability_1",
            effect = AbilityEffect.DamageEnemy(5),
        ),
        AbilityNode(
            id = 2,
            name = "Ability 2",
            description = "Restore some HP.",
            manaCost = 3,
            prerequisiteIds = listOf(1),
            atlasKey = "ability_2",
            effect = AbilityEffect.HealSelf(5),
        ),
        AbilityNode(
            id = 3,
            name = "Ability 3",
            description = "Unleash a powerful strike.",
            manaCost = 10,
            prerequisiteIds = listOf(2),
            atlasKey = "ability_3",
            effect = AbilityEffect.DamageEnemy(20),
        ),
        // AbilityNode(
        //     id = 4,
        //     name = "Ability Name",
        //     description = "Short description of what it does.",
        //     manaCost = 5,
        //     prerequisiteIds = listOf(3),
        //     atlasKey = "ability_4",
        //     effect = AbilityEffect.DamageEnemy(15)
        // ),
    ),
)

val ABILITY_TREES: Map<Int, AbilityTree> = mapOf(
    1 to ABILITY_TREE_CHARACTER_1,
)
