package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.configurations.CHARACTER_CONFIGS
import com.github.jacks.roleplayinggame.saveManager.CharacterData
import com.github.jacks.roleplayinggame.configurations.ItemCategory
import com.github.jacks.roleplayinggame.events.AddCharacterToPartyEvent
import com.github.jacks.roleplayinggame.events.PartyUpdatedEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.saveManager.SaveManager
import com.github.quillraven.fleks.IntervalSystem

class PartySystem(
    private val gameStage: Stage,
    private val saveManager: SaveManager,
) : IntervalSystem(), EventListener {

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
        saveManager.gatherAndSave(world)
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
        }
        saveManager.gatherAndSave(world)
    }

    companion object {
        const val MAX_COMBAT_SLOTS = 3
    }
}
