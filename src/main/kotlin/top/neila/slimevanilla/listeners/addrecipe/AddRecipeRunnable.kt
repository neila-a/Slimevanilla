package top.neila.slimevanilla.listeners.addrecipe

import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlockMachine
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.OreWasher
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.scheduler.BukkitRunnable
import top.neila.slimevanilla.core.Slimevanilla
import top.neila.slimevanilla.defines.recipetypes.lists.needToCountRecipeTypes
import top.neila.slimevanilla.defines.recipetypes.multiBlockToRecipeTypeMap
import top.neila.slimevanilla.listeners.getRecipeKey
import top.neila.slimevanilla.listeners.recipeList

/**
 * 原版配方书 key → 该配方在 Slimefun 机器中的完整输入矩阵（9 格）。
 * 注册配方时按 key 存入，供 onPlayerRecipeBookClick 直接取用，
 * 避免「按产物 id 反查 getRecipeInputs」这种脆弱、且会在同产物多配方时取错输入的逻辑。
 */
val recipeInputMap = mutableMapOf<NamespacedKey, Array<ItemStack?>>()

class AddRecipeRunnable : BukkitRunnable() {
    override fun run() {
        val singleSlotTypes = needToCountRecipeTypes.toSet()

        val machines = Slimefun.getRegistry().enabledSlimefunItems
            .filterIsInstance<MultiBlockMachine>()
            .filter { it::class in multiBlockToRecipeTypeMap }

        for (machine in machines) {
            /*
             * 注意：MultiBlockMachine 的 recipeType 字段恒为 RecipeType.MULTIBLOCK，
             * 不能用于区分机器类型。应使用具体类在 multiBlockToRecipeTypeMap 中的分类标签。
             */
            val type = multiBlockToRecipeTypeMap[machine::class] ?: continue

            /*
             * getRecipes() 为扁平列表：[输入0, 输出0, 输入1, 输出1, ...]，
             * 偶数索引是 3x3 输入矩阵（ItemStack[9]），奇数索引是对应输出的 ItemStack[1]。
             * 用原始输入索引 i 作为配方书 key 后缀，保证每条配方唯一且注册/解锁两边一致。
             */
            val recipes = machine.recipeList
            for (i in recipes.indices step 2) {
                val inputMatrix = recipes[i]
                var output = recipes[i + 1].firstOrNull() ?: continue

                val key = machine.getRecipeKey(i)
                /*
                 * 洗矿机（OreWasher）：原版由 Sifted Ore 随机产出 9 种矿石粉之一（不可选）。
                 * 为在合成台架构下还原「随机」特性，合成时的实际产出（预览与合成结果）都改为
                 * 随机一种真矿石粉，在 SlimevanillaListener 的 onPrepareItemCraft / onCraftItem 中处理，
                 * 此处配方书注册仍用 output（作为配方书里展示的默认产物）。
                 */
                val effectiveOutput = output

                when (type) {
                    in singleSlotTypes -> {
                        /*
                         * 单格输入机器（Compressor/GrindStone/Juicer/OreCrusher/OreWasher/PressureChamber）
                         * 参考其 onInteract，取首个非空输入格。
                         * 注意：此处不能用 ShapelessRecipe，因为其单种材料 amount 上限为 9，
                         * 无法表达如硫酸盐（16 个下界岩）这类配方。改用 ShapedRecipe 放在第一格，
                         * 单格 amount 上限为 64，才能正确携带材料数量。
                         */
                        val input = inputMatrix.firstOrNull { it != null } ?: continue
                        val recipe = ShapedRecipe(key, effectiveOutput)
                        recipe.shape("A")
                        /*
                         * 注意：必须传 ItemStack（而非 Material），否则 Slimefun 物品的 NBT/自定义贴图会丢失，
                         * 配方书与合成网格里会变成单纯的原版材质。ItemStack 的 amount 在 ShapedRecipe
                         * 中会被 Bukkit 忽略，材料数量（如硫酸盐需 16 个下界岩）需在
                         * onPrepareItemCraft 中按实际 amount 手工匹配、在 onCraftItem 中手工扣减。
                         */
                        recipe.setIngredient('A', input)
                        recipe.category = effectiveOutput.bookCategory
                        recipe.group = recipeGroup(machine, inputMatrix, type)
                        recipe.safeAdd()
                    }

                    RecipeType.SMELTERY -> {
                        /*
                         * 熔炉：无序多格，参考 AbstractSmeltery.onInteract，注册为 ShapelessRecipe
                         */
                        val recipe = ShapelessRecipe(key, effectiveOutput)
                        /*
                         * 必须传 ItemStack（而非 Material），否则 Slimefun 物品的 NBT/自定义贴图会丢失。
                         * 同一物品可能重复出现（如 2 个铁锭），需按 Slimefun 相似度去重后累加 amount：
                         * 用 addIngredient(ItemStack) 时其 amount 才生效（不要用 addIngredient(Material, Int)，
                         * 那里的 Int 是已废弃的 data 值，并非数量）。
                         */
                        val merged = mutableListOf<ItemStack>()
                        inputMatrix.filterNotNull().forEach { part ->
                            val existing = merged.firstOrNull { SlimefunUtils.isItemSimilar(it, part, false) }
                            if (existing != null) existing.amount += part.amount
                            else merged += part.clone()
                        }
                        merged.forEach { recipe.addIngredient(it) }
                        recipe.category = effectiveOutput.bookCategory
                        recipe.group = recipeGroup(machine, inputMatrix, type)
                        recipe.safeAdd()
                    }

                    else -> {
                        /*
                         * 有序多格机器（EnhancedCraftingTable/ArmorForge/MagicWorkbench）
                         * 参考 EnhancedCraftingTable.onInteract，注册为 ShapedRecipe
                         */
                        val recipe = ShapedRecipeBuilder(key, effectiveOutput, inputMatrix)
                        recipe.category = effectiveOutput.bookCategory
                        recipe.group = recipeGroup(machine, inputMatrix, type)
                        recipe.safeAdd()
                    }
                }
                /*
                 * 记录该 key 对应的完整输入矩阵，供 onPlayerRecipeBookClick 精确取用
                 * （直接基于玩家点击的那条配方，而非按产物反查，避免同产物多配方取错输入）。
                 */
                recipeInputMap[key] = inputMatrix

                /*
                 * 盐（SALT）：原版 OreWasher 存在两种合成「1 沙→盐」与「2 沙→盐」，
                 * 为还原「随机消耗 1 或 2 个沙子」特性，额外注册一条 SAND×2→SALT 配方，
                 * 使玩家放 1 沙或 2 沙都能合成盐，且两条输入原料都是沙子（group 相同）。
                 */
                if (machine is OreWasher && SlimefunUtils.isItemSimilar(effectiveOutput, SlimefunItems.SALT, true)) {
                    val altKey = NamespacedKey(Slimevanilla.instance!!, "${key.key}_alt")
                    val altRecipe = ShapelessRecipe(altKey, effectiveOutput)
                    altRecipe.addIngredient(Material.SAND)
                    altRecipe.addIngredient(Material.SAND)
                    altRecipe.category = effectiveOutput.bookCategory
                    altRecipe.group = recipeGroup(
                        machine,
                        arrayOf(ItemStack(Material.SAND), null, null, null, null, null, null, null, null),
                        type
                    )
                    if (altRecipe.safeAdd()) {
                        recipeInputMap[altKey] = arrayOf(
                            ItemStack(Material.SAND).apply { amount = 2 },
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                        )
                    }
                }
            }
        }
    }
}