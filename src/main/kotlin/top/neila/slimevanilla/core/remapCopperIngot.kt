package top.neila.slimevanilla.core

import io.github.thebusybiscuit.slimefun4.api.items.ItemState
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * 把 Slimefun 自定义「铜锭」（COPPER_INGOT，材质为 BRICK）就地改造成原版铜锭  
 * （Material.COPPER_INGOT）。所有配方、机器与粘液指南书都持有该对象引用，  
 * 因此改一处即可全局生效，指南书也会显示原版铜锭。
 */
fun remapCopperIngot() {
    val original = SlimefunItems.COPPER_INGOT
    val vanilla = ItemStack(Material.COPPER_INGOT)

    /*
     * 遍历所有 Slimefun 物品，把机器配方（getRecipes）与指南书展示配方（recipe 字段）中
     * 引用 COPPER_INGOT 的元素全部替换为原版铜锭。这样：
     *  - 机器产出入/产出变为原版铜锭，玩家手里的原版铜锭与之完全互通；
     *  - 粘液指南书显示原版铜锭；
     *  - COPPER_INGOT 这个 Slimefun 物品本身保留 id，不会被置 null（否则 Slimefun 加载崩溃）。 
     */
    for (item in Slimefun.getRegistry().allSlimefunItems) {
        try {
            val recipeField = SlimefunItem::class.java.getDeclaredField("recipe").apply { isAccessible = true }
            val recipeArr = recipeField.get(item) as? Array<*>
            if (recipeArr != null) {
                for (i in recipeArr.indices) {
                    if (java.lang.reflect.Array.get(recipeArr, i) == original) {
                        java.lang.reflect.Array.set(recipeArr, i, vanilla)
                    }
                }
            }
        } catch (_: Exception) {
        }

        try {
            val recipes = item.javaClass.getMethod("getRecipes").invoke(item) as? List<*>
            recipes?.forEach { pair ->
                val arr = pair as? Array<*> ?: return@forEach
                for (i in arr.indices) {
                    if (java.lang.reflect.Array.get(arr, i) == original) {
                        java.lang.reflect.Array.set(arr, i, vanilla)
                    }
                }
            }
        } catch (_: Exception) {
        }

        /*
         * 电动机器（AContainer 系）的配方存在 List<MachineRecipe> 中，指南书经 getDisplayRecipes()
         * 实时从内部 MachineRecipe 的 input/output 数组转换显示；上面的 getRecipes() 只是转换副本，
         * 必须直接改 MachineRecipe 内部的 input/output 数组才能生效。
         */
        try {
            val machineRecipes = item.javaClass.getMethod("getMachineRecipes").invoke(item) as? List<*>
            machineRecipes?.forEach { recipe ->
                val cls = recipe?.javaClass ?: return@forEach
                val input = cls.getMethod("getInput").invoke(recipe) as? Array<*>
                if (input != null) {
                    for (i in input.indices) {
                        if (java.lang.reflect.Array.get(input, i) == original) java.lang.reflect.Array.set(
                            input,
                            i,
                            vanilla
                        )
                    }
                }
                val output = cls.getMethod("getOutput").invoke(recipe) as? Array<*>
                if (output != null) {
                    for (i in output.indices) {
                        if (java.lang.reflect.Array.get(output, i) == original) java.lang.reflect.Array.set(
                            output,
                            i,
                            vanilla
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    /*
     * 彻底移除粘液指南书中的「铜锭」条目：通过 Slimefun 的配置禁用机制把它标记为禁用，
     * 使其 isDisabled() 返回 true（指南书据此不显示该物品）。
     */
    SlimefunItem.getById("COPPER_INGOT")?.let { item ->
        // 1) 持久化到 Items.yml（符合 Slimefun 的配置禁用语义）
        Slimefun.getItemCfg().setValue("COPPER_INGOT.enabled", false)
        // 2) state 在 register() 时计算、load() 不会重算，故反射置为 DISABLED 让 isDisabled() 立即生效
        SlimefunItem::class.java.getDeclaredField("state").apply { isAccessible = true }
            .set(item, ItemState.DISABLED)
        // 3) 同时从所在 ItemGroup 移除条目（覆盖指南书直接遍历 ItemGroup 的实现路径）
        item.itemGroup.remove(item)
    }
}
