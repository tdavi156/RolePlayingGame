package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.RolePlayingGame.Companion.UNIT_SCALE
import com.github.jacks.roleplayinggame.components.ImageComponent
import com.github.jacks.roleplayinggame.components.InventoryComponent
import com.github.jacks.roleplayinggame.components.ItemComponent
import com.github.jacks.roleplayinggame.components.PhysicsComponent
import com.github.jacks.roleplayinggame.components.PhysicsComponent.Companion.bodyFromImageAndConfiguration
import com.github.jacks.roleplayinggame.components.PhysicsComponent.Companion.physicsComponentFromShape2D
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.PortalComponent
import com.github.jacks.roleplayinggame.events.MapChangeEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.systems.EntitySpawnSystem.Companion.PLAYER_CONFIGURATION
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import ktx.app.gdxError
import ktx.assets.disposeSafely
import ktx.tiled.height
import ktx.tiled.id
import ktx.tiled.layer
import ktx.tiled.property
import ktx.tiled.shape
import ktx.tiled.width
import ktx.tiled.x
import ktx.tiled.y

@AllOf([PortalComponent::class])
class PortalSystem(
    private val physicsWorld : World,
    private val gameStage : Stage,
    private val portalComponents : ComponentMapper<PortalComponent>,
    private val physicsComponents : ComponentMapper<PhysicsComponent>,
    private val imageComponents : ComponentMapper<ImageComponent>,
) : IteratingSystem(), EventListener {

    private var currentMap : TiledMap? = null

    fun setMap(mapName : String, targetPortalId : Int = -1) {
        currentMap?.disposeSafely()
        world.family(noneOf = arrayOf(PlayerComponent::class, ItemComponent::class)).forEach { world.remove(it) }
        val newMap = TmxMapLoader().load("maps/$mapName.tmx")
        currentMap = newMap

        if (targetPortalId != -1) {
            world.family(allOf = arrayOf(PlayerComponent::class)).forEach { playerEntity ->
                val targetPortal = targetPortalById(newMap, targetPortalId)
                val playerImage = imageComponents[playerEntity].image
                playerImage.setPosition(
                    targetPortal.x * UNIT_SCALE - playerImage.width * 0.5f + targetPortal.width * 0.5f * UNIT_SCALE,
                    targetPortal.y * UNIT_SCALE - targetPortal.height * 0.5f * UNIT_SCALE
                )

                configureEntity(playerEntity) {
                    physicsComponents.remove(it)
                    physicsComponents.add(it) {
                        body = bodyFromImageAndConfiguration(physicsWorld, playerImage, PLAYER_CONFIGURATION)
                    }
                }
            }
        }

        gameStage.fire(MapChangeEvent(newMap))
    }

    private fun targetPortalById(map : TiledMap, portalId : Int) : MapObject {
        return map.layer("portals").objects.first { it.id == portalId }
            ?: gdxError("There is no portal with id: $portalId")
    }

    override fun onTickEntity(entity: Entity) {
        val (id : Int, toMap : String, toPortal : Int, triggeringEntities : MutableSet<Entity>) = portalComponents[entity]
        if (triggeringEntities.isNotEmpty()) {
            triggeringEntities.clear()
            setMap(toMap, toPortal)
        }
    }

    override fun handle(event: Event?): Boolean {
        if (event is MapChangeEvent) {
            val portalLayer = event.map.layer("portals")
            portalLayer.objects.forEach { mapObject ->
                val toMap = mapObject.property("toMap","")
                val toPortal = mapObject.property("toPortal", -1)

                if (toMap.isBlank()) {
                    // target portal
                    return@forEach
                } else if (toPortal == -1) {
                    gdxError("Portal ${mapObject.id} does not have toPortal property.")
                }

                world.entity {
                    add<PortalComponent> {
                        this.id = mapObject.id
                        this.toMap = toMap
                        this.toPortal = toPortal
                    }
                    physicsComponentFromShape2D(physicsWorld, 0, 0, mapObject.shape, true)
                }

            }
            return true
        }
        return false
    }

    override fun onDispose() {
        currentMap?.disposeSafely()
    }
}
