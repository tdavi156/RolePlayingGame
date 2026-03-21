package com.github.jacks.roleplayinggame.saveManager

import com.github.jacks.roleplayinggame.components.StatType
import com.github.jacks.roleplayinggame.components.StatsProvider
import com.github.jacks.roleplayinggame.configurations.ItemCategory

data class CharacterData(
    val characterId: Int,
    val characterName: String,

    // ---- Overworld Stats ----
    var currentLevel: Int = 1,
    var currentEXP: Int = 0,
    var totalEXP: Int = 0,
    override var moveSpeed: Float = 1f,
    var currentSkillPoints: Int = 0,
    var totalSkillPoints: Int = 0,
    var currentAbilityPoints: Int = 0,
    var totalAbilityPoints: Int = 0,

    // ---- Skill Stats (invested points — affect BattleStats via recalculation) ----
    var stamina: Int = 0,
    var strength: Int = 0,
    var agility: Int = 0,
    var intelligence: Int = 0,
    var wisdom: Int = 0,

    // ---- Base Battle Stats (sourced from CharacterConfigs.kt — do not modify directly) ----
    val baseMaxHealth: Float = 0f,
    val baseMaxMana: Float = 0f,
    val baseAttackSpeed: Float = 1f,
    val baseAccuracy: Float = 1f,
    val baseEvasion: Float = 0f,
    val baseAttackDamage: Float = 0f,
    val baseAttackDamagePercent: Float = 1f,
    val baseSpellDamage: Float = 0f,
    val baseSpellDamagePercent: Float = 1f,
    val baseDefense: Float = 0f,
    val baseDefensePercent: Float = 1f,
    val baseResistance: Float = 0f,
    val baseResistancePercent: Float = 1f,

    // ---- Derived Battle Stats (computed from base + skill investment — read these at runtime) ----
    // Equipment stat bonuses will be added as additional terms in recalculateDerivedStats() in a future feature.
    override var maxHealth: Float = baseMaxHealth,
    override var currentHealth: Float = maxHealth,
    override var maxMana: Float = baseMaxMana,
    override var currentMana: Float = maxMana,
    override var attackSpeed: Float = baseAttackSpeed,
    override var accuracy: Float = baseAccuracy,
    override var evasion: Float = baseEvasion,
    override var attackDamage: Float = baseAttackDamage,
    override var attackDamagePercent: Float = baseAttackDamagePercent,
    override var spellDamage: Float = baseSpellDamage,
    override var spellDamagePercent: Float = baseSpellDamagePercent,
    override var defense: Float = baseDefense,
    override var defensePercent: Float = baseDefensePercent,
    override var resistance: Float = baseResistance,
    override var resistancePercent: Float = baseResistancePercent,

    // ---- Ability / Equipment state ----
    var unlockedAbilityIds: MutableSet<Int> = mutableSetOf(),
    var equippedItems: MutableMap<ItemCategory, Int?> = mutableMapOf(
        ItemCategory.HELMET to null,
        ItemCategory.WEAPON to null,
        ItemCategory.ARMOR  to null,
        ItemCategory.BOOTS  to null,
    ),
    var isUnlocked: Boolean = false,
) : StatsProvider() {

    /**
     * XP required to level up from the current level.
     * Formula: req(n) = n * 50 * (1.15 ^ n)
     */
    val experienceToNextLevel: Int
        get() = (currentLevel * 50 * Math.pow(1.15, currentLevel.toDouble())).toInt()

    /**
     * Add XP and process any level-ups. Returns the number of levels gained.
     * Each level grants +10 maxHP, +5 maxMana only — no other passive stat boosts.
     * Callers are responsible for firing LevelUpEvent / GainSkillPointEvent / GainAbilityPointEvent.
     */
    fun gainExperience(amount: Int): Int {
        currentEXP += amount
        totalEXP += amount
        var levelsGained = 0
        while (currentEXP >= experienceToNextLevel) {
            currentEXP -= experienceToNextLevel
            currentLevel++
            levelsGained++
            maxHealth += 10f
            currentHealth = (currentHealth + 10f).coerceAtMost(maxHealth)
            maxMana += 5f
            currentMana = (currentMana + 5f).coerceAtMost(maxMana)
        }
        return levelsGained
    }

    /**
     * Recomputes all derived BattleStats from base values + current skill investment.
     * Call this after investing skill points — never for temporary combat deltas.
     * Equipment stat bonuses will be added as additional terms here in a future feature.
     */
    fun recalculateDerivedStats() {
        maxHealth     = baseMaxHealth + (stamina * 10f)
        attackDamage  = baseAttackDamage + (strength * 3f)
        accuracy      = baseAccuracy + (agility * 0.05f)
        evasion       = baseEvasion + (agility * 0.05f)
        attackSpeed   = baseAttackSpeed + (agility * 1f)
        spellDamage   = baseSpellDamage + (intelligence * 3f)
        maxMana       = baseMaxMana + (intelligence * 5f) + (wisdom * 3f)
        resistance    = baseResistance + (wisdom * 2f)

        // Clamp current values to new maximums
        currentHealth = currentHealth.coerceAtMost(maxHealth)
        currentMana   = currentMana.coerceAtMost(maxMana)
    }

    /** Apply an equipment stat bonus additively. Used by StatSystem on equip/unequip. */
    fun increaseStat(statType: StatType, value: Float) {
        when (statType) {
            StatType.UNDEFINED     -> {}
            StatType.CURRENT_HEALTH -> currentHealth = (currentHealth + value).coerceAtMost(maxHealth)
            StatType.MAX_HEALTH    -> { maxHealth += value; currentHealth = (currentHealth + value).coerceAtMost(maxHealth) }
            StatType.ATTACK_DAMAGE -> attackDamage += value
            StatType.DEFENSE       -> defense += value
            StatType.MOVE_SPEED    -> moveSpeed += value
            StatType.ATTACK_SPEED  -> attackSpeed += value
        }
    }

    /** Remove an equipment stat bonus. Used by StatSystem on equip/unequip. */
    fun decreaseStat(statType: StatType, value: Float) {
        when (statType) {
            StatType.UNDEFINED     -> {}
            StatType.CURRENT_HEALTH -> currentHealth -= value
            StatType.MAX_HEALTH    -> { maxHealth -= value; currentHealth -= value }
            StatType.ATTACK_DAMAGE -> attackDamage -= value
            StatType.DEFENSE       -> defense -= value
            StatType.MOVE_SPEED    -> moveSpeed -= value
            StatType.ATTACK_SPEED  -> attackSpeed -= value
        }
    }
}
