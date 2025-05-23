package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.utils.Scaling
import com.github.jacks.roleplayinggame.RolePlayingGame.Companion.UNIT_SCALE
import com.github.jacks.roleplayinggame.components.AnimationComponent
import com.github.jacks.roleplayinggame.components.AnimationModel
import com.github.jacks.roleplayinggame.components.AnimationType
import com.github.jacks.roleplayinggame.components.ImageComponent
import com.github.jacks.roleplayinggame.components.PhysicsComponent.Companion.physicsComponentFromImage
import com.github.jacks.roleplayinggame.components.SpawnComponent
import com.github.jacks.roleplayinggame.components.SpawnConfiguration
import com.github.jacks.roleplayinggame.events.MapChangeEvent
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import ktx.app.gdxError
import ktx.math.vec2
import ktx.tiled.*
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody
import com.github.jacks.roleplayinggame.actors.FlipImage
import com.github.jacks.roleplayinggame.components.AiComponent
import com.github.jacks.roleplayinggame.components.AnimationDirection
import com.github.jacks.roleplayinggame.components.AttackComponent
import com.github.jacks.roleplayinggame.components.CollisionComponent
import com.github.jacks.roleplayinggame.components.DEFAULT_ATTACK_DAMAGE
import com.github.jacks.roleplayinggame.components.DEFAULT_LIFE
import com.github.jacks.roleplayinggame.components.DEFAULT_SPEED
import com.github.jacks.roleplayinggame.components.LifeComponent
import com.github.jacks.roleplayinggame.components.LootComponent
import com.github.jacks.roleplayinggame.components.MoveComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.StateComponent
import ktx.box2d.box
import ktx.box2d.circle
import kotlin.math.roundToInt

@AllOf([SpawnComponent::class])
class EntitySpawnSystem(
    private val physicsWorld : World,
    private val atlas : TextureAtlas,
    private val spawnComponents : ComponentMapper<SpawnComponent>
) : EventListener, IteratingSystem() {

    private val cachedConfigurations = mutableMapOf<String, SpawnConfiguration>()
    private val cachedSizes = mutableMapOf<AnimationModel, Vector2>()

    override fun onTickEntity(entity: Entity) {
        with(spawnComponents[entity]) {
            val configuration = spawnConfiguration(name)
            val relativeSize = size(configuration.model)

            world.entity {
                val imageComponent = add<ImageComponent> {
                    image = FlipImage().apply {
                        setPosition(location.x, location.y)
                        setSize(relativeSize.x, relativeSize.y)
                        setScaling(Scaling.fill)
                    }
                }

                add<AnimationComponent> {
                    nextAnimation(configuration.model, AnimationType.IDLE)
                }

                // Creates the physics box around the entity. Scaled and offset from the configuration
                val physicsComponent = physicsComponentFromImage(
                    physicsWorld,
                    imageComponent.image,
                    configuration.bodyType
                ) { physicsComponent, width, height ->
                    val scaledWidth = width * configuration.physicsScaling.x
                    val scaledHeight = height * configuration.physicsScaling.y
                    physicsComponent.offset.set(configuration.physicsOffset)
                    physicsComponent.size.set(scaledWidth, scaledHeight)

                    box(scaledWidth, scaledHeight, configuration.physicsOffset) {
                        isSensor = configuration.bodyType != StaticBody
                        userData = HIT_BOX_SENSOR
                    }

                    if (configuration.bodyType != StaticBody) {
                        val collisionHeight = scaledHeight * 0.4f
                        val collisionOffset = vec2().apply { set(configuration.physicsOffset) }
                        collisionOffset.y -= scaledHeight * 0.5f - collisionHeight * 0.5f
                        box(scaledWidth, collisionHeight, collisionOffset)
                    }
                }

                if (configuration.speedScaling > 0f) {
                    add<MoveComponent>() {
                        speed = DEFAULT_SPEED * configuration.speedScaling
                    }
                }

                if (configuration.canAttack) {
                    add<AttackComponent> {
                        maxDelay = configuration.attackDelay
                        damage = (DEFAULT_ATTACK_DAMAGE * configuration.attackScaling).roundToInt()
                        extraRange = configuration.attackRange
                    }
                }

                if (configuration.lifeScaling > 0) {
                    add<LifeComponent> {
                        maxHealth = DEFAULT_LIFE * configuration.lifeScaling
                        health = maxHealth
                    }
                }

                if (name == AnimationModel.PLAYER.atlasKey) {
                    add<PlayerComponent>()
                    add<StateComponent>()
                }

                if (configuration.bodyType != StaticBody) {
                    add<CollisionComponent>()
                }

                if (configuration.lootable) {
                    add<LootComponent>()
                }

                if (configuration.aiTreePath.isNotBlank()) {
                    add<AiComponent>() {
                        treePath = configuration.aiTreePath
                    }
                    physicsComponent.body.circle(4f) {
                        isSensor = true
                        userData = AI_SENSOR
                    }
                }
            }
        }
        world.remove(entity)
    }

    private fun spawnConfiguration(type : String) : SpawnConfiguration = cachedConfigurations.getOrPut(type) {
        when (type) {
            AnimationModel.PLAYER.atlasKey -> SpawnConfiguration(
                AnimationModel.PLAYER,
                speedScaling = 1.5f,
                lifeScaling = 1f,
                attackRange = 0.75f,
                attackScaling = 5f,
                physicsScaling = vec2(0.3f, 0.3f,),
                physicsOffset = vec2(0f, -10f * UNIT_SCALE)
            )
            AnimationModel.SLIME.atlasKey -> SpawnConfiguration(
                AnimationModel.SLIME,
                lifeScaling = 2f,
                attackRange = 1f,
                physicsScaling = vec2(0.3f, 0.3f),
                physicsOffset = vec2(0f, -2f * UNIT_SCALE),
                aiTreePath = "slimeBehavior.tree"
            )
            AnimationModel.CHEST.atlasKey -> SpawnConfiguration(
                AnimationModel.CHEST,
                speedScaling = 0f,
                bodyType = StaticBody,
                canAttack = false,
                lifeScaling = 0f,
                lootable = true
            )
            else -> gdxError("Type $type has no spawn configuration")
        }
    }

    private fun size(model : AnimationModel) = cachedSizes.getOrPut(model) {
        val regions = atlas.findRegions("${model.atlasKey}/${AnimationType.IDLE.atlasKey}${AnimationDirection.TO.atlasKey}")
        if (regions.isEmpty) {
            gdxError("There are no regions for the idle animation for the $model model")
        }
        val firstFrame = regions.first()
        vec2(firstFrame.originalWidth * UNIT_SCALE, firstFrame.originalHeight * UNIT_SCALE)
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is MapChangeEvent -> {
                val entityLayer = event.map.layer("entities")
                entityLayer.objects.forEach { mapObject ->
                    val name = mapObject.name ?: gdxError("Map Object $mapObject has no name")
                    world.entity {
                        add<SpawnComponent> {
                            this.name = name
                            this.location.set(mapObject.x * UNIT_SCALE, mapObject.y * UNIT_SCALE)
                        }
                    }
                }

                return true
            }
        }
        return false
    }

    companion object {
        const val HIT_BOX_SENSOR = "hitbox"
        const val AI_SENSOR = "aiSensor"
    }
}
