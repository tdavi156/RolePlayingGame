package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.components.DialogComponent
import com.github.jacks.roleplayinggame.components.DisarmComponent
import com.github.jacks.roleplayinggame.components.MoveComponent
import com.github.jacks.roleplayinggame.configurations.DialogId
import com.github.jacks.roleplayinggame.configurations.getDialogFlow
import com.github.jacks.roleplayinggame.configurations.getQuestManDialogFlow
import com.github.jacks.roleplayinggame.events.EntityDialogEvent
import com.github.jacks.roleplayinggame.events.fire
import com.github.quillraven.fleks.AllOf
import com.github.quillraven.fleks.ComponentMapper
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem

@AllOf([DialogComponent::class])
class DialogSystem(
    private val dialogComponents : ComponentMapper<DialogComponent>,
    private val moveComponents : ComponentMapper<MoveComponent>,
    private val disarmComponents : ComponentMapper<DisarmComponent>,
    private val stage : Stage
) : IteratingSystem() {

    private val questSystem by lazy { world.system<QuestSystem>() }

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

            dialog = if (dialogId == DialogId.QUEST_MAN) {
                getQuestManDialogFlow(questSystem)
            } else {
                getDialogFlow(dialogId) ?: return
            }
            dialog.stage = stage
            dialog.startDialog()
            currentDialog = dialog
            moveComponents.getOrNull(triggerEntity)?.let { it.isRooted = true }
            configureEntity(triggerEntity) { disarmComponents.add(it) }
            stage.fire(EntityDialogEvent(dialog))
        }
    }
}
