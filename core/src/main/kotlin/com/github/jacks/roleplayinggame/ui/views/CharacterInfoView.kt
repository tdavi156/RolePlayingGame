package com.github.jacks.roleplayinggame.ui.views

import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
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

    private lateinit var levelLabel: Label
    private lateinit var xpLabel: Label
    private lateinit var xpBar: Image
    private lateinit var hpLabel: Label
    private lateinit var manaLabel: Label
    private lateinit var attackLabel: Label
    private lateinit var defenseLabel: Label
    private lateinit var speedLabel: Label

    init {
        setFillParent(true)

        table { outerCell ->
            background = skin[Drawables.FRAME_BGD]
            pad(12f)

            label("Character", Labels.MEDIUM.skinKey) { cell ->
                cell.colspan(2).center().padBottom(10f)
            }
            row()

            label("Level:", Labels.SMALL.skinKey) { cell ->
                cell.left().padRight(10f).padBottom(4f)
            }
            this@CharacterInfoView.levelLabel = label("1", Labels.SMALL.skinKey) { cell ->
                cell.left().padBottom(4f).expandX()
            }
            row()

            label("XP:", Labels.SMALL.skinKey) { cell ->
                cell.left().padRight(10f).padBottom(4f)
            }
            this@CharacterInfoView.xpLabel = label("0 / 50", Labels.SMALL.skinKey) { cell ->
                cell.left().padBottom(4f)
            }
            row()

            stack { stackCell ->
                image(skin[Drawables.BAR_GREY_THICK])
                this@CharacterInfoView.xpBar = image(skin[Drawables.BAR_GREEN_THICK])
                stackCell.colspan(2).fillX().height(20f).padBottom(10f)
            }
            row()

            label("HP:", Labels.SMALL.skinKey) { cell ->
                cell.left().padRight(10f).padBottom(4f)
            }
            this@CharacterInfoView.hpLabel = label("0 / 0", Labels.SMALL.skinKey) { cell ->
                cell.left().padBottom(4f)
            }
            row()

            label("Mana:", Labels.SMALL.skinKey) { cell ->
                cell.left().padRight(10f).padBottom(4f)
            }
            this@CharacterInfoView.manaLabel = label("0 / 0", Labels.SMALL.skinKey) { cell ->
                cell.left().padBottom(4f)
            }
            row()

            label("Attack:", Labels.SMALL.skinKey) { cell ->
                cell.left().padRight(10f).padBottom(4f)
            }
            this@CharacterInfoView.attackLabel = label("0", Labels.SMALL.skinKey) { cell ->
                cell.left().padBottom(4f)
            }
            row()

            label("Defense:", Labels.SMALL.skinKey) { cell ->
                cell.left().padRight(10f).padBottom(4f)
            }
            this@CharacterInfoView.defenseLabel = label("0", Labels.SMALL.skinKey) { cell ->
                cell.left().padBottom(4f)
            }
            row()

            label("Speed:", Labels.SMALL.skinKey) { cell ->
                cell.left().padRight(10f)
            }
            this@CharacterInfoView.speedLabel = label("0.0", Labels.SMALL.skinKey) { cell ->
                cell.left()
            }

            outerCell.expand().center().width(280f)
        }

        model.onPropertyChange(CharacterInfoViewModel::playerLevel) { level ->
            levelLabel.txt = level.toString()
        }
        model.onPropertyChange(CharacterInfoViewModel::playerExperience) { xp ->
            xpLabel.txt = "$xp / ${model.playerExperienceToNext}"
        }
        model.onPropertyChange(CharacterInfoViewModel::playerExperienceToNext) { xpNext ->
            xpLabel.txt = "${model.playerExperience} / $xpNext"
            xpBar.scaleX = if (xpNext > 0) model.playerExperience.toFloat() / xpNext.toFloat() else 0f
        }
        model.onPropertyChange(CharacterInfoViewModel::playerCurrentHealth) { hp ->
            hpLabel.txt = "${hp.toInt()} / ${model.playerMaxHealth.toInt()}"
        }
        model.onPropertyChange(CharacterInfoViewModel::playerMaxHealth) { maxHp ->
            hpLabel.txt = "${model.playerCurrentHealth.toInt()} / ${maxHp.toInt()}"
        }
        model.onPropertyChange(CharacterInfoViewModel::playerCurrentMana) { mana ->
            manaLabel.txt = "${mana.toInt()} / ${model.playerMaxMana.toInt()}"
        }
        model.onPropertyChange(CharacterInfoViewModel::playerMaxMana) { maxMana ->
            manaLabel.txt = "${model.playerCurrentMana.toInt()} / ${maxMana.toInt()}"
        }
        model.onPropertyChange(CharacterInfoViewModel::playerAttack) { attack ->
            attackLabel.txt = attack.toInt().toString()
        }
        model.onPropertyChange(CharacterInfoViewModel::playerDefense) { defense ->
            defenseLabel.txt = defense.toInt().toString()
        }
        model.onPropertyChange(CharacterInfoViewModel::playerSpeed) { speed ->
            speedLabel.txt = speed.toString()
        }
    }

    override fun setVisible(visible: Boolean) {
        super.setVisible(visible)
        if (visible) {
            model.refreshStats()
        }
    }
}

@Scene2dDsl
fun <S> KWidget<S>.characterInfoView(
    model: CharacterInfoViewModel,
    skin: Skin = Scene2DSkin.defaultSkin,
    init: CharacterInfoView.(S) -> Unit = { }
): CharacterInfoView = actor(CharacterInfoView(model, skin), init)
