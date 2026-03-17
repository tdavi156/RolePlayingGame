package com.github.jacks.roleplayinggame.configurations

data class ShopConfig(
    val shopId: Int,
    val shopName: String,
    val equipmentIds: List<Int>,
    val consumableIds: List<Int>,
    val questIds: List<Int>,
    val enchantmentIds: List<Int>,
)

val OLDMAN_SHOP = ShopConfig(
    shopId = 1,
    shopName = "Old Man's Shop",
    equipmentIds = listOf(1001, 1002, 1003, 1004, 1005),
    consumableIds = listOf(2001, 2002, 2003),
    questIds = emptyList(),
    enchantmentIds = listOf(4001, 4002, 4003),
)

val ShopConfigs: List<ShopConfig> = listOf(
    OLDMAN_SHOP,
    // ShopConfig(
    //     shopId = 2,
    //     shopName = "Shop Name",
    //     equipmentIds = listOf(1001, 1002),
    //     consumableIds = listOf(2001),
    //     questIds = emptyList(),
    //     enchantmentIds = emptyList()
    // ),
)

fun shopConfigById(shopId: Int): ShopConfig? = ShopConfigs.find { it.shopId == shopId }
