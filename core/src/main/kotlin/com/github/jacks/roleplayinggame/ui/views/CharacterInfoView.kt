package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.github.jacks.roleplayinggame.saveManager.CharacterData
import com.github.jacks.roleplayinggame.ui.Drawables
import com.github.jacks.roleplayinggame.ui.Labels
import com.github.jacks.roleplayinggame.ui.get
import com.github.jacks.roleplayinggame.ui.viewmodels.CharacterInfoViewModel
import ktx.actors.txt
import ktx.scene2d.KTable
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.stack
import ktx.scene2d.table

class CharacterInfoView(
    private val model: CharacterInfoViewModel,
    skin: Skin,
) : Table(skin), KTable {

    // Left panel
    private val partyListTable = Table(skin)
    private val rowLabels = mutableListOf<Label>()

    // Right panel detail
    private lateinit var detailNameLabel: Label
    private lateinit var detailLevelLabel: Label
    private lateinit var detailXpLabel: Label
    private lateinit var detailXpBar: Image
    private lateinit var detailHpLabel: Label
    private lateinit var detailManaLabel: Label
    private lateinit var detailAttackLabel: Label
    private lateinit var detailDefenseLabel: Label
    private lateinit var detailSpeedLabel: Label
    private lateinit var detailGoldLabel: Label

    init {
        setFillParent(true)

        table { outerCell ->
            background = skin[Drawables.FRAME_BGD]
            pad(12f)

            // ── Title ──────────────────────────────────────────────────────────
            label("Party", Labels.MEDIUM.skinKey) { cell ->
                cell.colspan(2).center().padBottom(10f)
            }
            row()

            // ── Left panel — party list ────────────────────────────────────────
            val scroll = ScrollPane(this@CharacterInfoView.partyListTable, skin).apply {
                setFadeScrollBars(false)
            }
            add(scroll).width(140f).fillY().padRight(12f).top()

            // ── Right panel — detail ──────────────────────────────────────────
            table { detailCell ->
                this@CharacterInfoView.detailNameLabel = label("—", Labels.MEDIUM.skinKey) { cell ->
                    cell.colspan(2).center().padBottom(6f)
                }
                row()

                label("Level:", Labels.SMALL.skinKey) { cell -> cell.left().padRight(8f).padBottom(4f) }
                this@CharacterInfoView.detailLevelLabel = label("1", Labels.SMALL.skinKey) { cell ->
                    cell.left().padBottom(4f).expandX()
                }
                row()

                label("XP:", Labels.SMALL.skinKey) { cell -> cell.left().padRight(8f).padBottom(4f) }
                this@CharacterInfoView.detailXpLabel = label("0 / 50", Labels.SMALL.skinKey) { cell ->
                    cell.left().padBottom(4f)
                }
                row()

                stack { stackCell ->
                    image(skin[Drawables.BAR_GREY_THICK])
                    this@CharacterInfoView.detailXpBar = image(skin[Drawables.BAR_GREEN_THICK])
                    stackCell.colspan(2).fillX().height(16f).padBottom(8f)
                }
                row()

                label("HP:", Labels.SMALL.skinKey) { cell -> cell.left().padRight(8f).padBottom(4f) }
                this@CharacterInfoView.detailHpLabel = label("0 / 0", Labels.SMALL.skinKey) { cell ->
                    cell.left().padBottom(4f)
                }
                row()

                label("Mana:", Labels.SMALL.skinKey) { cell -> cell.left().padRight(8f).padBottom(4f) }
                this@CharacterInfoView.detailManaLabel = label("0 / 0", Labels.SMALL.skinKey) { cell ->
                    cell.left().padBottom(4f)
                }
                row()

                label("Attack:", Labels.SMALL.skinKey) { cell -> cell.left().padRight(8f).padBottom(4f) }
                this@CharacterInfoView.detailAttackLabel = label("0", Labels.SMALL.skinKey) { cell ->
                    cell.left().padBottom(4f)
                }
                row()

                label("Defense:", Labels.SMALL.skinKey) { cell -> cell.left().padRight(8f).padBottom(4f) }
                this@CharacterInfoView.detailDefenseLabel = label("0", Labels.SMALL.skinKey) { cell ->
                    cell.left().padBottom(4f)
                }
                row()

                label("Speed:", Labels.SMALL.skinKey) { cell -> cell.left().padRight(8f).padBottom(4f) }
                this@CharacterInfoView.detailSpeedLabel = label("0.0", Labels.SMALL.skinKey) { cell ->
                    cell.left().padBottom(4f)
                }
                row()

                label("Gold:", Labels.SMALL.skinKey) { cell -> cell.left().padRight(8f) }
                this@CharacterInfoView.detailGoldLabel = label("0", Labels.YELLOW.skinKey) { cell ->
                    cell.left()
                }

                detailCell.top().expandX()
            }

            outerCell.expand().center().width(440f)
        }

        // ── ViewModel bindings ─────────────────────────────────────────────────

        model.onPropertyChange(CharacterInfoViewModel::partyList) { list ->
            rebuildPartyList(list)
        }
        model.onPropertyChange(CharacterInfoViewModel::focusedIndex) { idx ->
            highlightRow(idx)
        }
        model.onPropertyChange(CharacterInfoViewModel::activeOverworldId) {
            rebuildPartyList(model.partyList)
        }
        model.onPropertyChange(CharacterInfoViewModel::detailName) { detailNameLabel.txt = it }
        model.onPropertyChange(CharacterInfoViewModel::detailLevel) { detailLevelLabel.txt = it.toString() }
        model.onPropertyChange(CharacterInfoViewModel::detailExperience) {
            detailXpLabel.txt = "$it / ${model.detailExpToNext}"
        }
        model.onPropertyChange(CharacterInfoViewModel::detailExpToNext) { next ->
            detailXpLabel.txt = "${model.detailExperience} / $next"
            detailXpBar.scaleX = if (next > 0) model.detailExperience.toFloat() / next.toFloat() else 0f
        }
        model.onPropertyChange(CharacterInfoViewModel::detailCurrentHp) {
            detailHpLabel.txt = "${it.toInt()} / ${model.detailMaxHp.toInt()}"
        }
        model.onPropertyChange(CharacterInfoViewModel::detailMaxHp) {
            detailHpLabel.txt = "${model.detailCurrentHp.toInt()} / ${it.toInt()}"
        }
        model.onPropertyChange(CharacterInfoViewModel::detailCurrentMana) {
            detailManaLabel.txt = "${it.toInt()} / ${model.detailMaxMana.toInt()}"
        }
        model.onPropertyChange(CharacterInfoViewModel::detailMaxMana) {
            detailManaLabel.txt = "${model.detailCurrentMana.toInt()} / ${it.toInt()}"
        }
        model.onPropertyChange(CharacterInfoViewModel::detailAttack) { detailAttackLabel.txt = it.toInt().toString() }
        model.onPropertyChange(CharacterInfoViewModel::detailDefense) { detailDefenseLabel.txt = it.toInt().toString() }
        model.onPropertyChange(CharacterInfoViewModel::detailSpeed) { detailSpeedLabel.txt = it.toString() }
        model.onPropertyChange(CharacterInfoViewModel::detailGold) { detailGoldLabel.txt = it.toString() }
    }

    private fun rebuildPartyList(list: List<CharacterData>) {
        partyListTable.clear()
        rowLabels.clear()
        val activeId = model.activeOverworldId
        list.forEachIndexed { idx, char ->
            val marker = if (char.characterId == activeId) "► " else "  "
            val rowLabel = Label(
                "$marker${char.characterName}  Lv${char.currentLevel}  ${char.currentHealth.toInt()}/${char.maxHealth.toInt()}",
                skin, Labels.SMALL.skinKey
            )
            rowLabels.add(rowLabel)
            partyListTable.add(rowLabel).left().padBottom(4f).expandX().fillX()
            partyListTable.row()
        }
        highlightRow(model.focusedIndex)
    }

    private fun highlightRow(idx: Int) {
        rowLabels.forEachIndexed { i, lbl ->
            val current = lbl.text.toString()
            val stripped = if (current.startsWith("> ")) current.removePrefix("> ") else current
            lbl.txt = if (i == idx) "> $stripped" else stripped
        }
    }

    override fun setVisible(visible: Boolean) {
        super.setVisible(visible)
        if (visible) model.refresh()
    }
}

@Scene2dDsl
fun <S> KWidget<S>.characterInfoView(
    model: CharacterInfoViewModel,
    skin: Skin = Scene2DSkin.defaultSkin,
    init: CharacterInfoView.(S) -> Unit = { }
): CharacterInfoView = actor(CharacterInfoView(model, skin), init)
