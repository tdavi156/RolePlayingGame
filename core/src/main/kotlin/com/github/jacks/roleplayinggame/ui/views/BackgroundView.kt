package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import ktx.scene2d.KTable
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor

class BackgroundView(skin : Skin) : KTable, Table(skin) {

    init {
        setFillParent(true)

        if (!skin.has(BACKGROUND_PIXMAP_KEY, TextureRegionDrawable::class.java)) {
            skin.add(BACKGROUND_PIXMAP_KEY, TextureRegionDrawable(
                Texture(
                    Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
                        this.drawPixel(0, 0, Color.rgba8888(0.6f, 0.4f, 0.1f, 1f))
                    }
                )
            ))
        }

        background = skin.get(BACKGROUND_PIXMAP_KEY, TextureRegionDrawable::class.java)
    }

    companion object {
        private const val BACKGROUND_PIXMAP_KEY = "backgroundTexturePixmap"
    }
}

@Scene2dDsl
fun <S> KWidget<S>.backgroundView(
    skin : Skin = Scene2DSkin.defaultSkin,
    init : BackgroundView.(S) -> Unit = { }
) : BackgroundView = actor(BackgroundView(skin), init)
