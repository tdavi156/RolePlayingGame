package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.jacks.roleplayinggame.systems.AttackSystem.Companion.AABB_RECT_1
import com.github.jacks.roleplayinggame.systems.AttackSystem.Companion.AABB_RECT_2
import com.github.quillraven.fleks.IntervalSystem
import ktx.assets.disposeSafely
import ktx.graphics.use

class DebugSystem(
    private val physicsWorld : World,
    private val stage : Stage
) : IntervalSystem(enabled = true) {

    private lateinit var physicsRenderer : Box2DDebugRenderer
    private lateinit var shapeRenderer : ShapeRenderer

    init {
        if(enabled) {
            physicsRenderer = Box2DDebugRenderer()
            shapeRenderer = ShapeRenderer()
        }
    }

    override fun onTick() {
        //physicsRenderer.render(physicsWorld, stage.camera.combined)
        shapeRenderer.use(ShapeRenderer.ShapeType.Line, stage.camera.combined) {
            it.setColor(1f, 0f, 0f, 0f)
            it.rect(AABB_RECT_1.x, AABB_RECT_1.y, AABB_RECT_1.width - AABB_RECT_1.x, AABB_RECT_1.height - AABB_RECT_1.y)
        }
        shapeRenderer.use(ShapeRenderer.ShapeType.Line, stage.camera.combined) {
            it.setColor(1f, 0f, 0f, 0f)
            it.rect(AABB_RECT_2.x, AABB_RECT_2.y, AABB_RECT_2.width - AABB_RECT_2.x, AABB_RECT_2.height - AABB_RECT_2.y)
        }
    }

    override fun onDispose() {
        if(enabled) {
            physicsRenderer.disposeSafely()
            shapeRenderer.disposeSafely()
        }
    }
}
