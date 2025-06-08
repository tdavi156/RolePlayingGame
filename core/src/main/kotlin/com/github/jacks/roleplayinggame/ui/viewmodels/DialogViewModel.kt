package com.github.jacks.roleplayinggame.ui.viewmodels

import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.dialog.Dialog
import com.github.jacks.roleplayinggame.events.EntityDialogEvent

class DialogViewModel(stage : Stage) : PropertyChangeSource(), EventListener {

    private lateinit var dialog : Dialog
    var text by propertyNotify("")
    var options by propertyNotify(listOf<DialogOptionModel>())
    var completed by propertyNotify(false)

    init {
        stage.addListener(this)
    }

    override fun handle(event: Event): Boolean {
        when (event) {
            is EntityDialogEvent -> {
                this.dialog = event.dialog
                updateTextAndOptions()
            }
            else -> return false
        }
        return true
    }

    fun triggerOption(optionIndex : Int) {
        dialog.triggerOption(optionIndex)
        updateTextAndOptions()
    }

    private fun updateTextAndOptions() {
        completed = dialog.isComplete()
        if (!completed) {
            text = dialog.currentNode.text
            options = dialog.currentNode.options.map { DialogOptionModel(it.id, it.text) }
        }
    }
}
