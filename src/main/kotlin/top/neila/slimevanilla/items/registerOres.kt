package top.neila.slimevanilla.items

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType.ENHANCED_CRAFTING_TABLE
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType.NULL
import org.bukkit.inventory.ItemStack
import top.neila.slimevanilla.core.Slimevanilla
import top.neila.slimevanilla.defines.ores.Metal
import top.neila.slimevanilla.defines.ores.oreId
import top.neila.slimevanilla.defines.ores.oreItem
import top.neila.slimevanilla.defines.ores.oreItemGroup
import top.neila.slimevanilla.defines.ores.rawBlockId
import top.neila.slimevanilla.defines.ores.rawBlockItem
import top.neila.slimevanilla.defines.ores.rawId
import top.neila.slimevanilla.defines.ores.rawItem

/**
 * 注册六种金属的「矿石 / 粗金属 / 粗金属块」共 18 个物品。
 *
 * 必须使用普通的 [SlimefunItem]：粘液会在放置时把物品 id 写进 BlockStorage、
 * 破坏时关掉原版掉落并掉回物品本身，因此物品放下去再挖回来仍是原物品，
 * 不会刷出原版矿石。实现 NotPlaceable（或用 UnplaceableBlock）会破坏这个机制。
 *
 * 在 onEnable 中调用：粘液的启动任务在所有插件 onEnable 之后才统一 load()，
 * 届时 [SlimefunItem.load] 会把粗金属块的配方注册进增强工作台这台机器。
 */
fun registerOres() {
    for (metal in Metal.entries) {
        // 矿石与粗金属没有合成来源（由挖矿获得），用无配方注册
        SlimefunItem(oreItemGroup, metal.oreItem, NULL, emptyArray()).register(Slimevanilla)
        SlimefunItem(oreItemGroup, metal.rawItem, NULL, emptyArray()).register(Slimevanilla)

        /*
         * 粗金属块：9 个粗金属在增强工作台合成。
         * SlimefunItem#load() 会调用 RecipeType#register，后者把配方加进增强工作台机器
         * （MultiBlockMachine#addRecipe），Slimevanilla 的 AddRecipeRunnable 随后会自动
         * 把它注册成原版配方书条目，无需我们额外处理。
         */
        SlimefunItem(
            oreItemGroup,
            metal.rawBlockItem,
            ENHANCED_CRAFTING_TABLE,
            Array<ItemStack>(9) { metal.rawItem }
        ).register(Slimevanilla)

        Slimevanilla.logger.info("已注册${metal.displayName}的矿物：${metal.oreId} / ${metal.rawId} / ${metal.rawBlockId}")
    }
}
