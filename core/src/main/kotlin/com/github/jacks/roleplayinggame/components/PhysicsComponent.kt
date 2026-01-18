package com.github.jacks.roleplayinggame.components

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Shape2D
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.jacks.roleplayinggame.RolePlayingGame.Companion.UNIT_SCALE
import com.github.jacks.roleplayinggame.systems.CollisionSpawnSystem.Companion.SPAWN_AREA_SIZE
import com.github.jacks.roleplayinggame.systems.EntityCreationSystem.Companion.HIT_BOX_SENSOR
import com.github.quillraven.fleks.ComponentListener
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.EntityCreateCfg
import ktx.app.gdxError
import ktx.box2d.body
import ktx.box2d.box
import ktx.box2d.loop
import ktx.math.vec2

class PhysicsComponent {
    val previousPosition = vec2()
    val impulse = vec2()
    val offset = vec2()
    val size = vec2()
    lateinit var body : Body

    companion object {
        private val TEMP_VEC = vec2()
        fun EntityCreateCfg.physicsComponentFromShape2D(
            world : World,
            x : Int,
            y : Int,
            shape : Shape2D,
            isPortal : Boolean = false
        ) : PhysicsComponent {
            when (shape) {
                is Rectangle -> {
                    val bodyX = x + shape.x * UNIT_SCALE
                    val bodyY = y + shape.y * UNIT_SCALE
                    val bodyWidth = shape.width * UNIT_SCALE
                    val bodyHeight = shape.height * UNIT_SCALE

                    return add {
                        body = world.body(StaticBody) {
                            position.set(bodyX, bodyY)
                            fixedRotation = false
                            allowSleep = false
                            loop(
                                vec2(0f, 0f),
                                vec2(bodyWidth, 0f),
                                vec2(bodyWidth, bodyHeight),
                                vec2(0f, bodyHeight)
                            ) {
                                this.isSensor = isPortal
                            }

                            if (!isPortal) {
                                TEMP_VEC.set(bodyWidth * 0.5f, bodyHeight * 0.5f)
                                box(SPAWN_AREA_SIZE + 2f, SPAWN_AREA_SIZE + 2f, TEMP_VEC) {
                                    isSensor = true
                                }
                            }
                        }
                    }
                }
                else -> gdxError("Shape $shape is not supported.")
            }
        }

        fun PhysicsComponent.bodyFromImageAndConfiguration(
            world : World,
            image : Image,
            bodyType : BodyType,
            physicsScaling : Vector2,
            physicsOffset : Vector2,
        ) : Body {
            val x = image.x
            val y = image.y
            val width = image.width
            val height = image.height
            val physicsComponent = this

            return world.body(bodyType) {
                position.set(x + width * 0.5f, y + height * 0.5f)
                fixedRotation = true
                allowSleep = false

                val scaledWidth = width * physicsScaling.x
                val scaledHeight = height * physicsScaling.y
                physicsComponent.offset.set(physicsOffset)
                physicsComponent.size.set(scaledWidth, scaledHeight)

                box(scaledWidth, scaledHeight, physicsOffset) {
                    isSensor = bodyType != StaticBody
                    userData = HIT_BOX_SENSOR
                }

                if (bodyType != StaticBody) {
                    val collisionHeight = scaledHeight * 0.4f
                    val collisionOffset = vec2().apply { set(physicsOffset) }
                    collisionOffset.y -= scaledHeight * 0.5f - collisionHeight * 0.5f
                    box(scaledWidth, collisionHeight, collisionOffset)
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
