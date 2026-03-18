package com.github.jacks.roleplayinggame.configurations

import com.github.jacks.roleplayinggame.dialog.Dialog
import com.github.jacks.roleplayinggame.dialog.dialog
import com.github.jacks.roleplayinggame.systems.QuestStatus
import com.github.jacks.roleplayinggame.systems.QuestSystem

enum class DialogId {
    NO_DIALOG,
    SLIME,
    SIGN_1,
    SIGN_2,
    QUEST_MAN,
    RECRUIT_CHARACTER_2,
    RECRUIT_CHARACTER_3;
}

private val dialogCache = mutableMapOf<DialogId, Dialog>()

fun getDialogFlow(dialogId: DialogId): Dialog? {
    if (dialogId == DialogId.NO_DIALOG || dialogId == DialogId.QUEST_MAN) return null
    // Recruit dialogs are always built fresh so npcEntity is not stale
    if (dialogId == DialogId.RECRUIT_CHARACTER_2 || dialogId == DialogId.RECRUIT_CHARACTER_3) {
        return buildDialogFlow(dialogId)
    }
    dialogCache[dialogId]?.let { return it }
    val built = buildDialogFlow(dialogId) ?: return null
    dialogCache[dialogId] = built
    return built
}

private fun buildDialogFlow(dialogId: DialogId): Dialog? = when (dialogId) {
    DialogId.NO_DIALOG, DialogId.QUEST_MAN -> null
    DialogId.RECRUIT_CHARACTER_2 -> dialog(dialogId.name) {
        node(0, "I am the Cleric. I can heal allies and smite foes. Want me to join your party?") {
            option("Yes") {
                action = {
                    this@dialog.addCharacterToParty(2)
                    this@dialog.endDialog()
                }
            }
            option("No") {
                action = { this@dialog.endDialog() }
            }
        }
    }
    DialogId.RECRUIT_CHARACTER_3 -> dialog(dialogId.name) {
        node(0, "I am the Ranger. I can take a hit and keep standing. Care to have me along?") {
            option("Yes") {
                action = {
                    this@dialog.addCharacterToParty(3)
                    this@dialog.endDialog()
                }
            }
            option("No") {
                action = { this@dialog.endDialog() }
            }
        }
    }
    DialogId.SLIME -> dialog(dialogId.name) {
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
    DialogId.SIGN_1 -> dialog(dialogId.name) {
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
    DialogId.SIGN_2 -> dialog(dialogId.name) {
        node(0, "This way leads to Map 2") {
            option("Okay") {
                action = { this@dialog.endDialog() }
            }
        }
    }
}

/** Returns a fresh QUEST_MAN dialog each interaction, branching on current quest status. */
fun getQuestManDialogFlow(questSystem: QuestSystem): Dialog {
    return when (questSystem.getState(1).status) {
        QuestStatus.NOT_STARTED -> dialog(DialogId.QUEST_MAN.name) {
            node(0, "Can you kill 1 blue slime for me?") {
                option("Accept") {
                    action = {
                        this@dialog.acceptQuest(1)
                        this@dialog.goToNode(1)
                    }
                }
                option("Decline") {
                    action = { this@dialog.endDialog() }
                }
            }
            node(1, "Great! Come back when it's done.") {
                option("Okay") {
                    action = { this@dialog.endDialog() }
                }
            }
        }
        QuestStatus.ACTIVE -> dialog(DialogId.QUEST_MAN.name) {
            node(0, "You haven't finished yet — come back when the blue slime is defeated.") {
                option("Okay") {
                    action = { this@dialog.endDialog() }
                }
            }
        }
        QuestStatus.CONDITIONS_MET -> dialog(DialogId.QUEST_MAN.name) {
            node(0, "You did it! Here's your reward — 100 gold.") {
                option("Thanks") {
                    action = {
                        this@dialog.completeQuest(1)
                        this@dialog.endDialog()
                    }
                }
            }
        }
        QuestStatus.COMPLETED -> dialog(DialogId.QUEST_MAN.name) {
            node(0, "Thanks again for helping me!") {
                option("No problem") {
                    action = { this@dialog.endDialog() }
                }
            }
        }
    }
}
