package com.github.jacks.roleplayinggame.configurations

import kotlin.random.Random

// Item name key pools — add new item name keys here as items are added to Items.kt.
// To create a new pool: val MY_POOL: List<String> = listOf("Item Name", ...)

val TIER_1_ITEMS: List<String> = listOf(
    "Sword",
    "Boots",
    "Armor",
)

val TIER_2_ITEMS: List<String> = listOf(
    "Helmet",
    "Big Sword",
)

/**
 * Returns true if a drop should occur.
 * @param chance 1–100; a roll ≤ chance triggers a drop (100 = always, 0 = never).
 */
fun rollForDrop(chance: Int): Boolean = Random.nextInt(1, 101) <= chance

/**
 * Returns a uniformly random [ItemData] from the given pool of item name keys.
 * Pools should only contain names that exist in Items.kt.
 */
fun rollRandomItem(pool: List<String>): ItemData = itemByName(pool.random())!!
