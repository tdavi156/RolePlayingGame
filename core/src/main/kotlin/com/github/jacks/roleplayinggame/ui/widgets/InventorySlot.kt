package com.github.jacks.roleplayinggame.ui.widgets

import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Scaling
import com.github.jacks.roleplayinggame.components.ItemCategory
import com.github.jacks.roleplayinggame.ui.Drawables
import com.github.jacks.roleplayinggame.ui.get
import com.github.jacks.roleplayinggame.ui.viewmodels.ItemModel
import ktx.actors.alpha
import ktx.actors.plusAssign
import ktx.scene2d.*

class InventorySlot(
    private val slotItemBackground : Drawables?,
    private val skin : Skin,
) : WidgetGroup(), KGroup {

    private val background = Image(skin[Drawables.INVENTORY_SLOT])
    private val slotItemInfo : Image? = if (slotItemBackground == null) null else Image(skin[slotItemBackground])
    private val itemImage = Image()
    var itemModel : ItemModel? = null

    val itemDrawable : Drawable
        get() = itemImage.drawable

    val isGear : Boolean
        get() = supportedItemCategory != ItemCategory.UNDEFINED

    val isEmpty : Boolean
        get() = itemModel == null

    val itemCategory : ItemCategory
        get() = itemModel?.itemCategory ?: ItemCategory.UNDEFINED

    val supportedItemCategory : ItemCategory
        get() {
            return when(slotItemBackground) {
                Drawables.INVENTORY_SLOT_HELMET -> ItemCategory.HELMET
                Drawables.INVENTORY_SLOT_WEAPON -> ItemCategory.WEAPON
                Drawables.INVENTORY_SLOT_ARMOR -> ItemCategory.ARMOR
                Drawables.INVENTORY_SLOT_BOOTS -> ItemCategory.BOOTS
                else -> ItemCategory.UNDEFINED
            }
        }

    init {
        this += background
        slotItemInfo?.let { info ->
            this += info.apply {
                alpha = 0.33f
                setPosition(3f, 3f)
                setSize(14f, 14f)
                setScaling(Scaling.contain)
            }
        }
        this += itemImage.apply {
            setPosition(3f, 3f)
            setSize(14f, 14f)
            setScaling(Scaling.contain)
        }
    }

    fun item(model : ItemModel?) {
        itemModel = model
        if (model == null) {
            itemImage.drawable = null
        } else {
            itemImage.drawable = skin.getDrawable(model.atlasKey)
        }
    }

    override fun getPrefWidth(): Float = background.drawable.minWidth
    override fun getPrefHeight(): Float = background.drawable.minHeight
}

@Scene2dDsl
fun <S> KWidget<S>.inventorySlot(
    slotItemBackground : Drawables? = null,
    skin : Skin = Scene2DSkin.defaultSkin,
    init : InventorySlot.(S) -> Unit = { }
) : InventorySlot = actor(InventorySlot(slotItemBackground, skin), init)
