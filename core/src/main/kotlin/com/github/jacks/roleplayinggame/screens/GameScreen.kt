package com.github.jacks.roleplayinggame.screens

import com.badlogic.gdx.ai.GdxAI
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.github.jacks.roleplayinggame.components.AiComponent.Companion.AiComponentListener
import com.github.jacks.roleplayinggame.components.FloatingTextComponent.Companion.FloatingTextComponentListener
import com.github.jacks.roleplayinggame.components.ImageComponent.Companion.ImageComponentListener
import com.github.jacks.roleplayinggame.components.PhysicsComponent.Companion.PhysicsComponentListener
import com.github.jacks.roleplayinggame.components.StateComponent.Companion.StateComponentListener
import com.github.jacks.roleplayinggame.events.MapChangeEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.input.PlayerKeyboardInputProcessor
import com.github.jacks.roleplayinggame.input.gdxInputProcessor
import com.github.jacks.roleplayinggame.systems.AiSystem
import com.github.jacks.roleplayinggame.systems.AnimationSystem
import com.github.jacks.roleplayinggame.systems.AttackSystem
import com.github.jacks.roleplayinggame.systems.AudioSystem
import com.github.jacks.roleplayinggame.systems.CameraSystem
import com.github.jacks.roleplayinggame.systems.CollisionDespawnSystem
import com.github.jacks.roleplayinggame.systems.CollisionSpawnSystem
import com.github.jacks.roleplayinggame.systems.DeathSystem
import com.github.jacks.roleplayinggame.systems.DebugSystem
import com.github.jacks.roleplayinggame.systems.EntitySpawnSystem
import com.github.jacks.roleplayinggame.systems.FloatingTextSystem
import com.github.jacks.roleplayinggame.systems.InventorySystem
import com.github.jacks.roleplayinggame.systems.LifeSystem
import com.github.jacks.roleplayinggame.systems.LootSystem
import com.github.jacks.roleplayinggame.systems.MoveSystem
import com.github.jacks.roleplayinggame.systems.PhysicsSystem
import com.github.jacks.roleplayinggame.systems.RenderSystem
import com.github.jacks.roleplayinggame.systems.StateSystem
import com.github.jacks.roleplayinggame.ui.viewmodels.GameViewModel
import com.github.jacks.roleplayinggame.ui.viewmodels.InventoryViewModel
import com.github.jacks.roleplayinggame.ui.views.gameView
import com.github.jacks.roleplayinggame.ui.views.inventoryView
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.world
import ktx.app.KtxScreen
import ktx.assets.disposeSafely
import ktx.box2d.createWorld
import ktx.log.logger
import ktx.math.vec2
import ktx.scene2d.actors

class GameScreen : KtxScreen {

    private val gameStage : Stage = Stage(ExtendViewport(16f, 9f))
    private val uiStage : Stage = Stage(ExtendViewport(320f, 180f))
    private val textureAtlas : TextureAtlas = TextureAtlas("assets/graphics/gameObjects.atlas")
    private var currentMap : TiledMap? = null
    private val physicsWorld = createWorld(gravity = vec2()).apply {
        autoClearForces = false
    }

    private val entityWorld : World = world {
        injectables {
            add(gameStage)
            add("uiStage", uiStage)
            add(textureAtlas)
            add(physicsWorld)
        }

        components {
            add<ImageComponentListener>()
            add<PhysicsComponentListener>()
            add<FloatingTextComponentListener>()
            add<StateComponentListener>()
            add<AiComponentListener>()
        }

        systems {
            add<EntitySpawnSystem>()
            add<CollisionSpawnSystem>()
            add<CollisionDespawnSystem>()
            add<MoveSystem>()
            add<AttackSystem>()
            add<LootSystem>()
            add<InventorySystem>()
            add<DeathSystem>()
            add<LifeSystem>()
            add<PhysicsSystem>()
            add<AnimationSystem>()
            add<StateSystem>()
            add<AiSystem>()
            add<CameraSystem>()
            add<FloatingTextSystem>()
            add<RenderSystem>()
            add<AudioSystem>()
            add<DebugSystem>()
        }
    }

    init {
        uiStage.actors {
            gameView(GameViewModel(entityWorld, gameStage))
            inventoryView(InventoryViewModel(entityWorld, gameStage)) {
                isVisible = false
            }
        }
    }

    override fun show() {
        log.debug { "Game Screen is shown" }

        entityWorld.systems.forEach { system ->
            if (system is EventListener) {
                gameStage.addListener(system)
            }
        }

        currentMap = TmxMapLoader().load("maps/map_1.tmx")
        gameStage.fire(MapChangeEvent(currentMap!!))

        PlayerKeyboardInputProcessor(entityWorld, uiStage)
        gdxInputProcessor(uiStage)
    }

    override fun resize(width: Int, height: Int) {
        gameStage.viewport.update(width, height, true)
        uiStage.viewport.update(width, height, true)
    }

    override fun render(delta: Float) {
        val deltaTime = delta.coerceAtMost(0.25f)
        GdxAI.getTimepiece().update(deltaTime)
        entityWorld.update(deltaTime)
    }

    override fun dispose() {
        gameStage.disposeSafely()
        uiStage.disposeSafely()
        textureAtlas.disposeSafely()
        entityWorld.dispose()
        currentMap?.disposeSafely()
    }

    companion object {
         private val log = logger<GameScreen>()
    }
}
