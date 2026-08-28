package top.neila.slimevanilla.listeners.craft

import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlockMachine
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils
import org.bukkit.inventory.ItemStack

/**
 * 参考 Slimefun 各 MultiBlockMachine 在 onInteract 时的配方匹配逻辑：
 * - 无序机器（熔炉 SMELTERY）：input 的每个材料可在工作台任意位置匹配；
 * - 其余有序机器（EnhancedCraftingTable/ArmorForge/MagicWorkbench）：逐格精确比较。
 */
fun matchesWorkbench(
    machine: MultiBlockMachine,
    matrix: Array<ItemStack?>,
    input: Array<ItemStack?>,
    type: RecipeType
): Boolean {
    if (type == RecipeType.SMELTERY) {
        /* 无序匹配：每个配方输入必须在矩阵中找到对应槽位，且矩阵中不得有未被消耗的“多余”物品。
         * 原版粘液冶炼炉会忽略多余物品（如“1 铜粉+1 铜锭”仍能合成只耗铜粉），这里禁止该行为。 */
        val used = BooleanArray(9)
        val allNeededMatched = input.filterNotNull().all { needStack ->
            matrix.withIndex().any { (i, slot) ->
                !used[i] && slot != null && SlimefunUtils.isItemSimilar(slot, needStack, true)
                    .also { if (it) used[i] = true }
            }
        }
        return allNeededMatched && matrix.withIndex().all { (i, slot) -> slot == null || used[i] }
        /* 矩阵里每一个非空槽都必须被某个配方输入消耗，否则视为“有多余物品”，不匹配。 */
    }

    for (i in 0..8) {
        if (!SlimefunUtils.isItemSimilar(matrix[i], input[i], true, true, false)) {
            return false
        }
    }
    return true
}