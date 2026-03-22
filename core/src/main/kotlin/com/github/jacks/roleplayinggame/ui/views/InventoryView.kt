package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.github.jacks.roleplayinggame.ui.Drawables
import com.github.jacks.roleplayinggame.ui.get
import com.github.jacks.roleplayinggame.ui.viewmodels.InventoryViewModel
import com.github.jacks.roleplayinggame.ui.widgets.InventoryLeftPanel
import com.github.jacks.roleplayinggame.ui.widgets.InventoryRightPanel
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
        defaults().minSize(0f)

        val innerTable = Table(skin).apply { defaults().minSize(0f) }
        innerTable.background = skin[Drawables.FRAME_BGD]
        leftPanel  = InventoryLeftPanel(model, skin)
        rightPanel = InventoryRightPanel(model, skin)
        innerTable.add(leftPanel).fill().width(160f).padRight(2f)
        innerTable.add(rightPanel).expand().fill()
        add(innerTable).expand().center().width(640f)
    }
}

@Scene2dDsl
fun <S> KWidget<S>.inventoryView(
    model: InventoryViewModel,
    skin: Skin = Scene2DSkin.defaultSkin,
    init: InventoryView.(S) -> Unit = {}
): InventoryView = actor(InventoryView(model, skin), init)
