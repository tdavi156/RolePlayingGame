package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.systems.AttackSystem.Companion.AABB_RECT
import com.github.quillraven.fleks.IntervalSystem
import ktx.assets.disposeSafely
import ktx.graphics.use

class DebugSystem(
    private val physicsWorld : World,
    private val stage : Stage
) : IntervalSystem(enabled = false) {

    private lateinit var physicsRenderer : Box2DDebugRenderer
    private lateinit var shapeRenderer : ShapeRenderer

    init {
        if(enabled) {
            physicsRenderer = Box2DDebugRenderer()
            shapeRenderer = ShapeRenderer()
        }
    }

    override fun onTick() {
        physicsRenderer.render(physicsWorld, stage.camera.combined)
        shapeRenderer.use(ShapeRenderer.ShapeType.Line, stage.camera.combined) {
            it.setColor(1f, 0f, 0f, 0f)
            it.rect(AABB_RECT.x, AABB_RECT.y, AABB_RECT.width - AABB_RECT.x, AABB_RECT.height - AABB_RECT.y)
        }
    }

    override fun onDispose() {
        if(enabled) {
            physicsRenderer.disposeSafely()
            shapeRenderer.disposeSafely()
        }
    }
}
