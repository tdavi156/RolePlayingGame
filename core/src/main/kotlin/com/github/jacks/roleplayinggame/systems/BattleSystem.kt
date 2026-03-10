package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.BattleComponent
import com.github.jacks.roleplayinggame.components.PhysicsComponent.Companion.physicsComponentFromShape2D
import com.github.jacks.roleplayinggame.components.PortalComponent
import com.github.jacks.roleplayinggame.events.BattleEndEvent
import com.github.jacks.roleplayinggame.events.BattleEvent
import com.github.jacks.roleplayinggame.events.MapChangeEvent
import com.github.jacks.roleplayinggame.events.PortalEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import ktx.app.gdxError
import ktx.tiled.id
import ktx.tiled.layer
import ktx.tiled.property
import ktx.tiled.shape

@AllOf([BattleComponent::class])
class BattleSystem(
    private val physicsWorld : World,
    private val gameStage : Stage,
    private val battleComponents : ComponentMapper<BattleComponent>,
) : IteratingSystem(), EventListener {

    private var currentBattleEntity: Entity? = null

    override fun onTickEntity(entity: Entity) {
        val battleComponent = battleComponents[entity]
        if (battleComponent.triggerEntities.isNotEmpty() && !battleComponent.battleInProgress) {
            battleComponent.battleInProgress = true
            battleComponent.triggerEntities.clear()
            currentBattleEntity = entity
            gameStage.fire(BattleEvent(enemy = entity))
        }
    }

    override fun handle(event: Event): Boolean {
        when(event) {
            is BattleEndEvent -> {
                currentBattleEntity?.let { entity ->
                    battleComponents.getOrNull(entity)?.battleInProgress = false
                }
                currentBattleEntity = null
                return true
            }
            is MapChangeEvent -> {
                val portalLayer = event.map.layer("portals")
                portalLayer.objects.forEach { mapObject ->
                    val toMap = mapObject.property("toMap","")
                    val toPortal = mapObject.property("toPortal", -1)

                    if (toMap.isBlank()) { return@forEach }
                    if (toPortal == -1) { gdxError("Portal ${mapObject.id} does not have toPortal property.") }

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
            else -> return false
        }
    }
}
