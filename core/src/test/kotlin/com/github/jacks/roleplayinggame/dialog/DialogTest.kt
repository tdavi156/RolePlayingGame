package com.github.jacks.roleplayinggame.dialog

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DialogTest {

    @Test
    fun testDialogDsl() {
        lateinit var firstNode : Node
        lateinit var secondNode : Node

        val testDialog = dialog("testDialog") {
            firstNode = node(0, "node 0 text") {
                option("next") {
                    action = { this@dialog.goToNode(1) }
                }
            }

            secondNode = node(1, "node 1 text") {
                option("previous") {
                    action = { this@dialog.goToNode(0) }
                }
                option("end") {
                    action = { this@dialog.endDialog() }
                }
            }
        }

        testDialog.startDialog()
        assertEquals(firstNode, testDialog.currentNode)
        assertNotEquals(secondNode, testDialog.currentNode)

        testDialog.triggerOption(0)
        assertEquals(secondNode, testDialog.currentNode)

        testDialog.triggerOption(1)
        assertTrue(testDialog.isComplete())
    }
}
