package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.Contact
import com.badlogic.gdx.physics.box2d.ContactImpulse
import com.badlogic.gdx.physics.box2d.ContactListener
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.Manifold
import com.badlogic.gdx.physics.box2d.World
import com.github.jacks.roleplayinggame.components.AiComponent
import com.github.jacks.roleplayinggame.components.CollisionComponent
import com.github.jacks.roleplayinggame.components.ImageComponent
import com.github.jacks.roleplayinggame.components.PhysicsComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.PortalComponent
import com.github.jacks.roleplayinggame.components.TiledComponent
import com.github.jacks.roleplayinggame.systems.EntitySpawnSystem.Companion.AI_SENSOR
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Fixed
import com.github.quillraven.fleks.IteratingSystem
import ktx.log.logger
import ktx.math.component1
import ktx.math.component2

val Fixture.entity : Entity
    get() = this.body.userData as Entity

@AllOf([PhysicsComponent::class, ImageComponent::class])
class PhysicsSystem (
    private val physicsWorld : World,
    private val imageComponents : ComponentMapper<ImageComponent>,
    private val physicsComponents : ComponentMapper<PhysicsComponent>,
    private val tiledComponents : ComponentMapper<TiledComponent>,
    private val collisionComponents : ComponentMapper<CollisionComponent>,
    private val aiComponents : ComponentMapper<AiComponent>,
    private val portalComponents : ComponentMapper<PortalComponent>,
    private val playerComponents : ComponentMapper<PlayerComponent>
) : ContactListener, IteratingSystem(interval = Fixed(1 / 60f)) {

    init {
        physicsWorld.setContactListener(this)
    }

    override fun onUpdate() {
        if (physicsWorld.autoClearForces) {
            log.error { "autoClearForces must be set to false." }
            physicsWorld.autoClearForces = false
        }
        super.onUpdate()
        physicsWorld.clearForces()
    }

    override fun onTick() {
        super.onTick()
        physicsWorld.step(deltaTime, 6, 2)
    }

    override fun onTickEntity(entity: Entity) {
        val physicsComponent = physicsComponents[entity]

        physicsComponent.previousPosition.set(physicsComponent.body.position)

        if (!physicsComponent.impulse.isZero) {
            physicsComponent.body.applyLinearImpulse(physicsComponent.impulse, physicsComponent.body.worldCenter, true)
            physicsComponent.impulse.setZero()
        }
    }

    override fun onAlphaEntity(entity: Entity, alpha: Float) {
        val physicsComponent = physicsComponents[entity]
        val imageComponent = imageComponents[entity]

        val (previousX, previousY) = physicsComponent.previousPosition
        val (bodyX, bodyY) = physicsComponent.body.position

        imageComponent.image.run {
            setPosition(
                MathUtils.lerp(previousX, bodyX, alpha) - width * 0.5f,
                MathUtils.lerp(previousY, bodyY, alpha) - height * 0.5f
            )
        }
    }

    override fun beginContact(contact: Contact) {
        val entityA : Entity = contact.fixtureA.entity
        val entityB : Entity = contact.fixtureB.entity
        val isEntityATiledCollisionSensor = entityA in tiledComponents && contact.fixtureA.isSensor
        val isEntityBCollisionFixture = entityB in collisionComponents && !contact.fixtureB.isSensor
        val isEntityBTiledCollisionSensor = entityB in tiledComponents && contact.fixtureB.isSensor
        val isEntityACollisionFixture = entityA in collisionComponents && !contact.fixtureA.isSensor
        val isEntityAAiSensor = entityA in aiComponents && contact.fixtureA.isSensor && contact.fixtureA.userData == AI_SENSOR
        val isEntityBAiSensor = entityB in aiComponents && contact.fixtureB.isSensor && contact.fixtureB.userData == AI_SENSOR

        when {
            isEntityATiledCollisionSensor && isEntityBCollisionFixture -> {
                tiledComponents[entityA].nearbyEntities += entityB
            }
            isEntityBTiledCollisionSensor && isEntityACollisionFixture  -> {
                tiledComponents[entityB].nearbyEntities += entityA
            }
            isEntityAAiSensor && isEntityBCollisionFixture -> {
                aiComponents[entityA].nearbyEntities += entityB
            }
            isEntityBAiSensor && isEntityACollisionFixture -> {
                aiComponents[entityB].nearbyEntities += entityA
            }
            entityA in portalComponents && entityB in playerComponents && !contact.fixtureB.isSensor -> {
                portalComponents[entityA].triggerEntities += entityB
            }
            entityB in portalComponents && entityA in playerComponents && !contact.fixtureA.isSensor -> {
                portalComponents[entityB].triggerEntities += entityA
            }
        }
    }

    override fun endContact(contact: Contact) {
        val entityA : Entity = contact.fixtureA.entity
        val entityB : Entity = contact.fixtureB.entity
        val isEntityATiledCollisionSensor = entityA in tiledComponents && contact.fixtureA.isSensor
        val isEntityBTiledCollisionSensor = entityB in tiledComponents && contact.fixtureB.isSensor
        val isEntityAAiSensor = entityA in aiComponents && contact.fixtureA.isSensor && contact.fixtureA.userData == AI_SENSOR
        val isEntityBAiSensor = entityB in aiComponents && contact.fixtureB.isSensor && contact.fixtureB.userData == AI_SENSOR

        when {
            isEntityATiledCollisionSensor && !contact.fixtureB.isSensor -> {
                tiledComponents[entityA].nearbyEntities -= entityB
            }
            isEntityBTiledCollisionSensor && !contact.fixtureA.isSensor  -> {
                tiledComponents[entityB].nearbyEntities -= entityA
            }
            isEntityAAiSensor && !contact.fixtureB.isSensor -> {
                aiComponents[entityA].nearbyEntities -= entityB
            }
            isEntityBAiSensor && !contact.fixtureA.isSensor -> {
                aiComponents[entityB].nearbyEntities -= entityA
            }

        }
    }

    override fun preSolve(contact: Contact, oldManifold: Manifold) {
        contact.isEnabled = ((contact.fixtureA.body.type == BodyDef.BodyType.StaticBody &&
                              contact.fixtureB.body.type == BodyDef.BodyType.DynamicBody) ||
                             (contact.fixtureB.body.type == BodyDef.BodyType.StaticBody &&
                              contact.fixtureA.body.type == BodyDef.BodyType.DynamicBody))
    }

    override fun postSolve(contact: Contact?, impulse: ContactImpulse?) = Unit

    companion object {
        private val log = logger<PhysicsSystem>()
    }
}
