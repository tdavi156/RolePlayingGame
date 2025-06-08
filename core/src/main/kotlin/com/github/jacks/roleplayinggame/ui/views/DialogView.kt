package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.github.jacks.roleplayinggame.ui.Buttons
import com.github.jacks.roleplayinggame.ui.Drawables
import com.github.jacks.roleplayinggame.ui.get
import com.github.jacks.roleplayinggame.ui.Labels
import com.github.jacks.roleplayinggame.ui.viewmodels.DialogViewModel
import ktx.actors.alpha
import ktx.actors.onClick
import ktx.actors.txt
import ktx.scene2d.*

class DialogView(
    private val model : DialogViewModel,
    skin : Skin
) : Table(skin), KTable {

    private val dialogText : Label
    private val buttonArea : Table

    init {
        setFillParent(true)
        this.alpha = 0f

        table {
            background = skin[Drawables.FRAME_BGD]

            this@DialogView.dialogText = label(text = "", style = Labels.FRAME.skinKey) { labelCell ->
                this.setAlignment(Align.topLeft)
                this.wrap = true
                labelCell.expand().fill().pad(8f).row()
            }

            this@DialogView.buttonArea = table { buttonAreaCell ->
                this.defaults().expand()
                textButton("", Buttons.TEXT_BUTTON.skinKey)
                textButton("", Buttons.TEXT_BUTTON.skinKey)
                buttonAreaCell.expandX().fillX().pad(0f, 8f, 8f, 8f)
            }

            it.expand().width(200f).height(130f).center().row()
        }

        model.onPropertyChange(DialogViewModel::text) {
            dialogText.txt = it
            this.alpha = 1f
        }
        model.onPropertyChange(DialogViewModel::completed) { completed ->
            if (completed) {
                this.alpha = 0f
                this.buttonArea.clearChildren()
            }
        }
        model.onPropertyChange(DialogViewModel::options) { dialogOptions ->
            buttonArea.clearChildren()
            dialogOptions.forEach {
                buttonArea.add(textButton(it.text, Buttons.TEXT_BUTTON.skinKey).apply {
                    onClick { this@DialogView.model.triggerOption(it.index) }
                })
            }
        }
    }
}

@Scene2dDsl
fun <S> KWidget<S>.dialogView(
    model : DialogViewModel,
    skin : Skin = Scene2DSkin.defaultSkin,
    init : DialogView.(S) -> Unit = { }
) : DialogView = actor(DialogView(model, skin), init)
