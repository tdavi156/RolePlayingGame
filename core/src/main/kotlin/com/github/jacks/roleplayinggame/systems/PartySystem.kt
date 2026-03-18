package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.configurations.ItemCategory
import com.github.jacks.roleplayinggame.events.AddCharacterToPartyEvent
import com.github.jacks.roleplayinggame.events.PartyUpdatedEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.quillraven.fleks.IntervalSystem
import ktx.preferences.flush
import ktx.preferences.get
import ktx.preferences.set

data class CharacterData(
    val characterId: Int,
    val characterName: String,
    // Stats
    var currentHp: Float,
    var maxHp: Float,
    var currentMana: Float,
    var maxMana: Float,
    var attack: Float,
    var defense: Float,
    var attackSpeed: Float,
    var moveSpeed: Float,
    // Progression
    var level: Int,
    var exp: Int,
    var skillPoints: Int,
    var abilityPoints: Int,
    var skillPointsInvestedAttack: Int,
    var skillPointsInvestedDefense: Int,
    // Abilities
    var unlockedAbilityIds: MutableSet<Int> = mutableSetOf(),
    // Equipment: null means nothing equipped in that slot
    var equippedItems: MutableMap<ItemCategory, Int?> = mutableMapOf(
        ItemCategory.HELMET to null,
        ItemCategory.WEAPON to null,
        ItemCategory.ARMOR  to null,
        ItemCategory.BOOTS  to null,
    ),
    // Party state
    var isUnlocked: Boolean = false,
)

class PartySystem(
    private val gameStage: Stage,
) : IntervalSystem(), EventListener {

    private val preferences: Preferences by lazy { Gdx.app.getPreferences("rolePlayingGamePrefs") }

    val characterDataMap: MutableMap<Int, CharacterData> = mutableMapOf()
    var activeOverworldCharacterId: Int = 1
    val combatSlots: MutableList<Int> = mutableListOf()

    override fun onTick() {
        // no-op — data-holder system, driven by events and direct calls
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is AddCharacterToPartyEvent -> {
                unlockCharacter(event.characterId)
                // Permanently remove the NPC from the world
                world.remove(event.npcEntity)
                // Notify all UIs to refresh
                gameStage.fire(PartyUpdatedEvent())
                return true
            }
            else -> return false
        }
    }

    // ── Accessors ──────────────────────────────────────────────────────────────

    fun getCharacterData(id: Int): CharacterData =
        characterDataMap[id] ?: error("No CharacterData found for characterId=$id")

    fun updateCharacterData(id: Int, block: CharacterData.() -> Unit) {
        val data = getCharacterData(id)
        data.block()
        saveCharacterData(id)
    }

    fun getUnlockedCharacters(): List<CharacterData> =
        characterDataMap.values.filter { it.isUnlocked }.sortedBy { it.characterId }

    // ── Party management ───────────────────────────────────────────────────────

    fun unlockCharacter(id: Int) {
        val data = characterDataMap[id] ?: return
        if (data.isUnlocked) return
        data.isUnlocked = true
        if (combatSlots.size < MAX_COMBAT_SLOTS && id !in combatSlots) {
            combatSlots.add(id)
            saveCombatSlots()
        }
        saveCharacterData(id)
    }

    // ── Persistence ────────────────────────────────────────────────────────────

    fun saveCharacterData(id: Int) {
        val data = characterDataMap[id] ?: return
        val prefix = "$KEY_CHARACTER_DATA_PREFIX${id}_"
        preferences.flush {
            this["${prefix}current_hp"]         = data.currentHp
            this["${prefix}max_hp"]             = data.maxHp
            this["${prefix}current_mana"]       = data.currentMana
            this["${prefix}max_mana"]           = data.maxMana
            this["${prefix}attack"]             = data.attack
            this["${prefix}defense"]            = data.defense
            this["${prefix}attack_speed"]       = data.attackSpeed
            this["${prefix}move_speed"]         = data.moveSpeed
            this["${prefix}level"]              = data.level
            this["${prefix}exp"]                = data.exp
            this["${prefix}skill_points"]       = data.skillPoints
            this["${prefix}ability_points"]     = data.abilityPoints
            this["${prefix}invested_attack"]    = data.skillPointsInvestedAttack
            this["${prefix}invested_defense"]   = data.skillPointsInvestedDefense
            this["${prefix}unlocked_abilities"] = data.unlockedAbilityIds.joinToString(",")
            this["${prefix}equipped_helmet"]    = data.equippedItems[ItemCategory.HELMET] ?: -1
            this["${prefix}equipped_weapon"]    = data.equippedItems[ItemCategory.WEAPON] ?: -1
            this["${prefix}equipped_armor"]     = data.equippedItems[ItemCategory.ARMOR]  ?: -1
            this["${prefix}equipped_boots"]     = data.equippedItems[ItemCategory.BOOTS]  ?: -1
            this["${prefix}is_unlocked"]        = data.isUnlocked
        }
    }

    fun loadCharacterData(id: Int): CharacterData? {
        val prefix = "$KEY_CHARACTER_DATA_PREFIX${id}_"
        if (!preferences.contains("${prefix}level")) return null
        val raw = preferences.getString("${prefix}unlocked_abilities", "")
        val abilities: MutableSet<Int> = if (raw.isBlank()) mutableSetOf()
        else raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toMutableSet()
        fun equipped(slot: String): Int? =
            preferences.getInteger("${prefix}equipped_$slot", -1).takeIf { it >= 0 }
        return CharacterData(
            characterId                  = id,
            characterName                = characterDataMap[id]?.characterName ?: "",
            currentHp                    = preferences.getFloat("${prefix}current_hp"),
            maxHp                        = preferences.getFloat("${prefix}max_hp"),
            currentMana                  = preferences.getFloat("${prefix}current_mana"),
            maxMana                      = preferences.getFloat("${prefix}max_mana"),
            attack                       = preferences.getFloat("${prefix}attack"),
            defense                      = preferences.getFloat("${prefix}defense"),
            attackSpeed                  = preferences.getFloat("${prefix}attack_speed"),
            moveSpeed                    = preferences.getFloat("${prefix}move_speed"),
            level                        = preferences.getInteger("${prefix}level"),
            exp                          = preferences.getInteger("${prefix}exp"),
            skillPoints                  = preferences.getInteger("${prefix}skill_points"),
            abilityPoints                = preferences.getInteger("${prefix}ability_points"),
            skillPointsInvestedAttack    = preferences.getInteger("${prefix}invested_attack"),
            skillPointsInvestedDefense   = preferences.getInteger("${prefix}invested_defense"),
            unlockedAbilityIds           = abilities,
            equippedItems                = mutableMapOf(
                ItemCategory.HELMET to equipped("helmet"),
                ItemCategory.WEAPON to equipped("weapon"),
                ItemCategory.ARMOR  to equipped("armor"),
                ItemCategory.BOOTS  to equipped("boots"),
            ),
            isUnlocked                   = preferences.getBoolean("${prefix}is_unlocked", false),
        )
    }

    fun saveCombatSlots() {
        preferences.flush {
            this[KEY_COMBAT_SLOTS] = combatSlots.joinToString(",")
        }
    }

    fun saveActiveCharacter() {
        preferences.flush {
            this[KEY_ACTIVE_CHARACTER] = activeOverworldCharacterId
        }
    }

    companion object {
        const val KEY_CHARACTER_DATA_PREFIX = "char_data_"
        const val KEY_ACTIVE_CHARACTER      = "active_character"
        const val KEY_COMBAT_SLOTS          = "combat_slots"
        const val MAX_COMBAT_SLOTS          = 3
    }
}
