package com.github.jacks.roleplayinggame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import ktx.assets.disposeSafely
import ktx.scene2d.Scene2DSkin
import ktx.style.SkinDsl
import ktx.style.label
import ktx.style.set
import ktx.style.skin
import ktx.style.textButton

enum class Drawables(
    val atlasKey : String
) {
    CHAR_INFO_BGD("char_info"),
    PLAYER("player"),
    SLIME("slime"),
    LIFE_BAR("life_bar"),
    MANA_BAR("mana_bar"),
    FRAME_BGD("frame_bgd"),
    FRAME_FGD("frame_fgd"),
    INVENTORY_SLOT("inv_slot"),
    INVENTORY_SLOT_HELMET("inv_slot_helmet"),
    INVENTORY_SLOT_WEAPON("inv_slot_weapon"),
    INVENTORY_SLOT_ARMOR("inv_slot_armor"),
    INVENTORY_SLOT_BOOTS("inv_slot_boots");
}

enum class Labels {
    FRAME,
    TITLE,
    LARGE;

    val skinKey = this.name.lowercase()
}

enum class Fonts(
    val atlasRegionKey : String,
    val scaling : Float
) {
    DEFAULT("fnt_white", 0.25f),
    BIG("fnt_white", 0.5f);

    val skinKey = "Font_${this.name.lowercase()}"
    val fontPath = "assets/ui/${this.atlasRegionKey}.fnt"
}

enum class Buttons {
    TEXT_BUTTON,
    IMAGE_BUTTON;

    val skinKey = this.name.lowercase()
}

operator fun Skin.get(drawable : Drawables) : Drawable = this.getDrawable(drawable.atlasKey)
operator fun Skin.get(font : Fonts) : BitmapFont = this.getFont(font.skinKey)

fun loadSkin() {
    Scene2DSkin.defaultSkin = skin(TextureAtlas("assets/ui/ui.atlas")) { skin ->
        loadFonts(skin)
        loadLabels(skin)
        loadButtons(skin)
    }
}

private fun @SkinDsl Skin.loadFonts(skin: Skin) {
    Fonts.entries.forEach { font ->
        skin[font.skinKey] = BitmapFont(Gdx.files.internal(font.fontPath), skin.getRegion(font.atlasRegionKey)).apply {
            data.setScale(font.scaling)
            data.markupEnabled = true
        }
    }
}

private fun @SkinDsl Skin.loadLabels(skin : Skin) {
    label(Labels.FRAME.skinKey) {
        font = skin[Fonts.DEFAULT]
        background = skin[Drawables.FRAME_FGD].apply {
            leftWidth = 3f
            rightWidth = 3f
            topHeight = 3f
            bottomHeight = 3f
        }
    }
    label(Labels.TITLE.skinKey) {
        font = skin[Fonts.BIG]
        fontColor = Color.SLATE
        background = skin[Drawables.FRAME_FGD].apply {
            leftWidth = 3f
            rightWidth = 3f
            topHeight = 3f
            bottomHeight = 3f
        }
    }
    label(Labels.LARGE.skinKey) {
        font = skin[Fonts.BIG]
        fontColor = Color.WHITE
    }
}

private fun @SkinDsl Skin.loadButtons(skin: Skin) {
    textButton(Buttons.TEXT_BUTTON.skinKey) {
        font = skin[Fonts.DEFAULT]
    }
}

fun disposeSkin() {
    Scene2DSkin.defaultSkin.disposeSafely()
}
