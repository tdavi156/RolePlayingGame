package com.github.jacks.roleplayinggame.ui.widgets

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.utils.Scaling
import com.github.jacks.roleplayinggame.ui.Drawables
import com.github.jacks.roleplayinggame.ui.get
import ktx.actors.plusAssign
import ktx.scene2d.KGroup
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor

class CharacterInfo(
    charDrawable : Drawables?,
    private val skin : Skin
) : WidgetGroup(), KGroup {

    private val background : Image = Image(skin[Drawables.CHAR_INFO_BGD])
    private val characterBackground : Image = Image(if(charDrawable == null) null else skin[charDrawable])
    private val lifeBar : Image = Image(skin[Drawables.LIFE_BAR])
    private val manaBar : Image = Image(skin[Drawables.MANA_BAR])

    init {
        this += background
        this += characterBackground.apply {
            setPosition(2f, 2f)
            setSize(22f, 20f)
            setScaling(Scaling.contain)
        }
        this += lifeBar.apply { setPosition(26f, 19f) }
        this += manaBar.apply { setPosition(26f, 13f) }
    }

    override fun getPrefWidth() = background.drawable.minWidth
    override fun getPrefHeight() = background.drawable.minHeight

    fun character(charDrawable : Drawables?) {
        if (charDrawable == null) {
            characterBackground.drawable = null
        } else {
            characterBackground.drawable = skin[charDrawable]
        }
    }

    fun life(percentage : Float, duration : Float = 0.75f) {
        lifeBar.clearActions()
        lifeBar += Actions.scaleTo(MathUtils.clamp(percentage, 0f, 1f), 1f, duration)
    }
}

@Scene2dDsl
fun <S> KWidget<S>.characterInfo(
    charDrawable : Drawables?,
    skin : Skin = Scene2DSkin.defaultSkin,
    init : CharacterInfo.(S) -> Unit = {}
) : CharacterInfo = actor(CharacterInfo(charDrawable, skin), init)
