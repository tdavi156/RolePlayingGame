package com.github.jacks.roleplayinggame.configurations

data class BattleEnchantmentItemData(
    val id: Int,
    val itemName: String,
    val uiAtlasKey: String,
    val itemDescription: String,
    val statType: BattleEnchantmentStatType,
    val statValue: Int,
    val goldValue: Int = 0,
    val isSellable: Boolean = false,
)

val BattleEnchantmentItems: List<BattleEnchantmentItemData> = listOf(
    // BattleEnchantmentItemData(
    //     id = 4000,
    //     itemName = "Item Name",
    //     uiAtlasKey = "atlas_key",
    //     itemDescription = "Passive bonus description.",
    //     statType = BattleEnchantmentStatType.ATTACK,
    //     statValue = 5
    // ),
    BattleEnchantmentItemData(4001, "War Sigil",      "sword2", "Increases attack during battle.",  BattleEnchantmentStatType.ATTACK,  5),
    BattleEnchantmentItemData(4002, "Iron Ward",      "sword2", "Increases defense during battle.", BattleEnchantmentStatType.DEFENSE, 3),
    BattleEnchantmentItemData(4003, "Vitality Stone", "sword2", "Increases max health in battle.",  BattleEnchantmentStatType.HEALTH,  15),
)

fun battleEnchantmentItemById(id: Int): BattleEnchantmentItemData? = BattleEnchantmentItems.find { it.id == id }
