package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.InventoryComponent
import com.github.jacks.roleplayinggame.saveManager.CharacterData
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
import com.github.jacks.roleplayinggame.saveManager.SaveManager
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IntervalSystem

class StatSystem(
    private val gameStage: Stage,
    private val saveManager: SaveManager,
) : IntervalSystem(), EventListener {

    private val playerFamily      by lazy { world.family(allOf = arrayOf(PlayerComponent::class)) }
    private val statMapper        by lazy { world.mapper<StatComponent>() }
    private val inventoryMapper   by lazy { world.mapper<InventoryComponent>() }
    private val physicsComponents by lazy { world.mapper<PhysicsComponent>() }
    private val playerMapper      by lazy { world.mapper<PlayerComponent>() }

    override fun onTick() = Unit

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun partySystem(): PartySystem = world.system<PartySystem>()

    private fun characterIdOf(entity: Entity): Int? =
        playerMapper.getOrNull(entity)?.characterId

    /**
     * Saves the CharacterData (HP, mana, skills, etc.) back to preferences.
     * Since StatComponent.stats IS CharacterData, no data copy is needed.
     */
    fun syncHpMana(entity: Entity) {
        val id = characterIdOf(entity) ?: return
        saveManager.gatherAndSave(world)
    }

    // ── Public stat helpers (used by ShopSystem / InventorySystem) ─────────────

    fun isStatFull(characterIndex: Int, statType: ConsumableStatType): Boolean {
        val entity = playerFamily.firstOrNull() ?: return true
        val stat = statMapper.getOrNull(entity) ?: return true
        return when (statType) {
            ConsumableStatType.HEALTH -> stat.stats.currentHealth >= stat.stats.maxHealth
            ConsumableStatType.MANA   -> stat.stats.currentMana   >= stat.stats.maxMana
        }
    }

    fun computeActualRecovery(characterIndex: Int, item: com.github.jacks.roleplayinggame.configurations.ConsumableItemData): Int {
        val entity = playerFamily.firstOrNull() ?: return 0
        val stat = statMapper.getOrNull(entity) ?: return 0
        return when (item.statType) {
            ConsumableStatType.HEALTH ->
                item.statValue.toFloat().coerceAtMost(stat.stats.maxHealth - stat.stats.currentHealth).toInt()
            ConsumableStatType.MANA ->
                item.statValue.toFloat().coerceAtMost(stat.stats.maxMana - stat.stats.currentMana).toInt()
        }
    }

    // ── Event handling ─────────────────────────────────────────────────────────

    override fun handle(event: Event): Boolean {
        when (event) {
            is EquipItemEvent -> {
                val entity   = playerFamily.firstOrNull() ?: return false
                val stat     = statMapper.getOrNull(entity) ?: return false
                val charData = stat.stats as? CharacterData ?: return false
                val inv      = inventoryMapper.getOrNull(entity) ?: return false
                val newItem  = equipmentItemById(event.itemId) ?: return false

                val oldId = inv.equippedItems[newItem.category]
                val speedBefore = charData.attackSpeed
                if (oldId != null) {
                    equipmentItemById(oldId)?.stats?.forEach { (statType, value) ->
                        charData.decreaseStat(statType.toStatType(), value.toFloat())
                    }
                }

                inv.equippedItems[newItem.category] = newItem.id
                charData.equippedItems[newItem.category] = newItem.id
                newItem.stats.forEach { (statType, value) ->
                    charData.increaseStat(statType.toStatType(), value.toFloat())
                }

                charData.currentHealth = charData.currentHealth.coerceAtMost(charData.maxHealth)
                charData.currentMana   = charData.currentMana.coerceAtMost(charData.maxMana)

                val charId = characterIdOf(entity)
                if (charId != null) {
                    saveManager.gatherAndSave(world)
                }

                if (charData.attackSpeed != speedBefore) {
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
                        val actual = item.statValue.toFloat().coerceAtMost(stat.stats.maxHealth - stat.stats.currentHealth).toInt()
                        stat.stats.currentHealth = (stat.stats.currentHealth + item.statValue).coerceAtMost(stat.stats.maxHealth)
                        if (pos != null) gameStage.fire(FloatingTextEvent(pos, "+$actual HP", Fonts.DAMAGE))
                    }
                    ConsumableStatType.MANA -> {
                        val actual = item.statValue.toFloat().coerceAtMost(stat.stats.maxMana - stat.stats.currentMana).toInt()
                        stat.stats.currentMana = (stat.stats.currentMana + item.statValue).coerceAtMost(stat.stats.maxMana)
                        if (pos != null) gameStage.fire(FloatingTextEvent(pos, "+$actual MP", Fonts.DAMAGE))
                    }
                }

                syncHpMana(entity)
                world.system<InventorySystem>().removeItem(event.itemId)
                return true
            }

            is LevelUpEvent -> {
                val pos: Vector2? = physicsComponents.getOrNull(event.entity)?.body?.position
                if (pos != null) {
                    gameStage.fire(FloatingTextEvent(Vector2(pos), "LEVEL UP!", Fonts.DAMAGE))
                }
                val id = characterIdOf(event.entity) ?: return true
                saveManager.gatherAndSave(world)
                return true
            }

            is GainSkillPointEvent -> {
                val stat     = statMapper.getOrNull(event.entity) ?: return false
                val charData = stat.stats as? CharacterData ?: return false
                charData.currentSkillPoints++
                charData.totalSkillPoints++
                val id = characterIdOf(event.entity) ?: return true
                saveManager.gatherAndSave(world)
                return true
            }

            is GainAbilityPointEvent -> {
                val stat     = statMapper.getOrNull(event.entity) ?: return false
                val charData = stat.stats as? CharacterData ?: return false
                charData.currentAbilityPoints++
                charData.totalAbilityPoints++
                val id = characterIdOf(event.entity) ?: return true
                saveManager.gatherAndSave(world)
                return true
            }

            is SkillPointsSaveEvent -> {
                val stat     = statMapper.getOrNull(event.entity) ?: return false
                val charData = stat.stats as? CharacterData ?: return false
                val totalPending = event.pendingStamina + event.pendingStrength + event.pendingAgility +
                                   event.pendingIntelligence + event.pendingWisdom
                charData.stamina      += event.pendingStamina
                charData.strength     += event.pendingStrength
                charData.agility      += event.pendingAgility
                charData.intelligence += event.pendingIntelligence
                charData.wisdom       += event.pendingWisdom
                charData.currentSkillPoints -= totalPending
                charData.recalculateDerivedStats()
                val id = characterIdOf(event.entity) ?: return true
                saveManager.gatherAndSave(world)
                gameStage.fire(SkillPointsChangedEvent(event.entity))
                gameStage.fire(SkillViewClosedEvent())
                return true
            }

            else -> return false
        }
    }
}
