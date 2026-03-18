package com.github.jacks.roleplayinggame.components

import com.github.jacks.roleplayinggame.configurations.DialogId
import com.github.jacks.roleplayinggame.dialog.Dialog
import com.github.quillraven.fleks.Entity

data class DialogComponent(
    var dialogId : DialogId = DialogId.NO_DIALOG
) {
    var interactingEntity : Entity? = null
    var currentDialog : Dialog? = null
}
