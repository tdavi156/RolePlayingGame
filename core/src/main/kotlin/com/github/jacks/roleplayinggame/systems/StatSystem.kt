package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.InventoryComponent
import com.github.jacks.roleplayinggame.components.PhysicsComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.configurations.ConsumableStatType
import com.github.jacks.roleplayinggame.configurations.consumableItemById
import com.github.jacks.roleplayinggame.configurations.equipmentItemById
import com.github.jacks.roleplayinggame.events.CombatSpeedChangedEvent
import com.github.jacks.roleplayinggame.events.EquipItemEvent
import com.github.jacks.roleplayinggame.events.FloatingTextEvent
import com.github.jacks.roleplayinggame.events.GainAbilityPointEvent
import com.github.jacks.roleplayinggame.events.GainSkillPointEvent
import com.github.jacks.roleplayinggame.events.LevelUpEvent
import com.github.jacks.roleplayinggame.events.SkillPointsChangedEvent
import com.github.jacks.roleplayinggame.events.SkillPointsSaveEvent
import com.github.jacks.roleplayinggame.events.SkillViewClosedEvent
import com.github.jacks.roleplayinggame.events.UseConsumableEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.ui.Fonts
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IntervalSystem

class StatSystem(
    private val gameStage: Stage,
) : IntervalSystem(), EventListener {

    private val playerFamily      by lazy { world.family(allOf = arrayOf(PlayerComponent::class)) }
    private val statMapper        by lazy { world.mapper<StatComponent>() }
    private val inventoryMapper   by lazy { world.mapper<InventoryComponent>() }
    private val physicsComponents by lazy { world.mapper<PhysicsComponent>() }
    private val playerMapper      by lazy { world.mapper<PlayerComponent>() }

    /** Guard flag — set true while writing CharacterData to prevent cascading save loops. */
    private var isSyncing = false

    override fun onTick() = Unit

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun partySystem(): PartySystem = world.system<PartySystem>()

    private fun characterIdOf(entity: Entity): Int? =
        playerMapper.getOrNull(entity)?.characterId

    /**
     * Writes HP and mana from [stat] back to the matching CharacterData.
     * No-op if isSyncing is already true (prevents cascading writes).
     */
    fun syncHpMana(entity: Entity, stat: StatComponent) {
        if (isSyncing) return
        val id = characterIdOf(entity) ?: return
        isSyncing = true
        try {
            partySystem().updateCharacterData(id) {
                currentHp   = stat.currentHealth
                currentMana = stat.currentMana
            }
        } finally {
            isSyncing = false
        }
    }

    /** Writes full stat snapshot back to CharacterData after a recalc. */
    private fun syncFullStats(entity: Entity, stat: StatComponent) {
        if (isSyncing) return
        val id = characterIdOf(entity) ?: return
        isSyncing = true
        try {
            partySystem().updateCharacterData(id) {
                currentHp                  = stat.currentHealth
                maxHp                      = stat.maxHealth
                currentMana                = stat.currentMana
                maxMana                    = stat.maxMana
                attack                     = stat.attackDamage
                defense                    = stat.defense
                attackSpeed                = stat.attackSpeed
                moveSpeed                  = stat.moveSpeed
                this.level                 = stat.level
                exp                        = stat.experience
                skillPoints                = stat.skillPoints
                abilityPoints              = stat.abilityPoints
                skillPointsInvestedAttack  = stat.skillPointsInvestedAttack
                skillPointsInvestedDefense = stat.skillPointsInvestedDefense
            }
        } finally {
            isSyncing = false
        }
    }

    // ── Public stat helpers (used by ShopSystem / InventorySystem) ─────────────

    fun isStatFull(characterIndex: Int, statType: ConsumableStatType): Boolean {
        val entity = playerFamily.firstOrNull() ?: return true
        val stat = statMapper.getOrNull(entity) ?: return true
        return when (statType) {
            ConsumableStatType.HEALTH -> stat.currentHealth >= stat.maxHealth
            ConsumableStatType.MANA   -> stat.currentMana   >= stat.maxMana
        }
    }

    fun computeActualRecovery(characterIndex: Int, item: com.github.jacks.roleplayinggame.configurations.ConsumableItemData): Int {
        val entity = playerFamily.firstOrNull() ?: return 0
        val stat = statMapper.getOrNull(entity) ?: return 0
        return when (item.statType) {
            ConsumableStatType.HEALTH ->
                item.statValue.toFloat().coerceAtMost(stat.maxHealth - stat.currentHealth).toInt()
            ConsumableStatType.MANA ->
                item.statValue.toFloat().coerceAtMost(stat.maxMana - stat.currentMana).toInt()
        }
    }

    // ── Event handling ─────────────────────────────────────────────────────────

    override fun handle(event: Event): Boolean {
        when (event) {
            is EquipItemEvent -> {
                val entity  = playerFamily.firstOrNull() ?: return false
                val stat    = statMapper.getOrNull(entity) ?: return false
                val inv     = inventoryMapper.getOrNull(entity) ?: return false
                val newItem = equipmentItemById(event.itemId) ?: return false

                val oldId = inv.equippedItems[newItem.category]
                val speedBefore = stat.attackSpeed
                if (oldId != null) {
                    equipmentItemById(oldId)?.stats?.forEach { (statType, value) ->
                        stat.decreaseStat(statType.toStatType(), value.toFloat())
                    }
                }

                inv.equippedItems[newItem.category] = newItem.id
                newItem.stats.forEach { (statType, value) ->
                    stat.increaseStat(statType.toStatType(), value.toFloat())
                }

                stat.currentHealth = stat.currentHealth.coerceAtMost(stat.maxHealth)
                stat.currentMana   = stat.currentMana.coerceAtMost(stat.maxMana)

                // Write back full stats + equipped items
                syncFullStats(entity, stat)
                val charId = characterIdOf(entity)
                if (charId != null && !isSyncing) {
                    isSyncing = true
                    try {
                        partySystem().updateCharacterData(charId) {
                            equippedItems[newItem.category] = newItem.id
                        }
                    } finally {
                        isSyncing = false
                    }
                }

                if (stat.attackSpeed != speedBefore) {
                    gameStage.fire(CombatSpeedChangedEvent(entity))
                }
                return true
            }

            is UseConsumableEvent -> {
                val entity = playerFamily.firstOrNull() ?: return false
                val stat   = statMapper.getOrNull(entity) ?: return false
                val item   = consumableItemById(event.itemId) ?: return false

                val pos = physicsComponents.getOrNull(entity)?.body?.position
                when (item.statType) {
                    ConsumableStatType.HEALTH -> {
                        val actual = item.statValue.toFloat().coerceAtMost(stat.maxHealth - stat.currentHealth).toInt()
                        stat.currentHealth = (stat.currentHealth + item.statValue).coerceAtMost(stat.maxHealth)
                        if (pos != null) gameStage.fire(FloatingTextEvent(pos, "+$actual HP", Fonts.DAMAGE))
                    }
                    ConsumableStatType.MANA -> {
                        val actual = item.statValue.toFloat().coerceAtMost(stat.maxMana - stat.currentMana).toInt()
                        stat.currentMana = (stat.currentMana + item.statValue).coerceAtMost(stat.maxMana)
                        if (pos != null) gameStage.fire(FloatingTextEvent(pos, "+$actual MP", Fonts.DAMAGE))
                    }
                }

                syncHpMana(entity, stat)
                world.system<InventorySystem>().removeItem(event.itemId)
                return true
            }

            is LevelUpEvent -> {
                val pos: Vector2? = physicsComponents.getOrNull(event.entity)?.body?.position
                if (pos != null) {
                    gameStage.fire(FloatingTextEvent(Vector2(pos), "LEVEL UP!", Fonts.DAMAGE))
                }
                val stat = statMapper.getOrNull(event.entity) ?: return true
                syncFullStats(event.entity, stat)
                return true
            }

            is GainSkillPointEvent -> {
                val stat = statMapper.getOrNull(event.entity) ?: return false
                stat.skillPoints++
                val id = characterIdOf(event.entity) ?: return true
                if (!isSyncing) {
                    isSyncing = true
                    try {
                        partySystem().updateCharacterData(id) { skillPoints = stat.skillPoints }
                    } finally {
                        isSyncing = false
                    }
                }
                return true
            }

            is GainAbilityPointEvent -> {
                val stat = statMapper.getOrNull(event.entity) ?: return false
                stat.abilityPoints++
                val id = characterIdOf(event.entity) ?: return true
                if (!isSyncing) {
                    isSyncing = true
                    try {
                        partySystem().updateCharacterData(id) { abilityPoints = stat.abilityPoints }
                    } finally {
                        isSyncing = false
                    }
                }
                return true
            }

            is SkillPointsSaveEvent -> {
                val stat = statMapper.getOrNull(event.entity) ?: return false
                stat.skillPointsInvestedAttack  += event.pendingAttackPoints
                stat.skillPointsInvestedDefense += event.pendingDefensePoints
                stat.skillPoints -= (event.pendingAttackPoints + event.pendingDefensePoints)
                stat.attackDamage += event.pendingAttackPoints * 2f
                stat.defense      += event.pendingDefensePoints * 1f

                syncFullStats(event.entity, stat)
                gameStage.fire(SkillPointsChangedEvent(event.entity))
                gameStage.fire(SkillViewClosedEvent())
                return true
            }

            else -> return false
        }
    }
}
