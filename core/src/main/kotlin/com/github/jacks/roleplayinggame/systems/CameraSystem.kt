package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.ImageComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.events.MapChangeEvent
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import ktx.tiled.height
import ktx.tiled.width

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

            camera.position.set(
                (image.x + (image.width * 0.5f)),
                (image.y + (image.height * 0.5f)),
                camera.position.z
            )

//            camera.position.set(
//                (image.x + (image.width * 0.5f)).coerceIn(viewWidth, maxMapWidth - viewWidth),
//                (image.y + (image.height * 0.5f)).coerceIn(viewHeight, maxMapHeight - viewHeight),
//                camera.position.z
//            )
        }
    }

    override fun handle(event: Event): Boolean {
        if (event is MapChangeEvent) {
            maxMapWidth = event.map.width.toFloat()
            maxMapHeight = event.map.height.toFloat()
            return true
        }
        return false
    }
}
