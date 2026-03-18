package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.github.jacks.roleplayinggame.components.FloatingTextComponent
import com.github.jacks.roleplayinggame.events.FloatingTextEvent
import com.github.jacks.roleplayinggame.ui.get
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.Qualifier
import ktx.math.vec2
import ktx.scene2d.Scene2DSkin

@AllOf([FloatingTextComponent::class])
class FloatingTextSystem(
    private val gameStage : Stage,
    @Qualifier("uiStage") private val uiStage : Stage,
    private val textComponents : ComponentMapper<FloatingTextComponent>,
) : IteratingSystem(), EventListener {

    private val uiStartLocation = vec2()
    private val uiTargetLocation = vec2()

    private fun Vector2.toUICoordinates(from : Vector2) {
        this.set(from)
        gameStage.viewport.project(this)
        uiStage.viewport.unproject(this)
    }

    override fun onTickEntity(entity: Entity) {
        with(textComponents[entity]) {
            if (time >= textDuration) {
                world.remove(entity)
                return
            }

            time += deltaTime
            uiStartLocation.toUICoordinates(textStartLocation)
            uiTargetLocation.toUICoordinates(textTargetLocation)
            uiStartLocation.interpolate(uiTargetLocation, (time / textDuration).coerceAtMost(1f), Interpolation.swingOut)
            label.setPosition(uiStartLocation.x, uiStage.viewport.worldHeight - uiStartLocation.y)
        }
    }

    override fun handle(event: Event): Boolean {
        if (event !is FloatingTextEvent) return false
        val style = Label.LabelStyle(Scene2DSkin.defaultSkin[event.fontType], Color.WHITE)
        world.entity {
            add<FloatingTextComponent> {
                textStartLocation.set(event.position.x, event.position.y)
                textDuration = 1.5f
                label = Label(event.text, style)
                label.setFontScale(0.75f)
            }
        }
        return true
    }
}
