package com.github.jacks.roleplayinggame.ui.widgets

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.github.jacks.roleplayinggame.ui.Drawables
import com.github.jacks.roleplayinggame.ui.Labels
import com.github.jacks.roleplayinggame.ui.get
import ktx.actors.plusAssign
import ktx.actors.txt
import ktx.scene2d.KTable
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor
import ktx.scene2d.label
import ktx.scene2d.stack

class BattleStatBar(
    private val skin: Skin
) : Table(skin), KTable {

    private val nameLabel: Label
    private val levelLabel: Label
    private val hpBarFill: Image
    private val manaBarFill: Image

    init {
        // Row 1: Name (left) + Level (right)
        nameLabel = label("Player", Labels.SMALL.skinKey) {
            it.expandX().left()
        }
        levelLabel = label("Lv.1", Labels.SMALL.skinKey) {
            it.right().row()
        }

        // Row 2: HP bar (grey background + green fill)
        hpBarFill = Image(this@BattleStatBar.skin[Drawables.BAR_GREEN_THICK])
        stack {
            actor(Image(this@BattleStatBar.skin[Drawables.BAR_GREY_THICK]))
            actor(this@BattleStatBar.hpBarFill)
            it.expandX().fillX().height(8f).colspan(2).row()
        }

        // Row 3: Mana bar (grey background + blue-tinted green fill)
        manaBarFill = Image(this@BattleStatBar.skin[Drawables.BAR_GREEN_THICK]).apply {
            color = Color(0.3f, 0.5f, 0.9f, 1f)
        }
        stack {
            actor(Image(this@BattleStatBar.skin[Drawables.BAR_GREY_THICK]))
            actor(this@BattleStatBar.manaBarFill)
            it.expandX().fillX().height(8f).colspan(2).padTop(2f)
        }
    }

    override fun layout() {
        super.layout()
        hpBarFill.setOrigin(0f, 0f)
        manaBarFill.setOrigin(0f, 0f)
    }

    fun updateName(name: String) {
        nameLabel.txt = name
    }

    fun updateLevel(level: Int) {
        levelLabel.txt = "Lv.$level"
    }

    fun life(percentage: Float, duration: Float = 0.75f) {
        hpBarFill.clearActions()
        hpBarFill += Actions.scaleTo(MathUtils.clamp(percentage, 0f, 1f), 1f, duration)
    }

    fun mana(percentage: Float, duration: Float = 0.75f) {
        manaBarFill.clearActions()
        manaBarFill += Actions.scaleTo(MathUtils.clamp(percentage, 0f, 1f), 1f, duration)
    }
}

@Scene2dDsl
fun <S> KWidget<S>.battleStatBar(
    skin: Skin = Scene2DSkin.defaultSkin,
    init: BattleStatBar.(S) -> Unit = {}
): BattleStatBar = actor(BattleStatBar(skin), init)
