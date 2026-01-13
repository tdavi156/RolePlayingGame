package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.Color
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
import com.github.jacks.roleplayinggame.components.DialogComponent
import com.github.jacks.roleplayinggame.components.DialogId
import com.github.jacks.roleplayinggame.components.InventoryComponent
import com.github.jacks.roleplayinggame.components.LifeComponent
import com.github.jacks.roleplayinggame.components.LootComponent
import com.github.jacks.roleplayinggame.components.MoveComponent
import com.github.jacks.roleplayinggame.components.PhysicsComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.components.SpawnerComponent
import com.github.jacks.roleplayinggame.components.SpawnerType
import com.github.jacks.roleplayinggame.components.StatComponent
import com.github.jacks.roleplayinggame.components.StateComponent
import ktx.box2d.circle
import ktx.log.logger
import kotlin.math.roundToInt

@AllOf([SpawnComponent::class])
class EntityCreationSystem(
    private val physicsWorld : World,
    private val atlas : TextureAtlas,
    private val spawnComponents : ComponentMapper<SpawnComponent>,
) : EventListener, IteratingSystem() {

    private val preferences : Preferences by lazy { Gdx.app.getPreferences("rolePlayingGamePrefs") }
    private val cachedConfigurations = mutableMapOf<String, SpawnConfiguration>()
    private val cachedSizes = mutableMapOf<AnimationModel, Vector2>()
    private val playerEntities = world.family(allOf = arrayOf(PlayerComponent::class))

    override fun onTickEntity(entity: Entity) {
        with(spawnComponents[entity]) {
            val configuration = spawnConfiguration(name)
            val relativeSize = size(configuration.model)
            val spawnPrefsName = spawnComponents[entity].prefsName

            val spawnedEntity = world.entity {
                val imageComponent = add<ImageComponent> {
                    image = FlipImage().apply {
                        setPosition(location.x, location.y)
                        setSize(relativeSize.x, relativeSize.y)
                        setScaling(Scaling.fill)
                        color = this@with.color
                    }
                }

                add<AnimationComponent> {
                    nextAnimation(configuration.model, AnimationType.IDLE)
                }

                val physicsComponent = add<PhysicsComponent> {
                    body = bodyFromImageAndConfiguration(physicsWorld, imageComponent.image, configuration)
                }

                if (configuration.dialogId != DialogId.NONE) {
                    add<DialogComponent> {
                        dialogId = configuration.dialogId
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

                if (name == PLAYER_NAME) {
                    add<PlayerComponent>()
                    add<StateComponent>()
                    add<InventoryComponent>()
                }

                if (configuration.stats != null) {
                    add<StatComponent> {
                        prefsName = spawnPrefsName
                        currentHealth = configuration.stats.currentHealth
                        maxHealth = configuration.stats.maxHealth
                        currentMana = configuration.stats.currentMana
                        maxMana = configuration.stats.maxMana
                        attackDamage = configuration.stats.attackDamage
                        attackPercent = configuration.stats.attackPercent
                        attackSpeed = configuration.stats.attackSpeed
                        defense = configuration.stats.defense
                        defensePercent = configuration.stats.defensePercent
                        moveSpeed = configuration.stats.moveSpeed
                    }
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

    private fun spawnConfiguration(name : String) : SpawnConfiguration = cachedConfigurations.getOrPut(name) {
        when (name) {
            "player" -> PLAYER_CONFIGURATION
            "slime" -> SLIME_CONFIGURATION
            "slimeDialog" -> SLIME_DIALOG_CONFIGURATION
            "chest" -> CHEST_CONFIGURATION
            "sign_1" -> SIGN_1_CONFIGURATION
            "sign_2" -> SIGN_2_CONFIGURATION
            else -> gdxError("Type $name has no spawn configuration")
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
                log.debug { "MapChangeEvent" }
                val entityLayer = event.map.layer("entities")
                val spawnerLayer = event.map.layer("spawners")
                entityLayer.objects.forEach { mapObject ->
                    val name = mapObject.name ?: gdxError("Map Object $mapObject has no name")
                    val prefsName = mapObject.properties.get("preferencesName")
                    if (name == "player" && playerEntities.isNotEmpty) {
                        return@forEach
                    }

                    if (prefsName != null && !preferences.getBoolean("${prefsName}_shouldSpawn", true)) {
                        return@forEach
                    }

                    world.entity {
                        add<SpawnComponent> {
                            this.name = name
                            this.prefsName = prefsName?.toString() ?: ""
                            this.location.set(mapObject.x * UNIT_SCALE, mapObject.y * UNIT_SCALE)
                            this.color = mapObject.property("color", Color.WHITE)
                        }
                    }
                }

                spawnerLayer.objects.forEach { spawner ->
                    // check if spawnerEntities is empty by checking any entities with a spawnerComponent
                    // to make sure they were removed properly
                    // if the checks are good then add entities
                    world.entity {
                        add<SpawnerComponent> {
                            this.spawnerId = spawner.id
                            this.mapId = 1
                            this.location.set(spawner.x, spawner.y)
                            this.spawnerType = SpawnerType.SLIME_SPAWNER
                            this.spawnTimer = 60f
                            this.currentTime = 0f
                            this.isSpawned = false
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
        val PLAYER_CONFIGURATION = SpawnConfiguration(
            AnimationModel.PLAYER,
            stats = StatComponent(
                currentHealth = 30f,
                maxHealth = 30f,
                attackDamage = 5f,
                defense = 1f,
                moveSpeed = 1f
            ),
            speedScaling = 1.5f,
            lifeScaling = 1f,
            attackRange = 0.75f,
            attackScaling = 1f,
            physicsScaling = vec2(0.3f, 0.3f,),
            physicsOffset = vec2(0f, -10f * UNIT_SCALE)
        )
        val SLIME_CONFIGURATION = SpawnConfiguration(
            AnimationModel.SLIME,
            stats = StatComponent(
                currentHealth = 10f,
                maxHealth = 10f,
                attackDamage = 3f,
                defense = 0f,
                moveSpeed = 1f
            ),
            speedScaling = 0.5f,
            lifeScaling = 1f,
            attackRange = 1f,
            attackScaling = 1f,
            physicsScaling = vec2(0.3f, 0.3f),
            physicsOffset = vec2(0f, -2f * UNIT_SCALE),
            aiTreePath = "slimeBehavior.tree"
        )
        val CHEST_CONFIGURATION = SpawnConfiguration(
            AnimationModel.CHEST,
            speedScaling = 0f,
            bodyType = StaticBody,
            canAttack = false,
            lifeScaling = 0f,
            lootable = true
        )
        val SLIME_DIALOG_CONFIGURATION = SpawnConfiguration(
            AnimationModel.SLIME,
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
