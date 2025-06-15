package com.github.jacks.roleplayinggame.quest

sealed interface Quest {
    val questId : String
    fun isComplete() : Boolean
    fun updateProgress()
}

data class KillQuest(var progress : Int = 0) : Quest {
    override val questId = "killQuest"

    override fun isComplete(): Boolean = progress == 10

    override fun updateProgress() {
        TODO("Not yet implemented")
    }

}
