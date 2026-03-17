package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.github.jacks.roleplayinggame.ui.Drawables
import com.github.jacks.roleplayinggame.ui.get
import com.github.jacks.roleplayinggame.ui.viewmodels.InventoryViewModel
import com.github.jacks.roleplayinggame.ui.widgets.InventoryLeftPanel
import com.github.jacks.roleplayinggame.ui.widgets.InventoryRightPanel
import com.github.jacks.roleplayinggame.ui.widgets.inventoryLeftPanel
import com.github.jacks.roleplayinggame.ui.widgets.inventoryRightPanel
import ktx.scene2d.KTable
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor

class InventoryView(
    val model: InventoryViewModel,
    skin: Skin,
) : Table(skin), KTable {

    val leftPanel: InventoryLeftPanel
    val rightPanel: InventoryRightPanel

    init {
        setFillParent(true)
        background = skin[Drawables.FRAME_BGD]

        leftPanel  = inventoryLeftPanel(model)  { it.expand().fill().padRight(2f) }
        rightPanel = inventoryRightPanel(model) { it.expand().fill() }
    }
}

@Scene2dDsl
fun <S> KWidget<S>.inventoryView(
    model: InventoryViewModel,
    skin: Skin = Scene2DSkin.defaultSkin,
    init: InventoryView.(S) -> Unit = {}
): InventoryView = actor(InventoryView(model, skin), init)
