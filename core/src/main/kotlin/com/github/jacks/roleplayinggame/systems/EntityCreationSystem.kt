package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.utils.Scaling
import com.github.jacks.roleplayinggame.RolePlayingGame.Companion.UNIT_SCALE
import com.github.jacks.roleplayinggame.saveManager.CharacterData
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
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType.DynamicBody
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.utils.Array
import com.github.jacks.roleplayinggame.actors.FlipImage
import com.github.jacks.roleplayinggame.components.AiComponent
import com.github.jacks.roleplayinggame.components.AnimationDirection
import com.github.jacks.roleplayinggame.components.AttackComponent
import com.github.jacks.roleplayinggame.components.BattleComponent
import com.github.jacks.roleplayinggame.components.CollisionComponent
import com.github.jacks.roleplayinggame.components.DEFAULT_ATTACK_DAMAGE
import com.github.jacks.roleplayinggame.components.DEFAULT_SPEED
import com.github.jacks.roleplayinggame.components.DialogComponent
import com.github.jacks.roleplayinggame.components.EnemyStats
import com.github.jacks.roleplayinggame.components.InventoryComponent
import com.github.jacks.roleplayinggame.configurations.DialogId
import com.github.jacks.roleplayinggame.components.MoveComponent
import com.github.jacks.roleplayinggame.components.NonPlayerComponent
import com.github.jacks.roleplayinggame.components.NonPlayerConfiguration
import com.github.jacks.roleplayinggame.components.PhysicsComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.PlayerConfiguration
import com.github.jacks.roleplayinggame.components.AbilityComponent
import com.github.jacks.roleplayinggame.components.ShopComponent
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.components.StateComponent
import com.github.jacks.roleplayinggame.configurations.ConfigurationType
import com.github.jacks.roleplayinggame.configurations.EnemyConfiguration
import ktx.actors.alpha
import ktx.box2d.circle
import ktx.log.logger
import kotlin.math.roundToInt

@AllOf([EntityCreationComponent::class])
class EntityCreationSystem(
    private val physicsWorld : World,
    private val atlas : TextureAtlas,
    private val entityCreationComponents : ComponentMapper<EntityCreationComponent>,
) : EventListener, IteratingSystem() {

    private val cachedConfigurations = mutableMapOf<String, SpawnConfiguration>()
    private val cachedSizes = mutableMapOf<AnimationModel, Vector2>()
    private val playerEntities = world.family(allOf = arrayOf(PlayerComponent::class))

    override fun onTickEntity(entity: Entity) {
        with(entityCreationComponents[entity]) {
            when(configurationType) {
                ConfigurationType.PLAYER -> {
                    val config = configuration as PlayerConfiguration
                    createPlayerEntity(config, location, this.characterId)
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
                                alpha = 0f
                                addAction(Actions.fadeIn(0.5f))
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
                        if (config.canBattle) { add<BattleComponent> {
                            toMap = config.battleMap
                            spawnerId = this@with.spawnerId
                            spawnerMapId = this@with.spawnerMapId
                        } }
                        if (this@with.shopId > 0) {
                            add<ShopComponent> { shopId = this@with.shopId }
                        }
                        if (config.dialogId != DialogId.NO_DIALOG) {
                            add<DialogComponent> { dialogId = config.dialogId }
                        }
                    }
                }
                ConfigurationType.ENEMY -> {
                    val config = configuration as EnemyConfiguration
                    createEnemyEntity(config, location, this.spawnerId, this.spawnerMapId)
                }
                else -> { gdxError("Entity has no configuration: $configurationType") }
            }
        }
        world.remove(entity)
    }

    /** Spawn a player entity directly at [location] for [charId]. */
    fun spawnPlayerCharacter(charId: Int, location: Vector2) {
        val config = com.github.jacks.roleplayinggame.configurations.Configurations.PLAYER_CONFIGURATION
        createPlayerEntity(config, location, charId)
    }

    private fun createPlayerEntity(
        config: PlayerConfiguration,
        location: Vector2,
        charId: Int,
    ) {
        val charData = world.system<PartySystem>().getCharacterData(charId)
        world.entity {
            val imageComponent = add<ImageComponent> {
                image = FlipImage().apply {
                    setPosition(location.x, location.y)
                    setSize(size(config.model).x, size(config.model).y)
                    setScaling(Scaling.fill)
                    color = config.color
                    alpha = 0f
                    addAction(Actions.fadeIn(0.5f))
                }
            }
            add<AnimationComponent> {
                nextAnimation(config.model, AnimationType.IDLE)
            }
            add<PhysicsComponent> {
                body = bodyFromImageAndConfiguration(physicsWorld, imageComponent.image, config.bodyType, config.physicsScaling, config.physicsOffset)
            }
            add<MoveComponent> {
                speed = DEFAULT_SPEED * config.speedScaling
            }
            add<AttackComponent> {
                maxDelay = config.attackDelay
                damage = (DEFAULT_ATTACK_DAMAGE * config.attackScaling).roundToInt()
                extraRange = config.attackRange
            }
            add<StatComponent> {
                stats = charData
            }
            add<PlayerComponent> { characterId = charId }
            add<StateComponent>()
            add<InventoryComponent> {
                charData.equippedItems.forEach { (category, itemId) ->
                    equippedItems[category] = itemId
                }
            }
            add<CollisionComponent>()
            add<AbilityComponent> {
                charData.unlockedAbilityIds.forEach { unlockedAbilityIds.add(it) }
            }
        }
    }

    private fun createEnemyEntity(
        config: EnemyConfiguration,
        location: Vector2,
        spawnerId: Int,
        spawnerMapId: Int,
    ) {
        world.entity {
            val imageComponent = add<ImageComponent> {
                image = FlipImage().apply {
                    setPosition(location.x, location.y)
                    setSize(size(config.animationModel).x, size(config.animationModel).y)
                    setScaling(Scaling.fill)
                    color = config.color
                    alpha = 0f
                    addAction(Actions.fadeIn(0.5f))
                }
            }
            add<AnimationComponent> {
                nextAnimation(config.animationModel, AnimationType.IDLE)
            }
            val physicsComponent = add<PhysicsComponent> {
                body = bodyFromImageAndConfiguration(physicsWorld, imageComponent.image, DynamicBody, config.physicsScaling, config.physicsOffset)
            }
            if (config.speedScaling > 0f) {
                add<MoveComponent> {
                    speed = DEFAULT_SPEED * config.speedScaling
                }
            }
            add<StatComponent> {
                stats = config.stats.copy()
            }
            add<CollisionComponent>()
            add<NonPlayerComponent>()
            if (config.hasAiBehavior) {
                add<AiComponent> {
                    treePath = config.aiTreePath
                }
            }
            physicsComponent.body.circle(4f) {
                isSensor = true
                userData = AI_SENSOR
            }
            add<BattleComponent> {
                toMap = config.battleMap
                this.spawnerId    = spawnerId
                this.spawnerMapId = spawnerMapId
            }
        }
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
                val activeCharId = world.system<PartySystem>().activeOverworldCharacterId
                entityLayer.objects.forEach { entity ->
                    world.entity {
                        add<EntityCreationComponent> {
                            this.configurationType = getConfigurationType(entity.name)
                            this.configuration = getConfiguration(entity.name)
                            this.entityName = entity.name
                            this.location.set(entity.x * UNIT_SCALE, entity.y * UNIT_SCALE)
                            this.shopId = entity.properties.get("shopId", 0, Int::class.java)
                            // Pass the active character ID so the player entity loads the correct CharacterData
                            if (this.configurationType == ConfigurationType.PLAYER) {
                                this.characterId = activeCharId
                            }
                        }
                    }
                }
                return true
            }
            else -> return false
        }
    }

    companion object {
        private val log = logger<EntityCreationSystem>()
        const val HIT_BOX_SENSOR = "hitbox"
        const val AI_SENSOR = "aiSensor"
        const val PLAYER_NAME = "player"

    }
}
