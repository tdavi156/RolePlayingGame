package com.github.jacks.roleplayinggame.ui.widgets

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.*
import com.github.jacks.roleplayinggame.components.ItemCategory
import com.github.jacks.roleplayinggame.ui.viewmodels.ItemModel

class InventoryDragSource(
    val inventorySlot : InventorySlot
) : Source(inventorySlot) {

    val isGear : Boolean
        get() = inventorySlot.isGear

    val supportedItemCategory : ItemCategory
        get() = inventorySlot.supportedItemCategory

    override fun dragStart(event: InputEvent, x: Float, y: Float, pointer: Int): Payload? {
        if (inventorySlot.itemModel == null) {
            return null
        }

        return Payload().apply {
            `object` = inventorySlot.itemModel
            dragActor = Image(inventorySlot.itemDrawable).apply {
                setSize(DRAG_ACTOR_SIZE, DRAG_ACTOR_SIZE)
            }
            inventorySlot.item(null)
        }
    }

    override fun dragStop(event: InputEvent, x: Float, y: Float, pointer: Int, payload: Payload, target: DragAndDrop.Target?) {
        if (target == null /* add logic for if the target is the same as the source */) {
            inventorySlot.item(payload.`object` as ItemModel)
        }
    }

    companion object {
        const val DRAG_ACTOR_SIZE = 22f
    }
}

class InventoryDragTarget(
    private val inventorySlot: InventorySlot,
    private val onDrop : (sourceSlot : InventorySlot, targetSlot : InventorySlot, itemModel : ItemModel) -> Unit,
    private val supportedItemCategory : ItemCategory? = null
) : DragAndDrop.Target(inventorySlot) {

    private val isGear : Boolean
        get() = supportedItemCategory != null

    private fun isSupported(category: ItemCategory) : Boolean = supportedItemCategory == category

    override fun drag(source: Source, payload: Payload, x: Float, y: Float, pointer: Int): Boolean {
        val itemModel = payload.`object` as ItemModel
        val dragSource = source as InventoryDragSource
        val sourceCategory = dragSource.supportedItemCategory

        return if (isGear && isSupported(itemModel.itemCategory)) {
            true
        } else if (!isGear && dragSource.isGear && (inventorySlot.isEmpty || inventorySlot.itemCategory == sourceCategory)) {
            true
        } else if (!isGear && !dragSource.isGear) {
            true
        } else {
            payload.dragActor.color = Color.FIREBRICK
            return false
        }
    }

    override fun reset(source: Source, payload: Payload) {
        payload.dragActor.color = Color.WHITE
    }

    override fun drop(source: Source, payload: Payload, x: Float, y: Float, pointer: Int) {
        onDrop(
            (source as InventoryDragSource).inventorySlot,
            actor as InventorySlot,
            payload.`object` as ItemModel
        )
    }
}
