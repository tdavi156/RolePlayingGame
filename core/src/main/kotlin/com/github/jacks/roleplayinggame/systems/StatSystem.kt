package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
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
import com.github.jacks.roleplayinggame.events.EquipItemEvent
import com.github.jacks.roleplayinggame.events.FloatingTextEvent
import com.github.jacks.roleplayinggame.events.AbilityPointsSaveEvent
import com.github.jacks.roleplayinggame.events.GainAbilityPointEvent
import com.github.jacks.roleplayinggame.events.GainSkillPointEvent
import com.github.jacks.roleplayinggame.events.LevelUpEvent
import com.github.jacks.roleplayinggame.events.SkillPointsChangedEvent
import com.github.jacks.roleplayinggame.events.SkillPointsSaveEvent
import com.github.jacks.roleplayinggame.events.SkillViewClosedEvent
import com.github.jacks.roleplayinggame.events.UseConsumableEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.ui.Fonts
import com.github.quillraven.fleks.IntervalSystem
import ktx.preferences.flush
import ktx.preferences.set

class StatSystem(
    private val gameStage: Stage,
) : IntervalSystem(), EventListener {

    private val playerFamily      by lazy { world.family(allOf = arrayOf(PlayerComponent::class)) }
    private val statMapper        by lazy { world.mapper<StatComponent>() }
    private val inventoryMapper   by lazy { world.mapper<InventoryComponent>() }
    private val physicsComponents by lazy { world.mapper<PhysicsComponent>() }
    private val preferences: Preferences by lazy { Gdx.app.getPreferences("rolePlayingGamePrefs") }

    override fun onTick() = Unit

    /** Returns true if the character's stat targeted by [statType] is already at its maximum. */
    fun isStatFull(characterIndex: Int, statType: ConsumableStatType): Boolean {
        val entity = playerFamily.firstOrNull() ?: return true
        val stat = statMapper.getOrNull(entity) ?: return true
        return when (statType) {
            ConsumableStatType.HEALTH -> stat.currentHealth >= stat.maxHealth
            ConsumableStatType.MANA   -> stat.currentMana   >= stat.maxMana
        }
    }

    /**
     * Returns the actual amount that would be restored for [item], clamped to the remaining
     * capacity of the stat. Does NOT apply the change.
     */
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

    override fun handle(event: Event): Boolean {
        when (event) {
            is EquipItemEvent -> {
                val entity  = playerFamily.firstOrNull() ?: return false
                val stat    = statMapper.getOrNull(entity) ?: return false
                val inv     = inventoryMapper.getOrNull(entity) ?: return false
                val newItem = equipmentItemById(event.itemId) ?: return false

                // Remove old equipped item's stat bonuses
                val oldId = inv.equippedItems[newItem.category]
                if (oldId != null) {
                    equipmentItemById(oldId)?.stats?.forEach { (statType, value) ->
                        stat.decreaseStat(statType.toStatType(), value.toFloat())
                    }
                }

                // Update slot and apply new bonuses
                inv.equippedItems[newItem.category] = newItem.id
                newItem.stats.forEach { (statType, value) ->
                    stat.increaseStat(statType.toStatType(), value.toFloat())
                }

                // Clamp current values to their maximums
                stat.currentHealth = stat.currentHealth.coerceAtMost(stat.maxHealth)
                stat.currentMana   = stat.currentMana.coerceAtMost(stat.maxMana)
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
                        // TODO: Replace with Fonts.HEAL (green) once "heal.fnt" asset is created
                        if (pos != null) gameStage.fire(FloatingTextEvent(pos, "+$actual HP", Fonts.DAMAGE))
                    }
                    ConsumableStatType.MANA -> {
                        val actual = item.statValue.toFloat().coerceAtMost(stat.maxMana - stat.currentMana).toInt()
                        stat.currentMana = (stat.currentMana + item.statValue).coerceAtMost(stat.maxMana)
                        // TODO: Replace with Fonts.MANA (blue) once "mana.fnt" asset is created
                        if (pos != null) gameStage.fire(FloatingTextEvent(pos, "+$actual MP", Fonts.DAMAGE))
                    }
                }

                world.system<InventorySystem>().removeItem(event.itemId)
                return true
            }

            is LevelUpEvent -> {
                // Fire "LEVEL UP!" floating text over the levelled entity
                // TODO: Replace with Fonts.LEVEL_UP (gold) once "levelup.fnt" asset is created
                val pos: Vector2? = physicsComponents.getOrNull(event.entity)?.body?.position
                if (pos != null) {
                    gameStage.fire(FloatingTextEvent(Vector2(pos), "LEVEL UP!", Fonts.DAMAGE))
                }
                return true
            }

            is GainSkillPointEvent -> {
                val stat = statMapper.getOrNull(event.entity) ?: return false
                stat.skillPoints++
                preferences.flush {
                    this["player_skill_points"] = stat.skillPoints
                }
                return true
            }

            is GainAbilityPointEvent -> {
                val stat = statMapper.getOrNull(event.entity) ?: return false
                stat.abilityPoints++
                preferences.flush {
                    this["player_ability_points"] = stat.abilityPoints
                }
                return true
            }

            is AbilityPointsSaveEvent -> {
                val stat = statMapper.getOrNull(event.entity) ?: return false
                stat.abilityPoints -= event.pendingIds.size
                preferences.flush {
                    this["player_ability_points"] = stat.abilityPoints
                }
                return true
            }

            is SkillPointsSaveEvent -> {
                val stat = statMapper.getOrNull(event.entity) ?: return false
                stat.skillPointsInvestedAttack  += event.pendingAttackPoints
                stat.skillPointsInvestedDefense += event.pendingDefensePoints
                stat.skillPoints -= (event.pendingAttackPoints + event.pendingDefensePoints)
                // Apply effective stat bonuses for newly invested points only
                stat.attackDamage += event.pendingAttackPoints * 2f
                stat.defense      += event.pendingDefensePoints * 1f
                preferences.flush {
                    this["player_skill_points"]     = stat.skillPoints
                    this["player_invested_attack"]  = stat.skillPointsInvestedAttack
                    this["player_invested_defense"] = stat.skillPointsInvestedDefense
                }
                gameStage.fire(SkillPointsChangedEvent(event.entity))
                gameStage.fire(SkillViewClosedEvent())
                return true
            }

            else -> return false
        }
    }
}
