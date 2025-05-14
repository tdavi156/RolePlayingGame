package com.github.jacks.roleplayinggame.components

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Shape2D
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.jacks.roleplayinggame.RolePlayingGame.Companion.UNIT_SCALE
import com.github.jacks.roleplayinggame.systems.CollisionSpawnSystem.Companion.SPAWN_AREA_SIZE
import com.github.quillraven.fleks.ComponentListener
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.EntityCreateCfg
import ktx.app.gdxError
import ktx.box2d.BodyDefinition
import ktx.box2d.body
import ktx.box2d.circle
import ktx.box2d.loop
import ktx.math.vec2

class PhysicsComponent {
    val previousPosition = vec2()
    val impulse = vec2()
    val offset = vec2()
    val size = vec2()
    lateinit var body : Body

    companion object {
        fun EntityCreateCfg.physicsComponentFromShape2D(
            world : World,
            x : Int,
            y : Int,
            shape : Shape2D
        ) : PhysicsComponent {
            when (shape) {
                is Rectangle -> {
                    val bodyX = x + shape.x * UNIT_SCALE
                    val bodyY = y + shape.y * UNIT_SCALE
                    val bodyWidth = shape.width * UNIT_SCALE
                    val bodyHeight = shape.height * UNIT_SCALE

                    return add {
                        body = world.body(BodyType.StaticBody) {
                            position.set(bodyX, bodyY)
                            fixedRotation = false
                            allowSleep = false
                            loop(
                                vec2(0f, 0f),
                                vec2(bodyWidth, 0f),
                                vec2(bodyWidth, bodyHeight),
                                vec2(0f, bodyHeight)
                            )
                            circle(SPAWN_AREA_SIZE + 2f) { isSensor = true }
                        }
                    }
                }
                else -> gdxError("Shape $shape is not supported.")
            }
        }

        fun EntityCreateCfg.physicsComponentFromImage(
            world : World,
            image : Image,
            bodyType : BodyType,
            fixtureAction : BodyDefinition.(PhysicsComponent, Float, Float) -> Unit
        ) : PhysicsComponent {
            val x = image.x
            val y = image.y
            val width = image.width
            val height = image.height

            return add {
                body = world.body(bodyType) {
                    position.set(x + width * 0.5f, y + height * 0.5f)
                    fixedRotation = true
                    allowSleep = false
                    this.fixtureAction(this@add, width, height)
                }
            }
        }

        class PhysicsComponentListener : ComponentListener<PhysicsComponent> {
            override fun onComponentAdded(entity: Entity, component: PhysicsComponent) {
                component.body.userData = entity
            }

            override fun onComponentRemoved(entity: Entity, component: PhysicsComponent) {
                val body = component.body
                body.world.destroyBody(body)
                body.userData = null
            }
        }
    }
}
