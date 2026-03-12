package com.github.jacks.roleplayinggame.actors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable

class FlipImage : Image() {
    var flipX = false
    var useWhiteShader = false

    override fun draw(batch: Batch, parentAlpha: Float) {
        validate()
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)

        val prevShader = if (useWhiteShader) {
            val prev = batch.shader
            batch.shader = whiteShader
            prev
        } else null

        val toDraw = drawable
        if (toDraw is TransformDrawable && (scaleX != 1f || scaleY != 1f || rotation != 0f)) {
            toDraw.draw(
                batch,
                if (flipX) x + imageX + imageWidth * scaleX else x + imageX,
                y + imageY,
                originX - imageX,
                originY - imageY,
                imageWidth,
                imageHeight,
                if (flipX) -scaleX else scaleX,
                scaleY,
                rotation
            )
        } else {
            toDraw?.draw(
                batch,
                if (flipX) x + imageX + imageWidth * scaleX else x + imageX,
                y + imageY,
                if (flipX) -imageWidth * scaleX else imageWidth * scaleX,
                imageHeight * scaleY
            )
        }

        if (useWhiteShader) {
            batch.shader = prevShader
        }
    }

    companion object {
        private val whiteShader: ShaderProgram by lazy {
            val shader = ShaderProgram(
                Gdx.files.internal("shaders/default.vert"),
                Gdx.files.internal("shaders/white_flash.frag")
            )
            if (!shader.isCompiled) {
                Gdx.app.error("FlipImage", "White flash shader compilation failed:\n${shader.log}")
            }
            shader
        }
    }
}
