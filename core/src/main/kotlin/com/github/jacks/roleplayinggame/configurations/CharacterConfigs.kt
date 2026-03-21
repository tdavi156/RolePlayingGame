package com.github.jacks.roleplayinggame.configurations

/**
 * Base battle stat template for each character, sourced from this config at init time.
 * All values are Float. Accuracy defaults to 1f and evasion to 0f for all players per design decision.
 */
data class BaseStats(
    val baseMaxHealth: Float,
    val baseMaxMana: Float,
    val baseAttackSpeed: Float,
    val baseAccuracy: Float = 1f,
    val baseEvasion: Float = 0f,
    val baseAttackDamage: Float,
    val baseAttackDamagePercent: Float = 1f,
    val baseSpellDamage: Float = 0f,
    val baseSpellDamagePercent: Float = 1f,
    val baseDefense: Float,
    val baseDefensePercent: Float = 1f,
    val baseResistance: Float = 0f,
    val baseResistancePercent: Float = 1f,
    val moveSpeed: Float = 1f,
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
    baseStats = BaseStats(
        baseMaxHealth = 30f,
        baseMaxMana = 20f,
        baseAttackSpeed = 1f,
        baseAttackDamage = 5f,
        baseDefense = 1f,
    ),
    atlasKey = "player",
    portraitKey = "portrait_1",
)

val CHARACTER_2_CONFIG = CharacterConfig(
    characterId = 2,
    characterName = "Cleric",
    abilityTreeId = 2,
    baseStats = BaseStats(
        baseMaxHealth = 20f,
        baseMaxMana = 40f,
        baseAttackSpeed = 1.5f,
        baseAttackDamage = 3f,
        baseDefense = 1f,
    ),
    atlasKey = "player",
    portraitKey = "portrait_2",
)

val CHARACTER_3_CONFIG = CharacterConfig(
    characterId = 3,
    characterName = "Ranger",
    abilityTreeId = 3,
    baseStats = BaseStats(
        baseMaxHealth = 50f,
        baseMaxMana = 10f,
        baseAttackSpeed = 0.8f,
        baseAttackDamage = 4f,
        baseDefense = 3f,
    ),
    atlasKey = "player",
    portraitKey = "portrait_3",
)

val CHARACTER_CONFIGS: Map<Int, CharacterConfig> = mapOf(
    1 to CHARACTER_1_CONFIG,
    2 to CHARACTER_2_CONFIG,
    3 to CHARACTER_3_CONFIG,
)
