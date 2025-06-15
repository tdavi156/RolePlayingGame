package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.badlogic.gdx.utils.Align
import com.github.jacks.roleplayinggame.ui.Drawables
import com.github.jacks.roleplayinggame.ui.Labels
import com.github.jacks.roleplayinggame.ui.get
import com.github.jacks.roleplayinggame.ui.viewmodels.InventoryViewModel
import com.github.jacks.roleplayinggame.ui.viewmodels.ItemModel
import com.github.jacks.roleplayinggame.ui.widgets.InventoryDragSource
import com.github.jacks.roleplayinggame.ui.widgets.InventoryDragTarget
import com.github.jacks.roleplayinggame.ui.widgets.InventorySlot
import com.github.jacks.roleplayinggame.ui.widgets.inventorySlot
import ktx.scene2d.*

class InventoryView(
    private val model : InventoryViewModel,
    skin : Skin
) : Table(skin), KTable {

    private val inventorySlots = mutableListOf<InventorySlot>()
    private val gearSlots = mutableListOf<InventorySlot>()
    lateinit var trashSlot : InventorySlot

    init {
        // UI Component
        val titlePadding = 15f
        setFillParent(true)

        table { inventoryTableCell ->
            background = skin[Drawables.FRAME_BGD]
            label(text = "Inventory", style = Labels.TITLE.skinKey, skin) {
                this.setAlignment(Align.center)
                it.expandX().fill()
                    .pad(8f, titlePadding, 0f, titlePadding)
                    .top()
                    .row()
            }

            table { inventorySlotTableCell ->
                for (cellNumber in 1..18) {
                    this@InventoryView.inventorySlots += inventorySlot(skin = skin) { slotCell ->
                        slotCell.padBottom(2f)
                        if (cellNumber % 6 == 0) {
                            slotCell.row()
                        } else {
                            slotCell.padRight(2f)
                        }
                    }
                }
                inventorySlotTableCell.expand().fill()
            }

            inventoryTableCell.expand().width(150f).height(120f).left().center()
        }

//        table { trashTableCell ->
//            background = skin[Drawables.FRAME_BGD]
//            label(text = "Trash", style = Labels.TITLE.skinKey, skin) {
//                this.setAlignment(Align.center)
//                it.expandX().fill()
//                    .pad(8f, titlePadding, 0f, titlePadding)
//                    .top()
//                    .row()
//            }
//            table { trashSlotTableCell ->
//                this@InventoryView.trashSlot = inventorySlot(skin = skin, isTrashSlot = true) { trashCell ->
//                    trashCell.pad(2f)
//                }
//                trashSlotTableCell.expand().fill()
//            }
//            trashTableCell.expand().width(74f).height(70f).left().center()
//        }

        table { gearTableCell ->
            background = skin[Drawables.FRAME_BGD]
            label(text = "Gear", style = Labels.TITLE.skinKey, skin) {
                this.setAlignment(Align.center)
                it.expandX().fill()
                    .pad(8f, titlePadding, 0f, titlePadding)
                    .top()
                    .row()
            }

            table { gearInnerTableCell ->
                this@InventoryView.gearSlots += inventorySlot(Drawables.INVENTORY_SLOT_HELMET, skin) {
                    it.padBottom(2f).colspan(2).row()
                }
                this@InventoryView.gearSlots += inventorySlot(Drawables.INVENTORY_SLOT_WEAPON, skin) {
                    it.padBottom(2f).padRight(2f)
                }
                this@InventoryView.gearSlots += inventorySlot(Drawables.INVENTORY_SLOT_ARMOR, skin) {
                    it.padBottom(2f).row()
                }
                this@InventoryView.gearSlots += inventorySlot(Drawables.INVENTORY_SLOT_BOOTS, skin) {
                    it.colspan(2).row()
                }
                gearInnerTableCell.expand().fill()
            }

            gearTableCell.expand().width(90f).height(120f).left().center()
        }

        setupDragAndDrop()

        // Data Binding
        model.onPropertyChange(InventoryViewModel::playerItems) { itemModels ->
            clearInventoryAndGear()
            itemModels.forEach {
                if (it.isEquipped) {
                    gear(it)
                } else {
                    item(it)
                }
            }
        }
    }

    private fun setupDragAndDrop() {
        val dnd = DragAndDrop()
        dnd.setDragActorPosition(
            InventoryDragSource.DRAG_ACTOR_SIZE * 0.5f,
            -InventoryDragSource.DRAG_ACTOR_SIZE * 0.5f
        )

        inventorySlots.forEach { slot ->
            dnd.addSource(InventoryDragSource(slot))
            dnd.addTarget(InventoryDragTarget(slot, ::onItemDropped))
        }
        gearSlots.forEach { slot ->
            dnd.addSource(InventoryDragSource(slot))
            dnd.addTarget(InventoryDragTarget(slot, ::onItemDropped, slot.supportedItemCategory))
        }
        //dnd.addTarget(InventoryDragTarget(trashSlot, ::onItemDropped))
    }

    private fun onItemDropped(
        sourceSlot : InventorySlot,
        targetSlot : InventorySlot,
        itemModel : ItemModel
    ) {
        sourceSlot.item(targetSlot.itemModel)
        targetSlot.item(itemModel)

        val sourceItem = sourceSlot.itemModel

        if (sourceSlot.isGear) {
            model.unequip(itemModel)
            if (sourceItem != null) {
                model.equip(sourceItem)
            }
        } else if (sourceItem != null) {
            model.inventoryItem(inventorySlots.indexOf(sourceSlot), sourceItem)
        }

        if (targetSlot.isGear) {
            if (sourceItem != null) {
                model.unequip(sourceItem)
            }
            model.equip(itemModel)
        } else {
            model.inventoryItem(inventorySlots.indexOf(targetSlot), itemModel)
        }
    }

    fun item(itemModel : ItemModel) {
        inventorySlots[itemModel.slotIndex].item(itemModel)
    }

    fun gear(itemModel : ItemModel) {
        gearSlots.firstOrNull { it.supportedItemCategory == itemModel.itemCategory }?.item(itemModel)
    }

    private fun clearInventoryAndGear() {
        inventorySlots.forEach { it.item(null) }
        gearSlots.forEach { it.item(null) }
    }
}

@Scene2dDsl
fun <S> KWidget<S>.inventoryView(
    model : InventoryViewModel,
    skin : Skin = Scene2DSkin.defaultSkin,
    init : InventoryView.(S) -> Unit = {}
) : InventoryView = actor(InventoryView(model, skin), init)
