package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
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
    private val optionButtons = mutableListOf<TextButton>()
    private var focusedOptionIndex = 0

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
                buttonAreaCell.expandX().fillX().pad(0f, 8f, 8f, 8f)
            }

            it.expand().width(200f).height(130f).padTop(8f).top().row()
        }

        addListener(object : InputListener() {
            override fun keyDown(event: InputEvent, keycode: Int): Boolean {
                if (this@DialogView.alpha == 0f || optionButtons.isEmpty()) return false
                when (keycode) {
                    Keys.LEFT, Keys.UP -> {
                        focusedOptionIndex = (focusedOptionIndex - 1).coerceAtLeast(0)
                        updateButtonFocus()
                        return true
                    }
                    Keys.RIGHT, Keys.DOWN -> {
                        focusedOptionIndex = (focusedOptionIndex + 1).coerceAtMost(optionButtons.size - 1)
                        updateButtonFocus()
                        return true
                    }
                    Keys.ENTER, Keys.NUMPAD_ENTER -> {
                        model.triggerOption(focusedOptionIndex)
                        return true
                    }
                    else -> return false
                }
            }
        })

        model.onPropertyChange(DialogViewModel::text) {
            dialogText.txt = it
            this.alpha = 1f
            stage?.setKeyboardFocus(this)
        }
        model.onPropertyChange(DialogViewModel::completed) { completed ->
            if (completed) {
                this.alpha = 0f
                buttonArea.clearChildren()
                optionButtons.clear()
                focusedOptionIndex = 0
            }
        }
        model.onPropertyChange(DialogViewModel::options) { dialogOptions ->
            buttonArea.clearChildren()
            optionButtons.clear()
            focusedOptionIndex = 0
            dialogOptions.forEach { option ->
                val btn = scene2d.textButton(option.text, Buttons.BROWN_BUTTON_SMALL.skinKey) {
                    onClick { this@DialogView.model.triggerOption(option.index) }
                }
                optionButtons.add(btn)
                buttonArea.add(btn).expandX().fillX().pad(2f)
            }
            updateButtonFocus()
        }
    }

    private fun updateButtonFocus() {
        optionButtons.forEachIndexed { index, button ->
            button.isChecked = (index == focusedOptionIndex)
        }
    }
}

@Scene2dDsl
fun <S> KWidget<S>.dialogView(
    model : DialogViewModel,
    skin : Skin = Scene2DSkin.defaultSkin,
    init : DialogView.(S) -> Unit = { }
) : DialogView = actor(DialogView(model, skin), init)
