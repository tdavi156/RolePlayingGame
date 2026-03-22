package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.github.jacks.roleplayinggame.ui.viewmodels.MapViewModel
import ktx.scene2d.KTable
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor

class MapView(
    model : MapViewModel,
    skin : Skin,
) : Table(skin), KTable {

    init {
        setFillParent(true)
        stage = getStage()
    }

}

@Scene2dDsl
fun <S> KWidget<S>.mapView(
    model : MapViewModel,
    skin : Skin = Scene2DSkin.defaultSkin,
    init : MapView.(S) -> Unit = { }
) : MapView = actor(MapView(model, skin), init)
