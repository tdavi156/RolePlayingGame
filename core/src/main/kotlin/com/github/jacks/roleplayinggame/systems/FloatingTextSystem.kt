package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.FloatingTextComponent
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.Qualifier
import ktx.math.vec2

@AllOf([FloatingTextComponent::class])
class FloatingTextSystem(
    private val gameStage : Stage,
    @Qualifier("uiStage") private val uiStage : Stage,
    private val textComponents : ComponentMapper<FloatingTextComponent>
) : IteratingSystem() {

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
}
