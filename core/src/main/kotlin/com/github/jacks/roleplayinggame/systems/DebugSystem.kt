package com.github.jacks.roleplayinggame.systems

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.IntervalSystem
import ktx.assets.disposeSafely

class DebugSystem(
    private val physicsWorld : World,
    private val stage : Stage
) : IntervalSystem(enabled = false) {

    private val lazyPhysicsRenderer = lazy { Box2DDebugRenderer() }
    private val lazyShapeRenderer = lazy { ShapeRenderer() }
    private val physicsRenderer by lazyPhysicsRenderer
    private val shapeRenderer by lazyShapeRenderer

    override fun onTick() {
        physicsRenderer.render(physicsWorld, stage.camera.combined)
    }

    override fun onDispose() {
        if (lazyPhysicsRenderer.isInitialized()) physicsRenderer.disposeSafely()
        if (lazyShapeRenderer.isInitialized()) shapeRenderer.disposeSafely()
    }
}
