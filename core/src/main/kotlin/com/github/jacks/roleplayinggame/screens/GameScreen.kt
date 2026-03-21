package com.github.jacks.roleplayinggame.screens

import com.badlogic.gdx.ai.GdxAI
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.github.jacks.roleplayinggame.RolePlayingGame
import com.github.jacks.roleplayinggame.components.AiComponent.Companion.AiComponentListener
import com.github.jacks.roleplayinggame.components.FloatingTextComponent.Companion.FloatingTextComponentListener
import com.github.jacks.roleplayinggame.components.ImageComponent.Companion.ImageComponentListener
import com.github.jacks.roleplayinggame.components.PhysicsComponent.Companion.PhysicsComponentListener
import com.github.jacks.roleplayinggame.components.StateComponent.Companion.StateComponentListener
import com.github.jacks.roleplayinggame.components.BattleEndReason
import com.github.jacks.roleplayinggame.components.PhysicsComponent
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.events.BattleEndEvent
import com.github.jacks.roleplayinggame.events.BattleEndTransitionStartEvent
import com.github.jacks.roleplayinggame.events.BattleEvent
import com.github.jacks.roleplayinggame.events.BattleRewardEvent
import com.github.jacks.roleplayinggame.events.BattleTransitionStartEvent
import com.github.jacks.roleplayinggame.events.AbilityViewClosedEvent
import com.github.jacks.roleplayinggame.events.CombatInventoryClosedEvent
import com.github.jacks.roleplayinggame.events.CombatInventoryOpenEvent
import com.github.jacks.roleplayinggame.events.CombatItemUseDismissedEvent
import com.github.jacks.roleplayinggame.events.GameResumeEvent
import com.github.jacks.roleplayinggame.events.RewardDismissedEvent
import com.github.jacks.roleplayinggame.events.InitializeGameEvent
import com.github.jacks.roleplayinggame.events.SkillViewClosedEvent
import com.github.jacks.roleplayinggame.events.SwitchActiveCharacterEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.jacks.roleplayinggame.input.PlayerKeyboardInputProcessor
import com.github.jacks.roleplayinggame.input.gdxInputProcessor
import com.github.jacks.roleplayinggame.events.ShopClosedEvent
import com.github.jacks.roleplayinggame.events.ShopOpenEvent
import com.github.jacks.roleplayinggame.systems.AbilitySystem
import com.github.jacks.roleplayinggame.systems.AiSystem
import com.github.jacks.roleplayinggame.systems.PartySystem
import com.github.jacks.roleplayinggame.systems.AnimationSystem
import com.github.jacks.roleplayinggame.systems.BattleSystem
import com.github.jacks.roleplayinggame.systems.ResourceSystem
import com.github.jacks.roleplayinggame.systems.SettingsSystem
import com.github.jacks.roleplayinggame.systems.QuestSystem
import com.github.jacks.roleplayinggame.systems.ShopSystem
import com.github.jacks.roleplayinggame.systems.CameraSystem
import com.github.jacks.roleplayinggame.systems.CollisionDespawnSystem
import com.github.jacks.roleplayinggame.systems.CollisionSpawnSystem
import com.github.jacks.roleplayinggame.systems.DebugSystem
import com.github.jacks.roleplayinggame.systems.DialogSystem
import com.github.jacks.roleplayinggame.systems.InteractionSystem
import com.github.jacks.roleplayinggame.systems.EntityCreationSystem
import com.github.jacks.roleplayinggame.systems.FloatingTextSystem
import com.github.jacks.roleplayinggame.systems.InitializeGameSystem
import com.github.jacks.roleplayinggame.systems.InventorySystem
import com.github.jacks.roleplayinggame.systems.StatSystem
import com.github.jacks.roleplayinggame.systems.LootSystem
import com.github.jacks.roleplayinggame.systems.MapSystem
import com.github.jacks.roleplayinggame.systems.MoveSystem
import com.github.jacks.roleplayinggame.systems.PhysicsSystem
import com.github.jacks.roleplayinggame.systems.PortalSystem
import com.github.jacks.roleplayinggame.systems.RenderSystem
import com.github.jacks.roleplayinggame.saveManager.SaveManager
import com.github.jacks.roleplayinggame.systems.SpawnerSystem
import com.github.jacks.roleplayinggame.systems.StateSystem
import com.github.jacks.roleplayinggame.ui.viewmodels.BattleViewModel
import com.github.jacks.roleplayinggame.ui.viewmodels.RewardViewModel
import com.github.jacks.roleplayinggame.ui.viewmodels.ShopViewModel
import com.github.jacks.roleplayinggame.ui.views.BattleView
import com.github.jacks.roleplayinggame.ui.views.RewardView
import com.github.jacks.roleplayinggame.ui.views.ShopView
import com.github.jacks.roleplayinggame.ui.views.rewardView
import com.github.jacks.roleplayinggame.ui.views.shopView
import com.github.jacks.roleplayinggame.ui.views.FadeInOutView
import com.github.jacks.roleplayinggame.ui.views.MainGameView
import com.github.jacks.roleplayinggame.ui.viewmodels.CharacterInfoViewModel
import com.github.jacks.roleplayinggame.ui.viewmodels.DialogViewModel
import com.github.jacks.roleplayinggame.ui.viewmodels.MainGameViewModel
import com.github.jacks.roleplayinggame.ui.viewmodels.InventoryViewModel
import com.github.jacks.roleplayinggame.ui.viewmodels.MapViewModel
import com.github.jacks.roleplayinggame.ui.viewmodels.MenuViewModel
import com.github.jacks.roleplayinggame.ui.viewmodels.QuestViewModel
import com.github.jacks.roleplayinggame.ui.viewmodels.SettingsViewModel
import com.github.jacks.roleplayinggame.ui.viewmodels.AbilityViewModel
import com.github.jacks.roleplayinggame.ui.viewmodels.SkillViewModel
import com.github.jacks.roleplayinggame.ui.views.AbilityView
import com.github.jacks.roleplayinggame.ui.views.BackgroundView
import com.github.jacks.roleplayinggame.ui.views.PauseView
import com.github.jacks.roleplayinggame.ui.views.SkillView
import com.github.jacks.roleplayinggame.ui.views.abilityView
import com.github.jacks.roleplayinggame.ui.views.backgroundView
import com.github.jacks.roleplayinggame.ui.views.battleView
import com.github.jacks.roleplayinggame.ui.views.characterInfoView
import com.github.jacks.roleplayinggame.ui.views.dialogView
import com.github.jacks.roleplayinggame.ui.views.fadeInOutView
import com.github.jacks.roleplayinggame.ui.views.inventoryView
import com.github.jacks.roleplayinggame.ui.views.mainGameView
import com.github.jacks.roleplayinggame.ui.views.mapView
import com.github.jacks.roleplayinggame.ui.views.menuView
import com.github.jacks.roleplayinggame.ui.views.pauseView
import com.github.jacks.roleplayinggame.ui.views.questView
import com.github.jacks.roleplayinggame.ui.views.settingsView
import com.github.jacks.roleplayinggame.ui.views.skillView
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.world
import ktx.app.KtxScreen
import ktx.assets.disposeSafely
import ktx.box2d.createWorld
import ktx.log.logger
import ktx.math.vec2
import ktx.scene2d.actors

class GameScreen(game : RolePlayingGame) : KtxScreen, EventListener {
    private val gameStage = game.gameStage
    private val uiStage = game.uiStage
    private val textureAtlas : TextureAtlas = TextureAtlas("assets/graphics/gameObjects.atlas")
    private val physicsWorld = createWorld(gravity = vec2()).apply { autoClearForces = false }
    private lateinit var fadeView: FadeInOutView
    private var currentBattleEnemy: Entity? = null

    /** Central save/load manager — injected into the ECS world so any system can receive it. */
    val saveManager = SaveManager()

    private val entityWorld : World = world {
        injectables {
            add(gameStage)
            add("uiStage", uiStage)
            add(textureAtlas)
            add(physicsWorld)
            add(saveManager)
        }

        components {
            add<ImageComponentListener>()
            add<PhysicsComponentListener>()
            add<FloatingTextComponentListener>()
            add<StateComponentListener>()
            add<AiComponentListener>()
        }

        systems {
            add<SettingsSystem>()
            add<ResourceSystem>()
            add<PartySystem>()
            add<InitializeGameSystem>()
            add<MapSystem>()
            add<EntityCreationSystem>()
            add<SpawnerSystem>()
            add<CollisionSpawnSystem>()
            add<CollisionDespawnSystem>()
            add<InventorySystem>()
            add<AbilitySystem>()
            add<StatSystem>()
            add<PortalSystem>()
            add<MoveSystem>()
            add<BattleSystem>()
            add<QuestSystem>()
            add<LootSystem>()
            add<DialogSystem>()
            add<InteractionSystem>()
            add<ShopSystem>()
            add<PhysicsSystem>()
            add<AnimationSystem>()
            add<StateSystem>()
            add<AiSystem>()
            add<CameraSystem>()
            add<FloatingTextSystem>()
            add<RenderSystem>()
            //add<AudioSystem>()
            //add<DebugSystem>()
        }
    }

    private val playerFamily   by lazy { entityWorld.family(allOf = arrayOf(PlayerComponent::class)) }
    private val physicsMapper  by lazy { entityWorld.mapper<PhysicsComponent>() }
    private val playerMapper   by lazy { entityWorld.mapper<PlayerComponent>() }
    private val settingsViewModel      = SettingsViewModel(uiStage, entityWorld.system<SettingsSystem>(), saveManager)
    private val characterInfoViewModel = com.github.jacks.roleplayinggame.ui.viewmodels.CharacterInfoViewModel(entityWorld, gameStage)
    private val rewardViewModel        = RewardViewModel(gameStage)
    private val shopViewModel          = ShopViewModel(entityWorld, gameStage)
    private val abilityViewModel       = AbilityViewModel(entityWorld, gameStage)

    init {
        uiStage.actors {
            log.debug { "UI Stage is initialized" }

            // overlay to fade in and out on screen transition
            // fade in out

            // main UI, actor.get(0)
            mainGameView(MainGameViewModel(entityWorld, gameStage), gameStage) { isVisible = true }

            // battle UI, actor.get(1)
            battleView(BattleViewModel(entityWorld, gameStage)) { isVisible = false }

            // reward overlay, actor.get(2)
            rewardView(rewardViewModel) { isVisible = false }

            // pauseView, actor.get(3)
            pauseView { isVisible = false }

            // dialog UI, actor.get(4)
            dialogView(DialogViewModel(gameStage))

            // background, actor.get(5)
            backgroundView() { isVisible = false }

            // characterInfo UI, actor.get(6)
            characterInfoView(characterInfoViewModel) { isVisible = false }

            // inventory UI, actor.get(7)
            inventoryView(InventoryViewModel(entityWorld, gameStage)) { isVisible = false }

            // skills UI, actor.get(8)
            skillView(SkillViewModel(entityWorld, gameStage)) { isVisible = false }

            // quests UI, actor.get(9)
            questView(QuestViewModel(entityWorld, gameStage)) { isVisible = false }

            // map UI, actor.get(10)
            mapView(MapViewModel(entityWorld, gameStage)) { isVisible = false }

            // menu UI, actor.get(11)
            menuView(MenuViewModel(stage, saveManager, entityWorld)) { isVisible = false }

            // settings UI, actor.get(12)
            settingsView(settingsViewModel) { isVisible = false }

            // shop UI, actor.get(13)
            shopView(shopViewModel) { isVisible = false }

            // ability UI, actor.get(14)
            abilityView(abilityViewModel) { isVisible = false }

            // fade overlay, actor.get(15) — drawn on top for screen transitions
            fadeView = fadeInOutView { isVisible = false }
        }
    }

    override fun show() {
        log.debug { "Game Screen is shown" }

        entityWorld.systems.forEach { system ->
            if (system is EventListener) {
                gameStage.addListener(system)
            }
        }
        gameStage.addListener(this)

        gameStage.fire(InitializeGameEvent())
        PlayerKeyboardInputProcessor(entityWorld, gameStage, uiStage, settingsViewModel, characterInfoViewModel)
        gdxInputProcessor(uiStage)
        disableOverworldSystems()
    }

    private fun pauseWorld(pause : Boolean) {
        val mandatorySystems = setOf(
            AnimationSystem::class,
            CameraSystem::class,
            RenderSystem::class,
            DebugSystem::class,
            // audio system?
            // inventory system?
        )

        entityWorld.systems
            .filter { it::class !in mandatorySystems}
            .forEach { it.enabled = !pause }

        if (!pause) {
            disableOverworldSystems()
        }

        uiStage.actors.filterIsInstance<PauseView>().first().isVisible = pause
    }

    override fun pause() = pauseWorld(true)
    override fun resume() = pauseWorld(false)

    override fun handle(event: Event): Boolean {
        when (event) {
            is SwitchActiveCharacterEvent -> {
                val partySystem = entityWorld.system<com.github.jacks.roleplayinggame.systems.PartySystem>()
                val newId = event.newCharacterId
                // Guard: same character, not unlocked, or out of range
                if (newId == partySystem.activeOverworldCharacterId) return true
                val charData = partySystem.characterDataMap[newId] ?: return true
                if (!charData.isUnlocked) return true
                switchActiveCharacter(newId)
                return true
            }
            is BattleTransitionStartEvent -> {
                enterBattleMode(event.enemy)
                return true
            }
            is BattleEndTransitionStartEvent -> {
                exitBattleMode(event.reason)
                return true
            }
            is BattleRewardEvent -> {
                uiStage.actors.filterIsInstance<RewardView>().first().isVisible = true
                return true
            }
            is RewardDismissedEvent -> {
                uiStage.actors.filterIsInstance<RewardView>().first().isVisible = false
                exitBattleMode(BattleEndReason.WIN)
                return true
            }
            is CombatInventoryOpenEvent -> {
                uiStage.actors.filterIsInstance<com.github.jacks.roleplayinggame.ui.views.InventoryView>().first().isVisible = true
                return true
            }
            is CombatInventoryClosedEvent -> {
                uiStage.actors.filterIsInstance<com.github.jacks.roleplayinggame.ui.views.InventoryView>().first().isVisible = false
                return true
            }
            is CombatItemUseDismissedEvent -> {
                uiStage.actors.filterIsInstance<com.github.jacks.roleplayinggame.ui.views.InventoryView>().first().isVisible = false
                return true
            }
            is ShopOpenEvent -> {
                entityWorld.systems
                    .filter { it::class !in shopMandatorySystems }
                    .forEach { it.enabled = false }
                uiStage.actors.filterIsInstance<ShopView>().first().isVisible = true
                return true
            }
            is ShopClosedEvent -> {
                entityWorld.systems.forEach { it.enabled = true }
                disableOverworldSystems()
                uiStage.actors.filterIsInstance<ShopView>().first().isVisible = false
                return true
            }
            is SkillViewClosedEvent -> {
                uiStage.actors.filterIsInstance<SkillView>().firstOrNull()?.isVisible = false
                uiStage.actors.filterIsInstance<BackgroundView>().firstOrNull()?.isVisible = false
                gameStage.fire(GameResumeEvent())
                return true
            }
            is AbilityViewClosedEvent -> {
                uiStage.actors.filterIsInstance<AbilityView>().firstOrNull()?.isVisible = false
                uiStage.actors.filterIsInstance<BackgroundView>().firstOrNull()?.isVisible = false
                gameStage.fire(GameResumeEvent())
                return true
            }
            else -> return false
        }
    }

    private fun switchActiveCharacter(newCharacterId: Int) {
        // Capture the current player's world position before removing them
        val currentPos = playerFamily.firstOrNull()
            ?.let { physicsMapper.getOrNull(it)?.body?.position?.cpy() }
            ?: com.badlogic.gdx.math.Vector2(0f, 0f)

        fadeView.color.a = 0f
        fadeView.isVisible = true
        fadeView.clearActions()
        fadeView.addAction(Actions.sequence(
            Actions.fadeIn(FADE_DURATION),
            Actions.run {
                // Remove all current overworld player entities
                playerFamily.forEach { entityWorld.remove(it) }

                // Update party state
                val partySystem = entityWorld.system<com.github.jacks.roleplayinggame.systems.PartySystem>()
                partySystem.activeOverworldCharacterId = newCharacterId
                saveManager.gatherAndSave(entityWorld)

                // Spawn the new character at the old position
                entityWorld.system<com.github.jacks.roleplayinggame.systems.EntityCreationSystem>()
                    .spawnPlayerCharacter(newCharacterId, currentPos)
            },
            Actions.delay(FADE_HOLD_DURATION),
            Actions.fadeOut(FADE_DURATION),
            Actions.run { fadeView.isVisible = false }
        ))
    }

    private fun enterBattleMode(enemy: Entity) {
        currentBattleEnemy = enemy
        // Freeze gameplay immediately so the player can't move during the fade
        entityWorld.systems
            .filter { it::class !in battleModeSystems }
            .forEach { it.enabled = false }
        fadeView.color.a = 0f
        fadeView.isVisible = true
        fadeView.clearActions()
        fadeView.addAction(Actions.sequence(
            Actions.fadeIn(FADE_DURATION),
            Actions.run {
                // Fire BattleEvent while screen is black — MapSystem loads the battle map,
                // which fires BattleMapChangeEvent, creating the battle enemy entity.
                gameStage.fire(BattleEvent(enemy = enemy))
                uiStage.actors.filterIsInstance<MainGameView>().first().isVisible = false
                uiStage.actors.filterIsInstance<BattleView>().first().isVisible = true
            },
            Actions.delay(FADE_HOLD_DURATION),
            Actions.fadeOut(FADE_DURATION),
            Actions.run { fadeView.isVisible = false }
        ))
    }

    private fun exitBattleMode(reason: BattleEndReason) {
        fadeView.color.a = 0f
        fadeView.isVisible = true
        fadeView.clearActions()
        fadeView.addAction(Actions.sequence(
            Actions.fadeIn(FADE_DURATION),
            Actions.run {
                // Fire BattleEndEvent while screen is black — MapSystem loads the overworld map
                gameStage.fire(BattleEndEvent(reason))
                currentBattleEnemy = null
                entityWorld.systems.forEach { it.enabled = true }
                disableOverworldSystems()
                uiStage.actors.filterIsInstance<BattleView>().first().isVisible = false
                uiStage.actors.filterIsInstance<MainGameView>().first().isVisible = true
            },
            Actions.delay(FADE_HOLD_DURATION),
            Actions.fadeOut(FADE_DURATION),
            Actions.run { fadeView.isVisible = false }
        ))
    }

    override fun render(delta: Float) {
        val deltaTime = delta.coerceAtMost(0.25f)
        GdxAI.getTimepiece().update(deltaTime)
        entityWorld.update(deltaTime)
    }

    override fun dispose() {
        textureAtlas.disposeSafely()
        entityWorld.dispose()
    }

    private fun disableOverworldSystems() {
        entityWorld.systems
            .filter { it::class in overworldDisabledSystems }
            .forEach { it.enabled = false }
    }

    companion object {
        private val log = logger<GameScreen>()
        private const val FADE_DURATION = 0.4f
        private const val FADE_HOLD_DURATION = 0.4f
        private val battleModeSystems = setOf(
            AnimationSystem::class,
            CameraSystem::class,
            RenderSystem::class,
            DebugSystem::class,
            BattleSystem::class,
        )
        private val overworldDisabledSystems = setOf(
            AiSystem::class,
        )

        // Systems that must stay active while a shop is open (all others are paused)
        private val shopMandatorySystems = setOf(
            AnimationSystem::class,
            CameraSystem::class,
            RenderSystem::class,
            DebugSystem::class,
            InventorySystem::class,
            ResourceSystem::class,
            StatSystem::class,
            SettingsSystem::class,
            ShopSystem::class,
        )
    }
}
