package top.neila.slimevanilla.listeners.addrecipe

import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlockMachine
import org.bukkit.inventory.ItemStack
import top.neila.slimevanilla.defines.multiBlockTitleKeyMap

/**
 * 计算某条配方的原版配方书 group（CraftingRecipe#group）。
 * 只有「配方完全相同」（即输入矩阵逐格一致：相同位置、相同物品、相同数量、相同 NBT）
 * 的 recipe 才归入同一 group。因此 group 由「机器标识 + 输入矩阵精确签名」构成，
 * 签名对 9 个槽位逐一比较，包含物品 id、数量与 NBT，而非仅按原料名排序聚合
 * （后者会把同材料不同形状、不同数量或不同 NBT 的配方错误归并到同一组）。
 *
 * 特例：深层矿石（DEEPSLATE_*_ORE 等）与普通矿石（*_ORE）视为同一类原料，
 * 将其材质名归一化（去掉 DEEPSLATE_ 前缀）后再参与签名，使「深层钻石矿石→产物」
 * 与「钻石矿石→产物」这两条配方归入同一 group，在配方书里归并显示。
 */
fun recipeGroup(machine: MultiBlockMachine, inputMatrix: Array<ItemStack?>, type: RecipeType): String {
    val machineKey = multiBlockTitleKeyMap[machine::class] ?: machine.javaClass.simpleName.lowercase()
    val signature = inputMatrix.joinToString(",") { slot ->
        if (slot == null) "0" else {
            val rawName = slot.type.name
            val normalized = if (rawName.startsWith("DEEPSLATE_")) rawName.removePrefix("DEEPSLATE_") else rawName
            "${normalized}#${slot.amount}#${slot.itemMeta?.persistentDataContainer.hashCode()}"
        }
    }
    return "${machineKey}_$signature"
}
