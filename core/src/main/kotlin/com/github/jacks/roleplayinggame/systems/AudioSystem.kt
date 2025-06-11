package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.jacks.roleplayinggame.events.EntityAttackEvent
import com.github.jacks.roleplayinggame.events.EntityDeathEvent
import com.github.jacks.roleplayinggame.events.EntityLootEvent
import com.github.jacks.roleplayinggame.events.GamePauseEvent
import com.github.jacks.roleplayinggame.events.GameResumeEvent
import com.github.jacks.roleplayinggame.events.MapChangeEvent
import com.github.quillraven.fleks.IntervalSystem
import ktx.assets.disposeSafely
import ktx.log.logger
import ktx.tiled.propertyOrNull

class AudioSystem : EventListener, IntervalSystem() {

    private val musicCache = mutableMapOf<String, Music>()
    private val soundCache = mutableMapOf<String, Sound>()
    private val soundRequests = mutableMapOf<String, Sound>()
    private var music : Music? = null

    override fun onTick() {
        if (soundRequests.isEmpty()) {
            return
        }

        soundRequests.values.forEach { it.play(1f) }
        soundRequests.clear()
    }

    override fun handle(event: Event?): Boolean {
        when (event) {
            /*
            is MapChangeEvent -> {
                event.map.propertyOrNull<String>("background_music")?.let { path ->
                    log.debug { "Music changed to $path" }
                    val newMusic = musicCache.getOrPut(path) {
                        Gdx.audio.newMusic(Gdx.files.internal("assets/$path")).apply {
                            isLooping = true
                        }
                    }
                    if (music != null && newMusic != music) {
                        music?.stop()
                    }
                    music = newMusic
                    music?.play()
                }
                return true
            }
             */
            is EntityAttackEvent -> queueSound("assets/audio/${event.model.atlasKey}_attack.wav")
            is EntityDeathEvent -> queueSound("assets/audio/${event.model.atlasKey}_death.wav")
            is EntityLootEvent -> queueSound("assets/audio/${event.model.atlasKey}_open.wav")
            is GamePauseEvent -> {
                music?.pause()
                soundCache.values.forEach { it.pause() }
            }
            is GameResumeEvent -> {
                music?.play()
                soundCache.values.forEach { it.resume() }
            }
        }

        return false
    }

    private fun queueSound(soundPath : String) {
        log.debug { "$soundPath was added to the soundQueue" }
        if (soundPath in soundRequests) {
            // sound is already queued -> do nothing
            return
        }

        val sound = soundCache.getOrPut(soundPath) {
            Gdx.audio.newSound(Gdx.files.internal(soundPath))
        }
        soundRequests[soundPath] = sound
    }

    override fun onDispose() {
        musicCache.values.forEach { it.disposeSafely() }
    }

    companion object {
        private val log = logger<AudioSystem>()
    }
}
