package com.github.jacks.roleplayinggame.dialog

import ktx.app.gdxError

@DslMarker
annotation class DialogDslMarker

fun dialog(id : String, configuration : Dialog.() -> Unit) : Dialog {
    return Dialog(id).apply(configuration)
}

@DialogDslMarker
data class Dialog(
    val id : String,
    private val nodes : MutableList<Node> = mutableListOf(),
    private var complete : Boolean = false
) {
    lateinit var currentNode : Node

    fun node(id : Int, text : String, configuration: Node.() -> Unit) : Node {
        return Node(id, text).apply {
            this.configuration()
            this@Dialog.nodes += this
        }
    }

    fun goToNode(nodeId : Int) {
        currentNode = nodes.firstOrNull { it.id == nodeId }
            ?: gdxError("There is no node with id $nodeId in dialog $this")
    }

    fun startDialog() {
        complete = false
        currentNode = nodes.first()
    }

    fun endDialog() {
        complete = true
    }

    fun isComplete() : Boolean {
        return complete
    }

    fun triggerOption(optionId : Int) {
        val option = currentNode[optionId] ?: gdxError("")
        option.action()
    }
}

@DialogDslMarker
data class Node(val id : Int, val text : String) {
    val options : MutableList<Option> = mutableListOf()

    fun option(text : String, configuration: Option.() -> Unit) : Option {
        return Option(options.size, text).apply {
            this.configuration()
            this@Node.options += this
        }
    }

    operator fun get(optionId : Int) : Option? {
        return options.getOrNull(optionId)
    }
}

@DialogDslMarker
data class Option(
    val id : Int,
    val text : String,
    var action : () -> Unit = { }
) {

}
