package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.ImageComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.events.BattleMapChangeEvent
import com.github.jacks.roleplayinggame.events.MapChangeEvent
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import ktx.tiled.height
import ktx.tiled.width
import kotlin.math.max
import kotlin.math.min

@AllOf([PlayerComponent::class, ImageComponent::class])
class CameraSystem (
    private val imageComponents : ComponentMapper<ImageComponent>,
    stage : Stage
) : EventListener, IteratingSystem() {

    private var maxMapWidth = 0f
    private var maxMapHeight = 0f
    private val camera = stage.camera

    override fun onTickEntity(entity: Entity) {
        with(imageComponents[entity]) {
            val viewWidth = camera.viewportWidth * 0.5f
            val viewHeight = camera.viewportHeight * 0.5f
            val minWidth = min(viewWidth, maxMapWidth - viewWidth)
            val maxWidth = max(viewWidth, maxMapWidth - viewWidth)
            val minHeight = min(viewHeight, maxMapHeight - viewHeight)
            val maxHeight = max(viewHeight, maxMapHeight - viewHeight)

            camera.position.set(
                (image.x + (image.width * 0.5f)).coerceIn(minWidth, maxWidth),
                (image.y + (image.height * 0.5f)).coerceIn(minHeight, maxHeight),
                camera.position.z
            )
        }
    }

    override fun handle(event: Event): Boolean {
        when(event) {
            is MapChangeEvent -> {
                maxMapWidth = event.map.width.toFloat()
                maxMapHeight = event.map.height.toFloat()
                return true
            }
            is BattleMapChangeEvent -> {
                // set the camera to a fixed position on the map, and possibly zoom out as well
                // or consider making the map smaller to fit everything on screen
                // there will be quite a bit of UI for the battle map so there needs to be enough space for it
                return true
            }
            else -> return false
        }
    }
}
