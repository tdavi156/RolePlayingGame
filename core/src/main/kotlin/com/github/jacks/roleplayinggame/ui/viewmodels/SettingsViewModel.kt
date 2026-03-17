package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.configurations.CombatAnimationSpeed
import com.github.jacks.roleplayinggame.configurations.Settings
import com.github.jacks.roleplayinggame.events.SettingsClosedEvent
import com.github.jacks.roleplayinggame.events.SettingsOpenEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.systems.SettingsSystem
import ktx.preferences.flush
import ktx.preferences.set

class SettingsViewModel(
    private val uiStage: Stage,
    private val settingsSystem: SettingsSystem
) : PropertyChangeSource(), EventListener {

    var masterVolume by propertyNotify(100)
    var musicVolume by propertyNotify(100)
    var effectsVolume by propertyNotify(100)
    var combatAnimationSpeed by propertyNotify(CombatAnimationSpeed.NORMAL)
    var autoClearText by propertyNotify(false)
    var focusedRow by propertyNotify(0)
    var row5FocusedButton by propertyNotify(0) // 0 = Save, 1 = Cancel

    init {
        uiStage.addListener(this)
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is SettingsOpenEvent -> {
                masterVolume = settingsSystem.settings.masterVolume
                musicVolume = settingsSystem.settings.musicVolume
                effectsVolume = settingsSystem.settings.effectsVolume
                combatAnimationSpeed = settingsSystem.settings.combatAnimationSpeed
                autoClearText = settingsSystem.settings.autoClearText
                focusedRow = 0
                row5FocusedButton = 0
                return true
            }
        }
        return false
    }

    fun moveFocusedRow(delta: Int) {
        focusedRow = (focusedRow + delta).coerceIn(0, 5)
    }

    /** Adjusts the value of the currently focused row by `delta` (positive = right/increase). */
    fun adjustCurrentValue(delta: Int) {
        when (focusedRow) {
            0 -> masterVolume = (masterVolume + delta).coerceIn(0, 100)
            1 -> musicVolume = (musicVolume + delta).coerceIn(0, 100)
            2 -> effectsVolume = (effectsVolume + delta).coerceIn(0, 100)
            3 -> cycleSpeed(if (delta > 0) 1 else -1)
            4 -> autoClearText = delta > 0
            5 -> row5FocusedButton = if (delta > 0) 1 else 0
        }
    }

    fun cycleSpeed(delta: Int) {
        val values = CombatAnimationSpeed.entries
        val currentIdx = values.indexOf(combatAnimationSpeed)
        combatAnimationSpeed = values[(currentIdx + delta + values.size) % values.size]
    }

    fun confirmCurrentRow() {
        if (focusedRow == 5) {
            if (row5FocusedButton == 0) save() else cancel()
        }
    }

    fun save() {
        settingsSystem.settings.masterVolume = masterVolume
        settingsSystem.settings.musicVolume = musicVolume
        settingsSystem.settings.effectsVolume = effectsVolume
        settingsSystem.settings.combatAnimationSpeed = combatAnimationSpeed
        settingsSystem.settings.autoClearText = autoClearText
        val prefs = Gdx.app.getPreferences("rolePlayingGamePrefs")
        prefs.flush {
            this[Settings.KEY_MASTER_VOLUME] = masterVolume
            this[Settings.KEY_MUSIC_VOLUME] = musicVolume
            this[Settings.KEY_EFFECTS_VOLUME] = effectsVolume
            this[Settings.KEY_COMBAT_ANIMATION_SPEED] = combatAnimationSpeed.ordinal
            this[Settings.KEY_AUTO_CLEAR_TEXT] = autoClearText
        }
        uiStage.fire(SettingsClosedEvent())
    }

    fun cancel() {
        uiStage.fire(SettingsClosedEvent())
    }
}
