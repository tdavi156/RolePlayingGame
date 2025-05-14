package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.jacks.roleplayinggame.components.CollisionComponent
import com.github.jacks.roleplayinggame.components.PhysicsComponent
import com.github.jacks.roleplayinggame.components.PhysicsComponent.Companion.physicsComponentFromShape2D
import com.github.jacks.roleplayinggame.components.TiledComponent
import com.github.jacks.roleplayinggame.events.CollisionDespawnEvent
import com.github.jacks.roleplayinggame.events.MapChangeEvent
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import ktx.box2d.body
import ktx.box2d.loop
import ktx.collections.GdxArray
import ktx.math.component1
import ktx.math.component2
import ktx.math.vec2
import ktx.tiled.forEachLayer
import ktx.tiled.height
import ktx.tiled.isEmpty
import ktx.tiled.shape
import ktx.tiled.width
import kotlin.math.max

@AllOf([PhysicsComponent::class, CollisionComponent::class])
class CollisionSpawnSystem(
    private val physicsWorld : World,
    private val physicsComponents : ComponentMapper<PhysicsComponent>
) : EventListener, IteratingSystem() {

    private val tiledLayers = GdxArray<TiledMapTileLayer>()
    private val processedCells = mutableSetOf<Cell>()

    private fun TiledMapTileLayer.forEachCell(
        startX : Int,
        startY : Int,
        size : Int,
        action : (TiledMapTileLayer.Cell, Int, Int) -> Unit
    ) {
        for (x in startX - size .. startX + size) {
            for (y in startY - size .. startY + size) {
                this.getCell(x, y)?.let { action(it, x, y) }
            }
        }
    }

    override fun onTickEntity(entity: Entity) {
        val (entityX, entityY) = physicsComponents[entity].body.position

        tiledLayers.forEach { layer ->
            layer.forEachCell(entityX.toInt(), entityY.toInt(), SPAWN_AREA_SIZE) { cell, x, y ->
                if (cell.tile.objects.isEmpty()) {
                    return@forEachCell
                }

                if (cell in processedCells) {
                    return@forEachCell
                }

                processedCells.add(cell)
                cell.tile.objects.forEach { mapObject ->
                    world.entity {
                        physicsComponentFromShape2D(physicsWorld, x, y, mapObject.shape)
                        add<TiledComponent> {
                            this.cell = cell
                            nearbyEntities.add(entity)
                        }
                    }
                }
            }
        }
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is MapChangeEvent -> {
                event.map.layers.getByType(TiledMapTileLayer::class.java, tiledLayers)

                // world boundary to disallow the player moving off the edge of the map
                world.entity {
                    val width = event.map.width.toFloat()
                    val height = event.map.height.toFloat()

                    add<PhysicsComponent> {
                        body = physicsWorld.body(BodyDef.BodyType.StaticBody) {
                            position.set(0f, 0f)
                            fixedRotation = true
                            allowSleep = false
                            loop(
                                vec2(0f, 0f),
                                vec2(width, 0f),
                                vec2(width, height),
                                vec2(0f, height)
                            )
                        }
                    }
                }
                return true
            }
            is CollisionDespawnEvent -> {
                processedCells.remove(event.cell)
                return true
            }
        }
        return false
    }

    companion object {
        const val SPAWN_AREA_SIZE = 10
    }
}
