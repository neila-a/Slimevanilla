package top.neila.slimevanilla.items

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType.ENHANCED_CRAFTING_TABLE
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType.ORE_CRUSHER
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.OreCrusher
import org.bukkit.inventory.ItemStack
import top.neila.slimevanilla.core.Slimevanilla
import top.neila.slimevanilla.defines.ores.Metal
import top.neila.slimevanilla.defines.ores.oreItem
import top.neila.slimevanilla.defines.ores.rawBlockItem
import top.neila.slimevanilla.defines.ores.rawItem

/** 单格输入机器（碎矿机等）的输入矩阵：只占第一格，其余留空 */
private fun singleSlot(input: ItemStack) = arrayOfNulls<ItemStack>(9).also { it[0] = input }

/** 碎矿机「矿石 → 金属粉」的产出数量：开启「矿石双倍」时为 2，否则为 1（与粘液的铁矿石→铁粉一致） */
private val oreDustAmount
    get() = (SlimefunItem.getById("ORE_CRUSHER") as? OreCrusher)?.let {
        if (it.isOreDoublingEnabled) 2 else 1
    } ?: 1

/**
 * 把「产出是已有粘液物品」的配方手动注入机器。
 *
 * 这些配方的产物（金属粉）或输入（粗金属块）已经是无主的粘液物品，没法再注册成新物品来
 * 顺带注册配方，只能直接调用 [io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType.register]
 * 加进机器。该方法内部要通过 id 找到机器，因此必须在粘液注册完成之后调用。
 *
 * 必须在 AddRecipeRunnable 之前调用：AddRecipeRunnable 会遍历机器现有配方生成原版配方，
 * 晚一步就注册不进玩家的原版配方书。
 *
 * 已知限制：粘液自身会在「矿石双倍」开关变化时动态改写铁粉配方的产出，我们这里只在注册时
 * 读一次，运行中改动该开关不会同步本插件已注册的产出数量（重启后生效）。
 */
fun registerOreRecipes() {
    val amount = oreDustAmount

    for (metal in Metal.entries) {
        // 增强工作台：粗金属块拆解回 9 个粗金属（合成的反向配方已由物品自身携带）
        ENHANCED_CRAFTING_TABLE.register(singleSlot(metal.rawBlockItem), SlimefunItemStack(metal.rawItem, 9))

        // 碎矿机：矿石 → 金属粉 ×N；粗金属 → 金属粉 ×1
        ORE_CRUSHER.register(singleSlot(metal.oreItem), SlimefunItemStack(metal.dust, amount))
        ORE_CRUSHER.register(singleSlot(metal.rawItem), SlimefunItemStack(metal.dust, 1))
    }

    Slimevanilla.logger.info(
        "已注入 ${Metal.entries.size} 种金属矿物配方：增强工作台（粗金属↔粗金属块）、碎矿机（矿石→粉尘×$amount、粗金属→粉尘×1）"
    )
}
