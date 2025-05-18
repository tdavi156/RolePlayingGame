package com.github.jacks.roleplayinggame.components

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ai.btree.BehaviorTree
import com.badlogic.gdx.ai.btree.utils.BehaviorTreeParser
import com.github.jacks.roleplayinggame.ai.AiEntity
import com.github.quillraven.fleks.ComponentListener
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

data class AiComponent (
    val nearbyEntities : MutableSet<Entity> = mutableSetOf(),
    var treePath : String = ""
) {
    lateinit var behaviorTree: BehaviorTree<AiEntity>

    companion object {
        class AiComponentListener(
            private val world : World
        ) : ComponentListener<AiComponent> {

            private val treeParser = BehaviorTreeParser<AiEntity>()

            override fun onComponentAdded(entity: Entity, component: AiComponent) {
                component.behaviorTree = treeParser.parse(
                    Gdx.files.internal("assets/aiBehaviorTrees/" + component.treePath),
                    AiEntity(entity, world)
                )
            }

            override fun onComponentRemoved(entity: Entity, component: AiComponent) = Unit
        }
    }
}
