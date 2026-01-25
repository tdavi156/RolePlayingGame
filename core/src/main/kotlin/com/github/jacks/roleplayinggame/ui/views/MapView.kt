package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.github.jacks.roleplayinggame.ui.Labels
import com.github.jacks.roleplayinggame.ui.viewmodels.MapViewModel
import ktx.scene2d.KTable
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor
import ktx.scene2d.label
import ktx.scene2d.table

class MapView(
    model : MapViewModel,
    skin : Skin,
) : Table(skin), KTable {

    init {
        setFillParent(true)
        stage = getStage()

        table {
            label("test label on Map view", Labels.RED.skinKey) { cell ->
                cell.center().pad(3f)
            }
        }
    }

}

@Scene2dDsl
fun <S> KWidget<S>.mapView(
    model : MapViewModel,
    skin : Skin = Scene2DSkin.defaultSkin,
    init : MapView.(S) -> Unit = { }
) : MapView = actor(MapView(model, skin), init)
