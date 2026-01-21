package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
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
import com.github.jacks.roleplayinggame.components.PhysicsComponent.Companion.bodyFromImageAndConfiguration
import com.github.jacks.roleplayinggame.components.EntityCreationComponent
import com.github.jacks.roleplayinggame.components.SpawnConfiguration
import com.github.jacks.roleplayinggame.configurations.Configurations.Companion.getConfiguration
import com.github.jacks.roleplayinggame.configurations.Configurations.Companion.getConfigurationType
import com.github.jacks.roleplayinggame.events.MapChangeEvent
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import ktx.app.gdxError
import ktx.math.vec2
import ktx.tiled.*
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody
import com.badlogic.gdx.utils.Array
import com.github.jacks.roleplayinggame.actors.FlipImage
import com.github.jacks.roleplayinggame.components.AiComponent
import com.github.jacks.roleplayinggame.components.AnimationDirection
import com.github.jacks.roleplayinggame.components.AttackComponent
import com.github.jacks.roleplayinggame.components.BattleComponent
import com.github.jacks.roleplayinggame.components.CollisionComponent
import com.github.jacks.roleplayinggame.components.DEFAULT_ATTACK_DAMAGE
import com.github.jacks.roleplayinggame.components.DEFAULT_LIFE
import com.github.jacks.roleplayinggame.components.DEFAULT_SPEED
import com.github.jacks.roleplayinggame.components.DialogId
import com.github.jacks.roleplayinggame.components.InventoryComponent
import com.github.jacks.roleplayinggame.components.LifeComponent
import com.github.jacks.roleplayinggame.components.MoveComponent
import com.github.jacks.roleplayinggame.components.NonPlayerComponent
import com.github.jacks.roleplayinggame.components.NonPlayerConfiguration
import com.github.jacks.roleplayinggame.components.PhysicsComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.PlayerConfiguration
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.components.StateComponent
import com.github.jacks.roleplayinggame.configurations.ConfigurationType
import ktx.box2d.circle
import ktx.log.logger
import kotlin.math.roundToInt

@AllOf([EntityCreationComponent::class])
class EntityCreationSystem(
    private val physicsWorld : World,
    private val atlas : TextureAtlas,
    private val entityCreationComponents : ComponentMapper<EntityCreationComponent>,
) : EventListener, IteratingSystem() {

    private val preferences : Preferences by lazy { Gdx.app.getPreferences("rolePlayingGamePrefs") }
    private val cachedConfigurations = mutableMapOf<String, SpawnConfiguration>()
    private val cachedSizes = mutableMapOf<AnimationModel, Vector2>()
    private val playerEntities = world.family(allOf = arrayOf(PlayerComponent::class))

    override fun onTickEntity(entity: Entity) {
        with(entityCreationComponents[entity]) {
            when(configurationType) {
                ConfigurationType.PLAYER -> {
                    val config = configuration as PlayerConfiguration
                    world.entity {
                        val imageComponent = add<ImageComponent> {
                            image = FlipImage().apply {
                                setPosition(location.x, location.y)
                                setSize(size(config.model).x, size(config.model).y)
                                setScaling(Scaling.fill)
                                color = config.color
                            }
                        }
                        val animationComponent = add<AnimationComponent> {
                            nextAnimation(config.model, AnimationType.IDLE)
                        }
                        val physicsComponent = add<PhysicsComponent> {
                            body = bodyFromImageAndConfiguration(physicsWorld, imageComponent.image, config.bodyType, config.physicsScaling, config.physicsOffset)
                        }
                        val moveComponent = add<MoveComponent> {
                            speed = DEFAULT_SPEED * config.speedScaling
                        }
                        // remove when the battle system is redone
                        val attackComponent = add<AttackComponent> {
                            maxDelay = config.attackDelay
                            damage = (DEFAULT_ATTACK_DAMAGE * config.attackScaling).roundToInt()
                            extraRange = config.attackRange
                        }
                        // refactor to be a part of the stats?
                        val lifeComponent = add<LifeComponent> {
                            maxHealth = DEFAULT_LIFE * config.lifeScaling
                            health = maxHealth
                        }
                        val statComponent = add<StatComponent> {
                            currentHealth = config.stats.currentHealth
                            maxHealth = config.stats.maxHealth
                            currentMana = config.stats.currentMana
                            maxMana = config.stats.maxMana
                            attackDamage = config.stats.attackDamage
                            attackPercent = config.stats.attackPercent
                            attackSpeed = config.stats.attackSpeed
                            defense = config.stats.defense
                            defensePercent = config.stats.defensePercent
                            moveSpeed = config.stats.moveSpeed
                        }
                        add<PlayerComponent>()
                        add<StateComponent>()
                        add<InventoryComponent>()
                        add<CollisionComponent>()
                    }
                }
                ConfigurationType.NON_PLAYER -> {
                    val config = configuration as NonPlayerConfiguration
                    world.entity {
                        val imageComponent = add<ImageComponent> {
                            image = FlipImage().apply {
                                setPosition(location.x, location.y)
                                setSize(size(config.model).x, size(config.model).y)
                                setScaling(Scaling.fill)
                                color = config.color
                            }
                        }
                        add<AnimationComponent> {
                            nextAnimation(config.model, AnimationType.IDLE)
                        }
                        val physicsComponent = add<PhysicsComponent> {
                            body = bodyFromImageAndConfiguration(physicsWorld, imageComponent.image, config.bodyType, config.physicsScaling, config.physicsOffset)
                        }
                        if (config.speedScaling > 0f) {
                            add<MoveComponent>() {
                                speed = DEFAULT_SPEED * config.speedScaling
                            }
                        }
                        if (config.canAttack) {
                            add<AttackComponent> {
                                maxDelay = config.attackDelay
                                damage = (DEFAULT_ATTACK_DAMAGE * config.attackScaling).roundToInt()
                                extraRange = config.attackRange
                            }
                        }
                        if (config.lifeScaling > 0f) {
                            add<LifeComponent> {
                                maxHealth = DEFAULT_LIFE * config.lifeScaling
                                health = maxHealth
                            }
                        }
                        if (config.hasStats) {
                            add<StatComponent> {
                                currentHealth = config.stats.currentHealth
                                maxHealth = config.stats.maxHealth
                                currentMana = config.stats.currentMana
                                maxMana = config.stats.maxMana
                                attackDamage = config.stats.attackDamage
                                attackPercent = config.stats.attackPercent
                                attackSpeed = config.stats.attackSpeed
                                defense = config.stats.defense
                                defensePercent = config.stats.defensePercent
                                moveSpeed = config.stats.moveSpeed
                            }
                        }
                        add<CollisionComponent>()
                        add<NonPlayerComponent>()
                        if (config.hasAiBehavior) {
                            add<AiComponent>() {
                                treePath = config.aiTreePath
                            }
                        }
                        physicsComponent.body.circle(4f) {
                            isSensor = true
                            userData = AI_SENSOR
                        }
                        if (config.canBattle) { add<BattleComponent>() }
                    }
                }
                else -> { gdxError("Entity has no configuration.") }
            }
        }
        world.remove(entity)
    }

    private fun size(model : AnimationModel) = cachedSizes.getOrPut(model) {
        val regions: Array<TextureAtlas.AtlasRegion> = if (model.hasDirection) {
            atlas.findRegions("${model.atlasKey}/${AnimationType.IDLE.atlasKey}${AnimationDirection.TO.atlasKey}")
        } else {
            atlas.findRegions("${model.atlasKey}/${AnimationType.IDLE.atlasKey}")
        }
        if (regions.isEmpty) { gdxError("There are no regions for the idle animation for the ${model.atlasKey} model") }
        val firstFrame = regions.first()
        vec2(firstFrame.originalWidth * UNIT_SCALE, firstFrame.originalHeight * UNIT_SCALE)
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is MapChangeEvent -> {
                val entityLayer = event.map.layer("entities")
                entityLayer.objects.forEach { entity ->
                    world.entity {
                        add<EntityCreationComponent> {
                            this.configurationType = getConfigurationType(entity.name)
                            this.configuration = getConfiguration(entity.name)
                            this.entityName = entity.name
                            this.location.set(entity.x * UNIT_SCALE, entity.y * UNIT_SCALE)
                        }
                    }
                }
                return true
            }
        }
        return false
    }

    companion object {
        private val log = logger<EntityCreationSystem>()
        const val HIT_BOX_SENSOR = "hitbox"
        const val AI_SENSOR = "aiSensor"
        const val PLAYER_NAME = "player"

        val SLIME_DIALOG_CONFIGURATION = SpawnConfiguration(
            AnimationModel.SLIME_BLUE,
            lifeScaling = 0f,
            physicsScaling = vec2(0.3f, 0.3f),
            physicsOffset = vec2(0f, -2f * UNIT_SCALE),
            dialogId = DialogId.SLIME
        )
        val SIGN_1_CONFIGURATION = SpawnConfiguration(
            AnimationModel.SIGN,
            lifeScaling = 0f,
            speedScaling = 0f,
            bodyType = StaticBody,
            canAttack = false,
            dialogId = DialogId.SIGN_1
        )
        val SIGN_2_CONFIGURATION = SpawnConfiguration(
            AnimationModel.SIGN,
            lifeScaling = 0f,
            speedScaling = 0f,
            bodyType = StaticBody,
            canAttack = false,
            dialogId = DialogId.SIGN_2
        )
    }
}
