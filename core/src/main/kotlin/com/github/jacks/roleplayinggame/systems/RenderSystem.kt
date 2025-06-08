package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.RolePlayingGame.Companion.UNIT_SCALE
import com.github.jacks.roleplayinggame.components.ImageComponent
import com.github.jacks.roleplayinggame.events.MapChangeEvent
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.Qualifier
import com.github.quillraven.fleks.collection.compareEntity
import ktx.assets.disposeSafely
import ktx.graphics.use
import ktx.log.logger
import ktx.tiled.forEachLayer

@AllOf([ImageComponent::class])
class RenderSystem(
    private val gameStage : Stage,
    @Qualifier("uiStage") private val uiStage : Stage,
    private val imageComponents : ComponentMapper<ImageComponent>
) : EventListener, IteratingSystem(
    comparator = compareEntity{ e1, e2 -> imageComponents[e1].compareTo(imageComponents[e2]) }
) {

    private val backgroundLayers = mutableListOf<TiledMapTileLayer>()
    private val foregroundLayers = mutableListOf<TiledMapTileLayer>()
    private val mapRenderer = OrthogonalTiledMapRenderer(null, UNIT_SCALE, gameStage.batch)
    private val orthographicCamera = gameStage.camera as OrthographicCamera

    override fun onTick() {
        super.onTick()

        with(gameStage) {
            viewport.apply()

            AnimatedTiledMapTile.updateAnimationBaseTime()
            mapRenderer.setView(orthographicCamera)
            if (backgroundLayers.isNotEmpty()) {
                gameStage.batch.color = Color.WHITE
                gameStage.batch.use(orthographicCamera.combined) {
                    backgroundLayers.forEach { mapRenderer.renderTileLayer(it) }
                }
            }

            act(deltaTime)
            draw()

            if (foregroundLayers.isNotEmpty()) {
                gameStage.batch.color = Color.WHITE
                gameStage.batch.use(orthographicCamera.combined) {
                    foregroundLayers.forEach { mapRenderer.renderTileLayer(it) }
                }
            }
        }

        with(uiStage) {
            viewport.apply()
            act(deltaTime)
            draw()

        }
    }

    override fun onTickEntity(entity: Entity) {
        imageComponents[entity].image.toFront()
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is MapChangeEvent -> {
                backgroundLayers.clear()
                foregroundLayers.clear()

                event.map.forEachLayer<TiledMapTileLayer> { layer ->
                    if (layer.name.startsWith("foreground_")) {
                        foregroundLayers.add(layer)
                    } else if (layer.name.startsWith("background_")) {
                        backgroundLayers.add(layer)
                    } else {
                        log.debug { "${layer.name} is not a foreground or background" }
                    }
                }

                return true
            }
        }

        return false
    }

    companion object {
        private val log = logger<AnimationSystem>()
    }

    override fun onDispose() {
        mapRenderer.disposeSafely()
    }
}
