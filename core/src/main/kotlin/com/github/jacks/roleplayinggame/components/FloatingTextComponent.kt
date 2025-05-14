package com.github.jacks.roleplayinggame.components

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeOut
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.github.quillraven.fleks.ComponentListener
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Qualifier
import ktx.actors.plusAssign
import ktx.math.vec2

class FloatingTextComponent {
    val textStartLocation = vec2()
    val textTargetLocation = vec2()
    var textDuration = 0f
    var time = 0f
    lateinit var label : Label

    companion object {
        class FloatingTextComponentListener(
            @Qualifier("uiStage") private val uiStage : Stage
        ) : ComponentListener<FloatingTextComponent> {
            override fun onComponentAdded(entity: Entity, component: FloatingTextComponent) {
                uiStage.addActor(component.label)
                component.label += fadeOut(component.textDuration, Interpolation.pow3OutInverse)
                component.textTargetLocation.set(
                    component.textStartLocation.x + MathUtils.random(-0.2f, 0.2f),
                    component.textStartLocation.y + 0.5f
                )
            }

            override fun onComponentRemoved(entity: Entity, component: FloatingTextComponent) {
                uiStage.root.removeActor(component.label)
            }
        }
    }
}
