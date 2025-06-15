package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.DialogComponent
import com.github.jacks.roleplayinggame.components.DialogId
import com.github.jacks.roleplayinggame.components.DisarmComponent
import com.github.jacks.roleplayinggame.components.MoveComponent
import com.github.jacks.roleplayinggame.dialog.Dialog
import com.github.jacks.roleplayinggame.dialog.dialog
import com.github.jacks.roleplayinggame.events.EntityDialogEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import ktx.app.gdxError

@AllOf([DialogComponent::class])
class DialogSystem(
    private val dialogComponents : ComponentMapper<DialogComponent>,
    private val moveComponents : ComponentMapper<MoveComponent>,
    private val disarmComponents : ComponentMapper<DisarmComponent>,
    private val stage : Stage
) : IteratingSystem() {

    private val dialogCache = mutableMapOf<DialogId, Dialog>()

    override fun onTickEntity(entity: Entity) {
        with(dialogComponents[entity]) {
            val triggerEntity = interactingEntity ?: return
            var dialog = currentDialog

            if (dialog != null) {
                if (dialog.isComplete()) {
                    moveComponents.getOrNull(triggerEntity)?.let { it.isRooted = false }
                    configureEntity(triggerEntity) { disarmComponents.remove(it) }
                    currentDialog = null
                    interactingEntity = null
                }
                return
            }

            dialog = getDialog(dialogId).also { it.startDialog() }
            currentDialog = dialog
            moveComponents.getOrNull(triggerEntity)?.let { it.isRooted = true }
            configureEntity(triggerEntity) { disarmComponents.add(it) }
            stage.fire(EntityDialogEvent(dialog))
        }
    }

    private fun getDialog(dialogId : DialogId) : Dialog {
        val slimeDialog = dialog(dialogId.name) {
            node(0, "Hello, I am a Slime. Welcome to the world of Slime Land") {
                option("Continue") {
                    action = { this@dialog.goToNode(1) }
                }
            }
            node(1, "Can you help me defeat the other slimes?") {
                option("Back") {
                    action = { this@dialog.goToNode(0) }
                }
                option("Yes") {
                    action = { this@dialog.endDialog() }
                }
            }
        }
        val sign1Dialog = dialog(dialogId.name) {
            node(0, "Welcome to Slime World. \n I have a quest for you!") {
                option("Okay") {
                    action = { this@dialog.goToNode(1) }
                }
            }
            node(1, "Can you kill 10 slimes for me?") {
                option("Accept") {
                    action = {
                        this@dialog.acceptQuest(0)
                        this@dialog.endDialog()
                    }
                }
                option("Decline") {
                    action = { this@dialog.endDialog() }
                }
            }
        }
        val sign2Dialog = dialog(dialogId.name) {
            node(0, "This way leads to Map 2") {
                option("Okay") {
                    action = { this@dialog.endDialog() }
                }
            }
        }

        return dialogCache.getOrPut(dialogId) {
            when (dialogId) {
                DialogId.SLIME -> slimeDialog
                DialogId.SIGN_1 -> sign1Dialog
                DialogId.SIGN_2 -> sign2Dialog
                else -> gdxError("No dialog configured for $dialogId.")
            }
        }
    }
}
