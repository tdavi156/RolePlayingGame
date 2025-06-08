package com.github.jacks.roleplayinggame.components

import com.github.jacks.roleplayinggame.dialog.Dialog
import com.github.quillraven.fleks.Entity

enum class DialogId {
    NONE,
    SLIME;
}

data class DialogComponent(
    var dialogId : DialogId = DialogId.NONE
) {
    var interactingEntity : Entity? = null
    var currentDialog : Dialog? = null
}
