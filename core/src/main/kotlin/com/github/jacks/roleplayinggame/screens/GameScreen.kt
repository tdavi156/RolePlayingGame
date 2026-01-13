package com.github.jacks.roleplayinggame.screens

import com.badlogic.gdx.ai.GdxAI
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.github.jacks.roleplayinggame.RolePlayingGame
import com.github.jacks.roleplayinggame.components.AiComponent.Companion.AiComponentListener
import com.github.jacks.roleplayinggame.components.FloatingTextComponent.Companion.FloatingTextComponentListener
import com.github.jacks.roleplayinggame.components.ImageComponent.Companion.ImageComponentListener
import com.github.jacks.roleplayinggame.components.PhysicsComponent.Companion.PhysicsComponentListener
import com.github.jacks.roleplayinggame.components.StateComponent.Companion.StateComponentListener
import com.github.jacks.roleplayinggame.input.PlayerKeyboardInputProcessor
import com.github.jacks.roleplayinggame.input.gdxInputProcessor
import com.github.jacks.roleplayinggame.systems.AiSystem
import com.github.jacks.roleplayinggame.systems.AnimationSystem
import com.github.jacks.roleplayinggame.systems.AttackSystem
import com.github.jacks.roleplayinggame.systems.CameraSystem
import com.github.jacks.roleplayinggame.systems.CollisionDespawnSystem
import com.github.jacks.roleplayinggame.systems.CollisionSpawnSystem
import com.github.jacks.roleplayinggame.systems.DeathSystem
import com.github.jacks.roleplayinggame.systems.DebugSystem
import com.github.jacks.roleplayinggame.systems.DialogSystem
import com.github.jacks.roleplayinggame.systems.EntityCreationSystem
import com.github.jacks.roleplayinggame.systems.FloatingTextSystem
import com.github.jacks.roleplayinggame.systems.InventorySystem
import com.github.jacks.roleplayinggame.systems.LifeSystem
import com.github.jacks.roleplayinggame.systems.LootSystem
import com.github.jacks.roleplayinggame.systems.MoveSystem
import com.github.jacks.roleplayinggame.systems.PhysicsSystem
import com.github.jacks.roleplayinggame.systems.PortalSystem
import com.github.jacks.roleplayinggame.systems.RenderSystem
import com.github.jacks.roleplayinggame.systems.StateSystem
import com.github.jacks.roleplayinggame.ui.viewmodels.DialogViewModel
import com.github.jacks.roleplayinggame.ui.viewmodels.MainGameViewModel
import com.github.jacks.roleplayinggame.ui.viewmodels.InventoryViewModel
import com.github.jacks.roleplayinggame.ui.views.PauseView
import com.github.jacks.roleplayinggame.ui.views.dialogView
import com.github.jacks.roleplayinggame.ui.views.inventoryView
import com.github.jacks.roleplayinggame.ui.views.mainGameView
import com.github.jacks.roleplayinggame.ui.views.pauseView
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.world
import ktx.app.KtxScreen
import ktx.assets.disposeSafely
import ktx.box2d.createWorld
import ktx.log.logger
import ktx.math.vec2
import ktx.preferences.get
import ktx.scene2d.actors

class GameScreen(game : RolePlayingGame) : KtxScreen {
    private val preferences = game.preferences
    private val gameStage = game.gameStage
    private val uiStage = game.uiStage
    private val textureAtlas : TextureAtlas = TextureAtlas("assets/graphics/gameObjects.atlas")
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
            add<EntityCreationSystem>()
            add<CollisionSpawnSystem>()
            add<CollisionDespawnSystem>()
            add<InventorySystem>()
            add<PortalSystem>()
            add<MoveSystem>()
            add<AttackSystem>()
            add<LootSystem>()
            add<DialogSystem>()
            add<DeathSystem>()
            add<LifeSystem>()
            add<PhysicsSystem>()
            add<AnimationSystem>()
            add<StateSystem>()
            add<AiSystem>()
            add<CameraSystem>()
            add<FloatingTextSystem>()
            add<RenderSystem>()
            //add<AudioSystem>()
            add<DebugSystem>()
        }
    }

    init {
        uiStage.actors {
            log.debug { "Stage is initialized" }

            // main UI, actor.get(0)
            mainGameView(MainGameViewModel(entityWorld, gameStage))

            // dialog UI, actor.get(1)
            dialogView(DialogViewModel(gameStage))

            // pause UI, actor.get(2)
            pauseView { this.isVisible = false }

            // inventory UI, actor.get(3)
            inventoryView(InventoryViewModel(entityWorld, gameStage)) { this.isVisible = false }
        }
    }

    override fun show() {
        log.debug { "Game Screen is shown" }

        entityWorld.systems.forEach { system ->
            if (system is EventListener) {
                gameStage.addListener(system)
            }
        }

        entityWorld.system<PortalSystem>().setMap(preferences["current_map", "map_1"])
        PlayerKeyboardInputProcessor(entityWorld, gameStage, uiStage)
        gdxInputProcessor(uiStage)
    }

    private fun pauseWorld(pause : Boolean) {
        val mandatorySystems = setOf(
            AnimationSystem::class,
            CameraSystem::class,
            RenderSystem::class,
            DebugSystem::class
        )

        entityWorld.systems
            .filter { it::class !in mandatorySystems}
            .forEach { it.enabled = !pause }

        uiStage.actors.filterIsInstance<PauseView>().first().isVisible = pause
    }

    override fun pause() = pauseWorld(true)
    override fun resume() = pauseWorld(false)

    override fun render(delta: Float) {
        val deltaTime = delta.coerceAtMost(0.25f)
        GdxAI.getTimepiece().update(deltaTime)
        entityWorld.update(deltaTime)
    }

    override fun dispose() {
        textureAtlas.disposeSafely()
        entityWorld.dispose()
    }

    companion object {
         private val log = logger<GameScreen>()
    }
}
