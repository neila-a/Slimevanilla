package top.neila.slimevanilla.defines.recipetypes.lists

import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType

/**
 * 有「time to craft」延迟产出特性的机器（还原原版盔甲锻造台：点击合成后延迟约 3 秒才给产物并播放音效）。
 * 仅用于有序多格机器（网格与配方一一对应），便于按位置扣减材料。
 */
val needTimeToCraftTypes = arrayOf(
    RecipeType.ARMOR_FORGE,
    RecipeType.MAGIC_WORKBENCH
)
