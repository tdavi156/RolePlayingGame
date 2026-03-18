package com.github.jacks.roleplayinggame.configurations

data class BaseStats(
    val maxHp: Int,
    val maxMana: Int,
    val attack: Int,
    val defense: Int,
    val attackSpeed: Float,
    val moveSpeed: Float,
)

data class CharacterConfig(
    val characterId: Int,
    val characterName: String,
    val abilityTreeId: Int,
    val baseStats: BaseStats,
    val atlasKey: String,
    val portraitKey: String,
)

val CHARACTER_1_CONFIG = CharacterConfig(
    characterId = 1,
    characterName = "Warrior",
    abilityTreeId = 1,
    baseStats = BaseStats(maxHp = 30, maxMana = 20, attack = 5, defense = 1, attackSpeed = 1f, moveSpeed = 1f),
    atlasKey = "player",
    portraitKey = "portrait_1",
)

val CHARACTER_2_CONFIG = CharacterConfig(
    characterId = 2,
    characterName = "Cleric",
    abilityTreeId = 2,
    baseStats = BaseStats(maxHp = 20, maxMana = 40, attack = 3, defense = 1, attackSpeed = 1.5f, moveSpeed = 1f),
    atlasKey = "player",
    portraitKey = "portrait_2",
)

val CHARACTER_3_CONFIG = CharacterConfig(
    characterId = 3,
    characterName = "Ranger",
    abilityTreeId = 3,
    baseStats = BaseStats(maxHp = 50, maxMana = 10, attack = 4, defense = 3, attackSpeed = 0.8f, moveSpeed = 1f),
    atlasKey = "player",
    portraitKey = "portrait_3",
)

val CHARACTER_CONFIGS: Map<Int, CharacterConfig> = mapOf(
    1 to CHARACTER_1_CONFIG,
    2 to CHARACTER_2_CONFIG,
    3 to CHARACTER_3_CONFIG,
    // CharacterConfig(
    //     characterId = 4,
    //     characterName = "Character Name",
    //     abilityTreeId = 4,
    //     baseStats = BaseStats(maxHp=80, maxMana=30, attack=12, defense=8, attackSpeed=1f, moveSpeed=1f),
    //     atlasKey = "character_4",
    //     portraitKey = "portrait_4"
    // ),
)
