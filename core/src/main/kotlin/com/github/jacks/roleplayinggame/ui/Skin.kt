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

enum class Colors(
    val color : Color
) {
    RED(Color(0.835f, 0.306f, 0.306f, 1f)),
    ORANGE(Color(0.878f, 0.6f, 0.322f, 1f)),
    YELLOW(Color(0.878f, 0.878f, 0.322f, 1f)),
    GREEN(Color(0.6f, 0.878f, 0.322f, 1f)),
    BLUE(Color(0.322f, 0.6f, 0.878f, 1f)),
    PURPLE(Color(0.6f, 0.322f, 0.878f, 1f)),
    PINK(Color(0.878f, 0.322f, 0.878f, 1f)),
    BROWN(Color(0.482f, 0.306f, 0.22f, 1f)),
    WHITE(Color(0.902f, 0.902f, 0.902f, 1f)),
    BLACK(Color(0.149f, 0.149f, 0.149f, 1f));
}

enum class Drawables(
    val atlasKey : String
) {
    CHAR_INFO_BGD("char_info"),
    PLAYER("player"),
    SLIME("slime"),
    LIFE_BAR("life_bar"),
    MANA_BAR("mana_bar"),
    FRAME_BGD("brownbgd"),
    FRAME_FGD("frame_fgd"),
    INVENTORY_SLOT("inv_slot"),
    INVENTORY_SLOT_HELMET("inv_slot_helmet"),
    INVENTORY_SLOT_WEAPON("inv_slot_weapon"),
    INVENTORY_SLOT_ARMOR("inv_slot_armor"),
    INVENTORY_SLOT_BOOTS("inv_slot_boots"),

    BAR_GREEN_THIN("bar_green_thin"),
    BAR_GREEN_THICK("bar_green_thick"),
    BAR_GREEN_THICK_A25("bar_green_thick_a25"),
    BAR_GREY_THICK("bar_grey_thick"),
    BAR_BLACK_THIN("bar_black_thin"),

    BACKGROUND_GREY("button_grey_up"),

    BUTTON_RED_UP("button_red_up"),
    BUTTON_RED_OVER("button_red_over"),
    BUTTON_RED_DISABLED("button_red_disabled"),

    BUTTON_ORANGE_UP("button_orange_up"),
    BUTTON_ORANGE_OVER("button_orange_over"),
    BUTTON_ORANGE_DISABLED("button_orange_disabled"),

    BUTTON_YELLOW_UP("button_yellow_up"),
    BUTTON_YELLOW_OVER("button_yellow_over"),
    BUTTON_YELLOW_DISABLED("button_yellow_disabled"),

    BUTTON_GREEN_UP("button_green_up"),
    BUTTON_GREEN_OVER("button_green_over"),
    BUTTON_GREEN_DISABLED("button_green_disabled"),

    BUTTON_LIGHT_BLUE_UP("button_light_blue_up"),
    BUTTON_LIGHT_BLUE_OVER("button_light_blue_over"),
    BUTTON_LIGHT_BLUE_DISABLED("button_light_blue_disabled"),

    BUTTON_BLUE_UP("button_blue_up"),
    BUTTON_BLUE_OVER("button_blue_over"),
    BUTTON_BLUE_DISABLED("button_blue_disabled"),

    BUTTON_PURPLE_UP("button_purple_up"),
    BUTTON_PURPLE_OVER("button_purple_over"),
    BUTTON_PURPLE_DISABLED("button_purple_disabled"),

    BUTTON_PINK_UP("button_pink_up"),
    BUTTON_PINK_OVER("button_pink_over"),
    BUTTON_PINK_DISABLED("button_pink_disabled"),

    BUTTON_BROWN_UP("button_brown_up"),
    BUTTON_BROWN_OVER("button_brown_over"),
    BUTTON_BROWN_DISABLED("button_brown_disabled"),

    BUTTON_WHITE_UP("button_white_up"),
    BUTTON_WHITE_OVER("button_white_over"),
    BUTTON_WHITE_DISABLED("button_white_disabled"),

    BUTTON_BLACK_UP("button_black_up"),
    BUTTON_BLACK_OVER("button_black_over"),
    BUTTON_BLACK_DISABLED("button_black_disabled"),

    BUTTON_GREY_UP("button_grey_up");
}

enum class Labels {
    FRAME,
    TINY,
    SMALL,
    MEDIUM,
    DEFAULT,
    LARGE,
    X_LARGE,
    XX_LARGE,
    TITLE,

    RED,
    ORANGE,
    YELLOW,
    GREEN,
    BLUE,
    PURPLE,
    PINK,
    BROWN,
    WHITE,
    BLACK,

    SMALL_RED_BGD,
    SMALL_ORANGE_BGD,
    SMALL_YELLOW_BGD,
    SMALL_GREEN_BGD,
    SMALL_BLUE_BGD,
    SMALL_PURPLE_BGD,
    SMALL_PINK_BGD,
    SMALL_BROWN_BGD,
    SMALL_WHITE_BGD,
    SMALL_BLACK_BGD,
    SMALL_GREY_BGD,

    ACH_COMPLETED_BGD,
    SOIL_TOOLTIP_BGD;

    val skinKey = this.name.lowercase()
}

enum class Fonts(
    val atlasRegionKey : String,
    val scaling : Float
) {
    DEFAULT("fnt_white", 0.25f),
    BIG("fnt_white", 0.5f),

    TINY("regular_12pt", 1f),
    SMALL("regular_16pt", 1f),
    MEDIUM("regular_20pt", 1f),
    //DEFAULT("default_24pt", 1f),
    LARGE("default_28pt", 1f),
    X_LARGE("default_32pt", 1f),
    XX_LARGE("default_40pt", 1f),
    TITLE("default_64pt", 1f),

    TINY_BUTTON("regular_12pt", 1f),
    SMALL_BUTTON("regular_16pt", 1f),
    MEDIUM_BUTTON("regular_20pt", 1f);

    val skinKey = "Font_${this.name.lowercase()}"
    val fontPath = "assets/fonts/${this.atlasRegionKey}.fnt"
}

enum class Buttons {
    TEXT_BUTTON,
    IMAGE_BUTTON,

    RED_BUTTON_SMALL,
    RED_BUTTON_MEDIUM,
    ORANGE_BUTTON_SMALL,
    ORANGE_BUTTON_MEDIUM,
    YELLOW_BUTTON_SMALL,
    YELLOW_BUTTON_MEDIUM,
    GREEN_BUTTON_SMALL,
    GREEN_BUTTON_MEDIUM,
    LIGHT_BLUE_BUTTON_SMALL,
    LIGHT_BLUE_BUTTON_MEDIUM,
    BLUE_BUTTON_SMALL,
    BLUE_BUTTON_MEDIUM,
    PURPLE_BUTTON_SMALL,
    PURPLE_BUTTON_MEDIUM,
    PINK_BUTTON_SMALL,
    PINK_BUTTON_MEDIUM,
    BROWN_BUTTON_SMALL,
    BROWN_BUTTON_MEDIUM,
    WHITE_BUTTON_SMALL,
    WHITE_BUTTON_MEDIUM,
    BLACK_BUTTON_SMALL,
    BLACK_BUTTON_MEDIUM,
    GREY_BUTTON_SMALL,
    GREY_BUTTON_MEDIUM;

    val skinKey = this.name.lowercase()
}

operator fun Skin.get(drawable : Drawables) : Drawable = this.getDrawable(drawable.atlasKey)
operator fun Skin.get(font : Fonts) : BitmapFont = this.getFont(font.skinKey)

fun loadSkin() {
    // figure out how to remove the assets part, it should be inherit
    Scene2DSkin.defaultSkin = skin(TextureAtlas("assets/ui/ui_objects.atlas")) { skin ->
        loadFonts(skin)
        loadLabels(skin)
        loadButtons(skin)
    }
}

private fun loadFonts(skin: Skin) {
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

    label(Labels.TINY.skinKey) {
        font = skin[Fonts.TINY]
        fontColor = Color.WHITE
    }
    label(Labels.SMALL.skinKey) {
        font = skin[Fonts.SMALL]
        fontColor = Color.WHITE
    }
    label(Labels.MEDIUM.skinKey) {
        font = skin[Fonts.MEDIUM]
        fontColor = Color.WHITE
    }
    label(Labels.DEFAULT.skinKey) {
        font = skin[Fonts.DEFAULT]
        fontColor = Color.WHITE
    }
    label(Labels.LARGE.skinKey) {
        font = skin[Fonts.LARGE]
        fontColor = Color.WHITE
    }
    label(Labels.X_LARGE.skinKey) {
        font = skin[Fonts.X_LARGE]
        fontColor = Color.WHITE
    }
    label(Labels.XX_LARGE.skinKey) {
        font = skin[Fonts.XX_LARGE]
        fontColor = Color.WHITE
    }
    label(Labels.TITLE.skinKey) {
        font = skin[Fonts.TITLE]
        fontColor = Color.WHITE
    }

    // value labels
    label(Labels.RED.skinKey) {
        font = skin[Fonts.DEFAULT]
        fontColor = Colors.RED.color
    }
    label(Labels.ORANGE.skinKey) {
        font = skin[Fonts.DEFAULT]
        fontColor = Colors.ORANGE.color
    }
    label(Labels.YELLOW.skinKey) {
        font = skin[Fonts.DEFAULT]
        fontColor = Colors.YELLOW.color
    }
    label(Labels.GREEN.skinKey) {
        font = skin[Fonts.DEFAULT]
        fontColor = Colors.GREEN.color
    }
    label(Labels.BLUE.skinKey) {
        font = skin[Fonts.DEFAULT]
        fontColor = Colors.BLUE.color
    }
    label(Labels.PURPLE.skinKey) {
        font = skin[Fonts.DEFAULT]
        fontColor = Colors.PURPLE.color
    }
    label(Labels.PINK.skinKey) {
        font = skin[Fonts.DEFAULT]
        fontColor = Colors.PINK.color
    }
    label(Labels.BROWN.skinKey) {
        font = skin[Fonts.DEFAULT]
        fontColor = Colors.BROWN.color
    }
    label(Labels.WHITE.skinKey) {
        font = skin[Fonts.DEFAULT]
        fontColor = Colors.WHITE.color
    }
    label(Labels.BLACK.skinKey) {
        font = skin[Fonts.DEFAULT]
        fontColor = Colors.BLACK.color
    }

    // tooltip labels
    label(Labels.SMALL_RED_BGD.skinKey) {
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color.BLACK
        background = skin.get(Drawables.BUTTON_RED_UP)
    }
    label(Labels.SMALL_ORANGE_BGD.skinKey) {
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color.BLACK
        background = skin.get(Drawables.BUTTON_ORANGE_UP)
    }
    label(Labels.SMALL_YELLOW_BGD.skinKey) {
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color.BLACK
        background = skin.get(Drawables.BUTTON_YELLOW_UP)
    }
    label(Labels.SMALL_GREEN_BGD.skinKey) {
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color.BLACK
        background = skin.get(Drawables.BUTTON_GREEN_UP)
    }
    label(Labels.SMALL_BLUE_BGD.skinKey) {
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color.BLACK
        background = skin.get(Drawables.BUTTON_BLUE_UP)
    }
    label(Labels.SMALL_PURPLE_BGD.skinKey) {
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color.BLACK
        background = skin.get(Drawables.BUTTON_PURPLE_UP)
    }
    label(Labels.SMALL_PINK_BGD.skinKey) {
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color.BLACK
        background = skin.get(Drawables.BUTTON_PINK_UP)
    }
    label(Labels.SMALL_BROWN_BGD.skinKey) {
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color.BLACK
        background = skin.get(Drawables.BUTTON_BROWN_UP)
    }
    label(Labels.SMALL_WHITE_BGD.skinKey) {
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color.BLACK
        background = skin.get(Drawables.BUTTON_WHITE_UP)
    }
    label(Labels.SMALL_BLACK_BGD.skinKey) {
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color.WHITE
        background = skin.get(Drawables.BUTTON_BLACK_UP)
    }
    label(Labels.SMALL_GREY_BGD.skinKey) {
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color.WHITE
        background = skin.get(Drawables.BUTTON_GREY_UP)
    }

    // misc labels
    label(Labels.ACH_COMPLETED_BGD.skinKey) {
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color.WHITE
        background = skin.get(Drawables.BUTTON_GREEN_DISABLED)
    }
    label(Labels.SOIL_TOOLTIP_BGD.skinKey) {
        font = skin[Fonts.TINY_BUTTON]
        fontColor = Color.WHITE
        background = skin.get(Drawables.BUTTON_BLACK_UP)
    }
}

private fun @SkinDsl Skin.loadButtons(skin: Skin) {
    textButton(Buttons.TEXT_BUTTON.skinKey) {
        font = skin[Fonts.DEFAULT]
    }

    textButton(Buttons.RED_BUTTON_SMALL.skinKey) {
        up = skin[Drawables.BUTTON_RED_UP]
        down = skin[Drawables.BUTTON_RED_DISABLED]
        over = skin[Drawables.BUTTON_RED_OVER]
        disabled = skin[Drawables.BUTTON_RED_DISABLED]
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color(0f, 0f, 0f, 1f)
    }
    textButton(Buttons.RED_BUTTON_MEDIUM.skinKey) {
        up = skin[Drawables.BUTTON_RED_UP]
        down = skin[Drawables.BUTTON_RED_DISABLED]
        over = skin[Drawables.BUTTON_RED_OVER]
        disabled = skin[Drawables.BUTTON_RED_DISABLED]
        font = skin[Fonts.MEDIUM]
        fontColor = Color(0f, 0f, 0f, 1f)
    }
    textButton(Buttons.ORANGE_BUTTON_SMALL.skinKey) {
        up = skin[Drawables.BUTTON_ORANGE_UP]
        down = skin[Drawables.BUTTON_ORANGE_DISABLED]
        over = skin[Drawables.BUTTON_ORANGE_OVER]
        disabled = skin[Drawables.BUTTON_ORANGE_DISABLED]
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color(0f, 0f, 0f, 1f)
    }
    textButton(Buttons.ORANGE_BUTTON_MEDIUM.skinKey) {
        up = skin[Drawables.BUTTON_ORANGE_UP]
        down = skin[Drawables.BUTTON_ORANGE_DISABLED]
        over = skin[Drawables.BUTTON_ORANGE_OVER]
        disabled = skin[Drawables.BUTTON_ORANGE_DISABLED]
        font = skin[Fonts.MEDIUM]
        fontColor = Color(0f, 0f, 0f, 1f)
    }
    textButton(Buttons.YELLOW_BUTTON_SMALL.skinKey) {
        up = skin[Drawables.BUTTON_YELLOW_UP]
        down = skin[Drawables.BUTTON_YELLOW_DISABLED]
        over = skin[Drawables.BUTTON_YELLOW_OVER]
        disabled = skin[Drawables.BUTTON_YELLOW_DISABLED]
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color(0f, 0f, 0f, 1f)
    }
    textButton(Buttons.YELLOW_BUTTON_MEDIUM.skinKey) {
        up = skin[Drawables.BUTTON_YELLOW_UP]
        down = skin[Drawables.BUTTON_YELLOW_DISABLED]
        over = skin[Drawables.BUTTON_YELLOW_OVER]
        disabled = skin[Drawables.BUTTON_YELLOW_DISABLED]
        font = skin[Fonts.MEDIUM]
        fontColor = Color(0f, 0f, 0f, 1f)
    }
    textButton(Buttons.GREEN_BUTTON_SMALL.skinKey) {
        up = skin[Drawables.BUTTON_GREEN_UP]
        down = skin[Drawables.BUTTON_GREEN_DISABLED]
        over = skin[Drawables.BUTTON_GREEN_OVER]
        disabled = skin[Drawables.BUTTON_GREEN_DISABLED]
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color(0f, 0f, 0f, 1f)
    }
    textButton(Buttons.GREEN_BUTTON_MEDIUM.skinKey) {
        up = skin[Drawables.BUTTON_GREEN_UP]
        down = skin[Drawables.BUTTON_GREEN_DISABLED]
        over = skin[Drawables.BUTTON_GREEN_OVER]
        disabled = skin[Drawables.BUTTON_GREEN_DISABLED]
        font = skin[Fonts.MEDIUM]
        fontColor = Color(0f, 0f, 0f, 1f)
    }
    textButton(Buttons.LIGHT_BLUE_BUTTON_SMALL.skinKey) {
        up = skin[Drawables.BUTTON_LIGHT_BLUE_UP]
        down = skin[Drawables.BUTTON_LIGHT_BLUE_DISABLED]
        over = skin[Drawables.BUTTON_LIGHT_BLUE_OVER]
        disabled = skin[Drawables.BUTTON_LIGHT_BLUE_DISABLED]
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color(0f, 0f, 0f, 1f)
    }
    textButton(Buttons.LIGHT_BLUE_BUTTON_MEDIUM.skinKey) {
        up = skin[Drawables.BUTTON_LIGHT_BLUE_UP]
        down = skin[Drawables.BUTTON_LIGHT_BLUE_DISABLED]
        over = skin[Drawables.BUTTON_LIGHT_BLUE_OVER]
        disabled = skin[Drawables.BUTTON_LIGHT_BLUE_DISABLED]
        font = skin[Fonts.MEDIUM]
        fontColor = Color(0f, 0f, 0f, 1f)
    }
    textButton(Buttons.BLUE_BUTTON_SMALL.skinKey) {
        up = skin[Drawables.BUTTON_BLUE_UP]
        down = skin[Drawables.BUTTON_BLUE_DISABLED]
        over = skin[Drawables.BUTTON_BLUE_OVER]
        disabled = skin[Drawables.BUTTON_BLUE_DISABLED]
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color(0f, 0f, 0f, 1f)
    }
    textButton(Buttons.BLUE_BUTTON_MEDIUM.skinKey) {
        up = skin[Drawables.BUTTON_BLUE_UP]
        down = skin[Drawables.BUTTON_BLUE_DISABLED]
        over = skin[Drawables.BUTTON_BLUE_OVER]
        disabled = skin[Drawables.BUTTON_BLUE_DISABLED]
        font = skin[Fonts.MEDIUM]
        fontColor = Color(0f, 0f, 0f, 1f)
    }
    textButton(Buttons.PURPLE_BUTTON_SMALL.skinKey) {
        up = skin[Drawables.BUTTON_PURPLE_UP]
        down = skin[Drawables.BUTTON_PURPLE_DISABLED]
        over = skin[Drawables.BUTTON_PURPLE_OVER]
        disabled = skin[Drawables.BUTTON_PURPLE_DISABLED]
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color(0f, 0f, 0f, 1f)
    }
    textButton(Buttons.PURPLE_BUTTON_MEDIUM.skinKey) {
        up = skin[Drawables.BUTTON_PURPLE_UP]
        down = skin[Drawables.BUTTON_PURPLE_DISABLED]
        over = skin[Drawables.BUTTON_PURPLE_OVER]
        disabled = skin[Drawables.BUTTON_PURPLE_DISABLED]
        font = skin[Fonts.MEDIUM]
        fontColor = Color(0f, 0f, 0f, 1f)
    }
    textButton(Buttons.PINK_BUTTON_SMALL.skinKey) {
        up = skin[Drawables.BUTTON_PINK_UP]
        down = skin[Drawables.BUTTON_PINK_DISABLED]
        over = skin[Drawables.BUTTON_PINK_OVER]
        disabled = skin[Drawables.BUTTON_PINK_DISABLED]
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color(0f, 0f, 0f, 1f)
    }
    textButton(Buttons.PINK_BUTTON_MEDIUM.skinKey) {
        up = skin[Drawables.BUTTON_PINK_UP]
        down = skin[Drawables.BUTTON_PINK_DISABLED]
        over = skin[Drawables.BUTTON_PINK_OVER]
        disabled = skin[Drawables.BUTTON_PINK_DISABLED]
        font = skin[Fonts.MEDIUM]
        fontColor = Color(0f, 0f, 0f, 1f)
    }
    textButton(Buttons.BROWN_BUTTON_SMALL.skinKey) {
        up = skin[Drawables.BUTTON_BROWN_UP]
        down = skin[Drawables.BUTTON_BROWN_DISABLED]
        over = skin[Drawables.BUTTON_BROWN_OVER]
        disabled = skin[Drawables.BUTTON_BROWN_DISABLED]
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color(0f, 0f, 0f, 1f)
    }
    textButton(Buttons.BROWN_BUTTON_MEDIUM.skinKey) {
        up = skin[Drawables.BUTTON_BROWN_UP]
        down = skin[Drawables.BUTTON_BROWN_DISABLED]
        over = skin[Drawables.BUTTON_BROWN_OVER]
        disabled = skin[Drawables.BUTTON_BROWN_DISABLED]
        font = skin[Fonts.MEDIUM]
        fontColor = Color(0f, 0f, 0f, 1f)
    }
    textButton(Buttons.WHITE_BUTTON_SMALL.skinKey) {
        up = skin[Drawables.BUTTON_WHITE_UP]
        down = skin[Drawables.BUTTON_WHITE_DISABLED]
        over = skin[Drawables.BUTTON_WHITE_OVER]
        disabled = skin[Drawables.BUTTON_WHITE_DISABLED]
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color(0f, 0f, 0f, 1f)
    }
    textButton(Buttons.WHITE_BUTTON_MEDIUM.skinKey) {
        up = skin[Drawables.BUTTON_WHITE_UP]
        down = skin[Drawables.BUTTON_WHITE_DISABLED]
        over = skin[Drawables.BUTTON_WHITE_OVER]
        disabled = skin[Drawables.BUTTON_WHITE_DISABLED]
        font = skin[Fonts.MEDIUM]
        fontColor = Color(0f, 0f, 0f, 1f)
    }
    textButton(Buttons.BLACK_BUTTON_SMALL.skinKey) {
        up = skin[Drawables.BUTTON_BLACK_UP]
        down = skin[Drawables.BUTTON_BLACK_DISABLED]
        over = skin[Drawables.BUTTON_BLACK_OVER]
        disabled = skin[Drawables.BUTTON_BLACK_DISABLED]
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color(1f, 1f, 1f, 1f)
    }
    textButton(Buttons.BLACK_BUTTON_MEDIUM.skinKey) {
        up = skin[Drawables.BUTTON_BLACK_UP]
        down = skin[Drawables.BUTTON_BLACK_DISABLED]
        over = skin[Drawables.BUTTON_BLACK_OVER]
        disabled = skin[Drawables.BUTTON_BLACK_DISABLED]
        font = skin[Fonts.MEDIUM]
        fontColor = Color(1f, 1f, 1f, 1f)
    }
    textButton(Buttons.GREY_BUTTON_SMALL.skinKey) {
        up = skin[Drawables.BUTTON_BLACK_OVER]
        down = skin[Drawables.BUTTON_BLACK_UP]
        over = skin[Drawables.BUTTON_GREY_UP]
        disabled = skin[Drawables.BUTTON_BLACK_UP]
        font = skin[Fonts.SMALL_BUTTON]
        fontColor = Color(1f, 1f, 1f, 1f)
    }
    textButton(Buttons.GREY_BUTTON_MEDIUM.skinKey) {
        up = skin[Drawables.BUTTON_BLACK_OVER]
        down = skin[Drawables.BUTTON_BLACK_UP]
        over = skin[Drawables.BUTTON_GREY_UP]
        disabled = skin[Drawables.BUTTON_BLACK_UP]
        font = skin[Fonts.MEDIUM]
        fontColor = Color(1f, 1f, 1f, 1f)
    }
}

fun disposeSkin() {
    Scene2DSkin.defaultSkin.disposeSafely()
}
