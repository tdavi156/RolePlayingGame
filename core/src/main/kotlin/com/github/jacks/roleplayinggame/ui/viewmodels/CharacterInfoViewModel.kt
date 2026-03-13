package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.events.EntityRespawnEvent
import com.github.jacks.roleplayinggame.events.EntityTakeDamageEvent
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.World

class CharacterInfoViewModel(
    world: World,
    stage: Stage,
) : PropertyChangeSource(), EventListener {

    private val playerFamily = world.family(allOf = arrayOf(PlayerComponent::class))
    private val statMapper: ComponentMapper<StatComponent> = world.mapper()

    var playerLevel by propertyNotify(1)
    var playerExperience by propertyNotify(0)
    var playerExperienceToNext by propertyNotify(50)
    var playerCurrentHealth by propertyNotify(0f)
    var playerMaxHealth by propertyNotify(0f)
    var playerCurrentMana by propertyNotify(0f)
    var playerMaxMana by propertyNotify(0f)
    var playerAttack by propertyNotify(0f)
    var playerDefense by propertyNotify(0f)
    var playerSpeed by propertyNotify(0f)

    init {
        stage.addListener(this)
    }

    fun refreshStats() {
        playerFamily.forEach { player ->
            val stat = statMapper[player]
            playerLevel = stat.level
            playerExperience = stat.experience
            playerExperienceToNext = stat.experienceToNextLevel
            playerCurrentHealth = stat.currentHealth
            playerMaxHealth = stat.maxHealth
            playerCurrentMana = stat.currentMana
            playerMaxMana = stat.maxMana
            playerAttack = stat.attackDamage
            playerDefense = stat.defense
            playerSpeed = stat.moveSpeed
        }
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is EntityTakeDamageEvent -> {
                playerFamily.forEach { player ->
                    if (player == event.entity) {
                        val stat = statMapper[player]
                        playerCurrentHealth = stat.currentHealth
                    }
                }
            }
            is EntityRespawnEvent -> refreshStats()
            else -> return false
        }
        return true
    }
}
