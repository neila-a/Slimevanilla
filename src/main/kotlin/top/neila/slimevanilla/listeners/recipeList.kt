package top.neila.slimevanilla.listeners

import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlockMachine
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun
import org.bukkit.inventory.ItemStack

/**
 * 取得某台 multiblock 机器的「输入/输出」配方对列表（与 MultiBlockMachine#getRecipes()
 * 相同的扁平格式：[输入0, 输出0, 输入1, 输出1, ...]，偶数索引是 3x3 输入矩阵 ItemStack[9]，
 * 奇数索引是对应输出的 ItemStack[1]）。
 *
 * 大多数机器直接来自 getRecipes()。但榨汁机（Juicer）是特例：它的配方并不存放在
 * Juicer#getRecipes() 中，而是由每个 Juice 物品注册时通过「recipeType == JUICER」注入到
 * Juicer 的 getRecipes()（见 Slimefun 的 RecipeType#register）；若注册顺序导致此时
 * getRecipes() 为空，则配方书完全不显示（实际合成仍由原版 MultiBlockMachineListener 处理，
 * 故「合成正常但配方书不显示」）。
 * 因此当 getRecipes() 为空时，兜底改为遍历所有 SlimefunItem，收集 recipeType 指向本机器的项，
 * 以其注册输入矩阵为 input、自身物品为 output，保证配方书能正确注册与解锁。
 */
val MultiBlockMachine.recipeList
    get(): List<Array<ItemStack?>> {
        val fromMachine = getRecipes()
        if (fromMachine.isNotEmpty()) return fromMachine.map { it as Array<ItemStack?> }
        val out = mutableListOf<Array<ItemStack?>>()
        for (item in Slimefun.getRegistry().enabledSlimefunItems) {
            if (item.recipeType.machine == this) {
                val input = item.recipe
                val output = item.item
                out.add(input as Array<ItemStack?>)
                out.add(arrayOf(output))
            }
        }
        return out
    }
