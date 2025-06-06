package com.github.jacks.roleplayinggame.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.github.jacks.roleplayinggame.components.InventoryComponent
import com.github.jacks.roleplayinggame.components.ItemCategory
import com.github.jacks.roleplayinggame.components.PlayerComponent
import com.github.jacks.roleplayinggame.input.gdxInputProcessor
import com.github.jacks.roleplayinggame.ui.viewmodels.InventoryViewModel
import com.github.jacks.roleplayinggame.ui.viewmodels.ItemModel
import com.github.jacks.roleplayinggame.ui.views.InventoryView
import com.github.jacks.roleplayinggame.ui.views.inventoryView
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.world
import ktx.app.KtxScreen
import ktx.scene2d.actors

class InventoryScreen : KtxScreen {
    private val stage : Stage = Stage(ExtendViewport(320f, 180f))
    private val entityWorld = world {  }
    private val playerEntity : Entity
    private val model = InventoryViewModel(entityWorld, stage)
    private lateinit var inventoryView : InventoryView

    init {
        playerEntity = entityWorld.entity {
            add<PlayerComponent>()
            add<InventoryComponent>()
        }
    }

    override fun show() {
        stage.clear()
        stage.addListener(model)
        stage.actors {
            inventoryView = inventoryView(model)
        }
        gdxInputProcessor(stage)
        stage.isDebugAll = false
    }

    override fun render(delta: Float) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            hide()
            show()
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            inventoryView.item(ItemModel(-1, ItemCategory.BOOTS, "boots", 1, false))
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            inventoryView.item(ItemModel(-1, ItemCategory.WEAPON, "sword", 3, false))
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) {
        }

        stage.act()
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
    }
}

