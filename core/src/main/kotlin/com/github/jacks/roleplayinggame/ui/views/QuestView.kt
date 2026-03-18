package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.github.jacks.roleplayinggame.ui.Drawables
import com.github.jacks.roleplayinggame.ui.Labels
import com.github.jacks.roleplayinggame.ui.get
import com.github.jacks.roleplayinggame.ui.viewmodels.QuestPanelFocus
import com.github.jacks.roleplayinggame.ui.viewmodels.QuestRowUiState
import com.github.jacks.roleplayinggame.ui.viewmodels.QuestViewModel
import ktx.actors.txt
import ktx.scene2d.KTable
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor
import ktx.scene2d.label
import ktx.scene2d.table

class QuestView(
    val model: QuestViewModel,
    skin: Skin,
) : Table(skin), KTable {

    private lateinit var activeListTable: Table
    private lateinit var completedListTable: Table
    private lateinit var infoNameLabel: Label
    private lateinit var infoDescLabel: Label
    private lateinit var infoProgressLabel: Label
    private lateinit var infoRewardLabel: Label

    init {
        setFillParent(true)
        pad(16f)
        background = skin[Drawables.FRAME_BGD]

        // Title
        label("Quest Log", Labels.MEDIUM.skinKey) { it.colspan(2).center().padBottom(8f) }
        row()

        // Panel headers
        label("Active Quests", Labels.SMALL.skinKey) { it.expandX().center().padRight(4f) }
        label("Completed Quests", Labels.SMALL.skinKey) { it.expandX().center() }
        row()

        // Quest list panels
        table { leftCell ->
            background = skin[Drawables.FRAME_BGD]
            pad(4f)
            this@QuestView.activeListTable = this
            leftCell.expand().fill().padRight(4f).minHeight(120f)
        }
        table { rightCell ->
            background = skin[Drawables.FRAME_BGD]
            pad(4f)
            this@QuestView.completedListTable = this
            rightCell.expand().fill().minHeight(120f)
        }
        row()

        // Info panel
        table { infoCell ->
            background = skin[Drawables.FRAME_BGD]
            pad(8f)
            left()
            this@QuestView.infoNameLabel = label("", Labels.SMALL.skinKey) { it.left().expandX().row() }
            this@QuestView.infoDescLabel = label("", Labels.TINY.skinKey) { it.left().expandX().row() }
            this@QuestView.infoProgressLabel = label("", Labels.TINY.skinKey) { it.left().expandX().row() }
            this@QuestView.infoRewardLabel = label("", Labels.TINY.skinKey) { it.left().expandX() }
            infoCell.colspan(2).expandX().fill().padTop(4f).minHeight(80f)
        }

        // Bind to model
        model.onPropertyChange(QuestViewModel::activeQuests) {
            rebuildPanel(activeListTable, it, model.activePanelFocus == QuestPanelFocus.ACTIVE, model.focusedQuestIndex)
        }
        model.onPropertyChange(QuestViewModel::completedQuests) {
            rebuildPanel(completedListTable, it, model.activePanelFocus == QuestPanelFocus.COMPLETED, model.focusedQuestIndex)
        }
        model.onPropertyChange(QuestViewModel::activePanelFocus) { focus ->
            rebuildPanel(activeListTable, model.activeQuests, focus == QuestPanelFocus.ACTIVE, model.focusedQuestIndex)
            rebuildPanel(completedListTable, model.completedQuests, focus == QuestPanelFocus.COMPLETED, model.focusedQuestIndex)
            updateInfoPanel()
        }
        model.onPropertyChange(QuestViewModel::focusedQuestIndex) { idx ->
            rebuildPanel(activeListTable, model.activeQuests, model.activePanelFocus == QuestPanelFocus.ACTIVE, idx)
            rebuildPanel(completedListTable, model.completedQuests, model.activePanelFocus == QuestPanelFocus.COMPLETED, idx)
            updateInfoPanel()
        }
    }

    private fun rebuildPanel(panelTable: Table, quests: List<QuestRowUiState>, isFocused: Boolean, focusedIndex: Int) {
        panelTable.clearChildren()
        if (quests.isEmpty()) {
            panelTable.add(Label("—", this.skin, Labels.TINY.skinKey)).expandX().center()
        } else {
            quests.forEachIndexed { idx, quest ->
                val isRowFocused = isFocused && idx == focusedIndex
                val labelStyle = when {
                    isRowFocused -> Labels.SMALL_YELLOW_BGD.skinKey
                    quest.isReadyToDeliver -> Labels.SMALL_GREEN_BGD.skinKey
                    else -> Labels.TINY.skinKey
                }
                val displayName = if (quest.isReadyToDeliver) "★ ${quest.questName}" else quest.questName
                panelTable.add(Label(displayName, this.skin, labelStyle)).expandX().left().padBottom(2f)
                panelTable.row()
            }
        }
        panelTable.top()
    }

    private fun updateInfoPanel() {
        val quest = model.focusedQuest
        if (quest == null) {
            infoNameLabel.txt = ""
            infoDescLabel.txt = ""
            infoProgressLabel.txt = ""
            infoRewardLabel.txt = ""
        } else {
            infoNameLabel.txt = quest.questName
            infoDescLabel.txt = quest.description
            infoProgressLabel.txt = quest.progressText
            infoRewardLabel.txt = quest.rewardText
        }
    }
}

@Scene2dDsl
fun <S> KWidget<S>.questView(
    model: QuestViewModel,
    skin: Skin = Scene2DSkin.defaultSkin,
    init: QuestView.(S) -> Unit = {},
): QuestView = actor(QuestView(model, skin), init)
