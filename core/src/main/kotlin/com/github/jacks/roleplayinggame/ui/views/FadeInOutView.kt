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

class FadeInOutView(skin : Skin) : KTable, Table(skin) {

    init {
        setFillParent(true)
        if (!skin.has(FADE_IN_OUT_PIXMAP_KEY, TextureRegionDrawable::class.java)) {
            skin.add(FADE_IN_OUT_PIXMAP_KEY, TextureRegionDrawable(
                Texture(
                    Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
                        this.drawPixel(0, 0, Color.rgba8888(0.3f, 0.2f, 0.7f, 0.5f))
                    }
                )
            ))
        }
        background = skin.get(FADE_IN_OUT_PIXMAP_KEY, TextureRegionDrawable::class.java)
    }

    companion object {
        private const val FADE_IN_OUT_PIXMAP_KEY = "fadeInOutTexturePixmap"
    }
}

@Scene2dDsl
fun <S> KWidget<S>.fadeInOutView(
    skin : Skin = Scene2DSkin.defaultSkin,
    init : FadeInOutView.(S) -> Unit = { }
) : FadeInOutView = actor(FadeInOutView(skin), init)
