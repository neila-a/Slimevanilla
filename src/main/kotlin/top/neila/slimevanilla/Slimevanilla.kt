package top.neila.slimevanilla

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemRegistryFinalizedEvent
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlockMachine
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.OreWasher
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils
import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.translation.TranslationStore
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.recipe.CraftingBookCategory
import java.util.Locale
import java.util.ResourceBundle

const val originalBugTrackerURL = "https://github.com/neila-a/Slimevanilla/issues"

class Slimevanilla : SlimevanillaBase() {
    override fun getJavaPlugin() = this
    override fun getBugTrackerURL() = originalBugTrackerURL

    /**
     * 原版配方书 key → 该配方在 Slimefun 机器中的完整输入矩阵（9 格）。
     * 注册配方时按 key 存入，供 onPlayerRecipeBookClick 直接取用，
     * 避免「按产物 id 反查 getRecipeInputs」这种脆弱、且会在同产物多配方时取错输入的逻辑。
     */
    internal val recipeInputMap = mutableMapOf<NamespacedKey, Array<ItemStack?>>()

    private fun addSlimefunRecipes() {
        val singleSlotTypes = needToCountRecipeTypes.toSet()
        var count = 0

        val machines = Slimefun.getRegistry().enabledSlimefunItems
            .filterIsInstance<MultiBlockMachine>()
            .filter { it::class in multiBlockToRecipeTypeMap }

        for (machine in machines) {
            /* 注意：MultiBlockMachine 的 recipeType 字段恒为 RecipeType.MULTIBLOCK，
             * 不能用于区分机器类型。应使用具体类在 multiBlockToRecipeTypeMap 中的分类标签。 */
            val type = multiBlockToRecipeTypeMap[machine::class] ?: continue

            /* getRecipes() 为扁平列表：[输入0, 输出0, 输入1, 输出1, ...]，
             * 偶数索引是 3x3 输入矩阵（ItemStack[9]），奇数索引是对应输出的 ItemStack[1]。
             * 用原始输入索引 i 作为配方书 key 后缀，保证每条配方唯一且注册/解锁两边一致。 */
            val recipes = recipeListOf(machine)
            for (i in recipes.indices step 2) {
                val inputMatrix = recipes[i]
                val output = recipes[i + 1].firstOrNull() ?: continue
                val key = recipeKey(machine, i)
                /* 洗矿机（OreWasher）：原版由 Sifted Ore 随机产出 9 种矿石粉之一（不可选）。
                 * 为在合成台架构下还原「随机」特性，合成时的实际产出（预览与合成结果）都改为
                 * 随机一种真矿石粉，在 SlimevanillaListener 的 onPrepareItemCraft / onCraftItem 中处理，
                 * 此处配方书注册仍用 output（作为配方书里展示的默认产物）。 */
                val effectiveOutput = output

                when {
                    type in singleSlotTypes -> {
                        /* 单格输入机器（Compressor/GrindStone/Juicer/OreCrusher/OreWasher/PressureChamber）
                         * 参考其 onInteract，取首个非空输入格。
                         * 注意：此处不能用 ShapelessRecipe，因为其单种材料 amount 上限为 9，
                         * 无法表达如硫酸盐（16 个下界岩）这类配方。改用 ShapedRecipe 放在第一格，
                         * 单格 amount 上限为 64，才能正确携带材料数量。 */
                        val input = inputMatrix.firstOrNull { it != null } ?: continue
                        val recipe = ShapedRecipe(key, effectiveOutput)
                        recipe.shape("A")
                        /* 注意：必须传 ItemStack（而非 Material），否则 Slimefun 物品的 NBT/自定义贴图会丢失，
                         * 配方书与合成网格里会变成单纯的原版材质。ItemStack 的 amount 在 ShapedRecipe
                         * 中会被 Bukkit 忽略，材料数量（如硫酸盐需 16 个下界岩）需在
                         * onPrepareItemCraft 中按实际 amount 手工匹配、在 onCraftItem 中手工扣减。 */
                        recipe.setIngredient('A', input)
                        recipe.category = bookCategoryOf(effectiveOutput)
                        recipe.group = recipeGroup(machine, inputMatrix, type)
                        if (safeAddRecipe(recipe)) count++
                    }
                    type == RecipeType.SMELTERY -> {
                        /* 熔炉：无序多格，参考 AbstractSmeltery.onInteract，注册为 ShapelessRecipe */
                        val recipe = ShapelessRecipe(key, effectiveOutput)
                        /* 必须传 ItemStack（而非 Material），否则 Slimefun 物品的 NBT/自定义贴图会丢失。
                         * 同一物品可能重复出现（如 2 个铁锭），需按 Slimefun 相似度去重后累加 amount：
                         * 用 addIngredient(ItemStack) 时其 amount 才生效（不要用 addIngredient(Material, Int)，
                         * 那里的 Int 是已废弃的 data 值，并非数量）。 */
                        val merged = mutableListOf<ItemStack>()
                        inputMatrix.filterNotNull().forEach { part ->
                            val existing = merged.firstOrNull { SlimefunUtils.isItemSimilar(it, part, false) }
                            if (existing != null) existing.amount += part.amount
                            else merged += part.clone()
                        }
                        merged.forEach { recipe.addIngredient(it) }
                        recipe.category = bookCategoryOf(effectiveOutput)
                        recipe.group = recipeGroup(machine, inputMatrix, type)
                        if (safeAddRecipe(recipe)) count++
                    }
                    else -> {
                        /* 有序多格机器（EnhancedCraftingTable/ArmorForge/MagicWorkbench）
                         * 参考 EnhancedCraftingTable.onInteract，注册为 ShapedRecipe */
                        val recipe = buildShapedRecipe(key, effectiveOutput, inputMatrix)
                        recipe.category = bookCategoryOf(effectiveOutput)
                        recipe.group = recipeGroup(machine, inputMatrix, type)
                        if (safeAddRecipe(recipe)) count++
                    }
                }
                /* 记录该 key 对应的完整输入矩阵，供 onPlayerRecipeBookClick 精确取用
                 * （直接基于玩家点击的那条配方，而非按产物反查，避免同产物多配方取错输入）。 */
                recipeInputMap[key] = inputMatrix

                /* 盐（SALT）：原版 OreWasher 存在两种合成「1 沙→盐」与「2 沙→盐」，
                 * 为还原「随机消耗 1 或 2 个沙子」特性，额外注册一条 SAND×2→SALT 配方，
                 * 使玩家放 1 沙或 2 沙都能合成盐，且两条输入原料都是沙子（group 相同）。 */
                if (machine is OreWasher && SlimefunUtils.isItemSimilar(effectiveOutput, SlimefunItems.SALT, true)) {
                    val altKey = NamespacedKey(this, "${key.key}_alt")
                    val altRecipe = org.bukkit.inventory.ShapelessRecipe(altKey, effectiveOutput)
                    altRecipe.addIngredient(org.bukkit.Material.SAND)
                    altRecipe.addIngredient(org.bukkit.Material.SAND)
                    altRecipe.category = bookCategoryOf(effectiveOutput)
                    altRecipe.group = recipeGroup(machine, arrayOf(ItemStack(org.bukkit.Material.SAND), null, null, null, null, null, null, null, null), type)
                    if (safeAddRecipe(altRecipe)) {
                        recipeInputMap[altKey] = arrayOf(ItemStack(org.bukkit.Material.SAND).apply { amount = 2 }, null, null, null, null, null, null, null, null)
                        count++
                    }
                }
            }
        }

        logger.info("Slimevanilla: 已从 multiblock 机器注册 $count 条原版配方书条目")
    }

    /**
     * 注册原版配方；若 key 已存在（理论上每条配方 key 唯一，不应发生）则跳过，
     * 避免 Bukkit 抛出 "Duplicate recipe" 异常中断整个注册流程。
     */
    private fun safeAddRecipe(recipe: org.bukkit.inventory.Recipe): Boolean {
        return try {
            server.addRecipe(recipe)
            true
        } catch (e: IllegalStateException) {
            false
        }
    }

    /**
     * 生成某机器某条配方对应的原版配方书 key。
     * 注册（addSlimefunRecipes）与解锁（onMultiBlockInteract）必须使用同一规则，
     * 否则配方书无法正确显示已解锁的配方。
     *
     * key 使用「机器标识 + 配方在 getRecipes() 扁平列表里的输入索引」。
     * 之所以不用产物 SF id，是因为：
     *  1) 同一产物可由多台机器产出（碳：压缩机 / 磨石），用产物 id 会让两台机器争抢同 key；
     *  2) 同一机器同一产物也可有多个不同输入的配方（压缩机：煤矿块×8→碳×9、
     *     煤炭×8→碳×1；磨石：钻石→碳），用产物 id 只会注册其中一条，其余被跳过，
     *     导致点开看到的是别的配方、且扣减数量取自错误配方（只扣 1 个）。
     * 用原始输入索引可保证每条配方都有唯一、稳定、与注册/解锁两边完全一致的 key。
     */
    internal fun recipeKey(machine: MultiBlockMachine, inputIndex: Int): NamespacedKey {
        val machineKey = multiBlockTitleKeyMap[machine::class] ?: machine.javaClass.simpleName.lowercase()
        return NamespacedKey(this, "${machineKey}_$inputIndex")
    }

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
    internal fun recipeListOf(machine: MultiBlockMachine): List<Array<ItemStack?>> {
        val fromMachine = machine.getRecipes()
        if (fromMachine.isNotEmpty()) return fromMachine.map { it as Array<ItemStack?> }
        val out = mutableListOf<Array<ItemStack?>>()
        for (item in Slimefun.getRegistry().enabledSlimefunItems) {
            if (item.recipeType.getMachine() == machine) {
                val input = item.recipe
                val output = item.item
                out.add(input as Array<ItemStack?>)
                out.add(arrayOf(output))
            }
        }
        return out
    }

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
    internal fun recipeGroup(machine: MultiBlockMachine, inputMatrix: Array<ItemStack?>, type: RecipeType): String {
        val machineKey = multiBlockTitleKeyMap[machine::class] ?: machine.javaClass.simpleName.lowercase()
        val signature = inputMatrix.joinToString(",") { slot ->
            if (slot == null) "0" else {
                val rawName = slot.type.name
                val normalized = if (rawName.startsWith("DEEPSLATE_")) rawName.removePrefix("DEEPSLATE_") else rawName
                "${normalized}#${slot.amount}#${slot.itemMeta?.persistentDataContainer?.hashCode() ?: 0}"
            }
        }
        return "${machineKey}_$signature"
    }

    private fun buildShapedRecipe(key: NamespacedKey, output: ItemStack, input: Array<ItemStack?>): ShapedRecipe {
        val recipe = ShapedRecipe(key, output)
        val rows = Array(3) { StringBuilder() }
        val ingredientIndex = linkedMapOf<String, ItemStack>()

        for (i in 0..8) {
            val slot = input[i]
            val r = i / 3
            if (slot == null) {
                rows[r].append(' ')
                continue
            }
            val ch = ingredientIndex.keys.firstOrNull { SlimefunUtils.isItemSimilar(ingredientIndex[it], slot, false) }
                ?: ('A' + ingredientIndex.size).toString().also { ingredientIndex[it] = slot }
            rows[r].append(ch)
        }

        recipe.shape(rows[0].toString(), rows[1].toString(), rows[2].toString())
        ingredientIndex.forEach { (ch, ing) -> recipe.setIngredient(ch[0], ing) }
        return recipe
    }

    /**
     * 按产物所属 itemGroup 决定原版配方书的分类页，比「按机器」更准确
     * （同一台机器可能产出不同分类的产物，如磨石既可产资源也可产建材）。
     * 分类页（Equipment / Redstone / Building / Misc）是 1.19+ 原版配方书的概念，
     * 只能映射到这四档，无法按每台机器单独成类。
     */
    private fun bookCategoryOf(output: ItemStack): CraftingBookCategory {
        val groupKey = SlimefunItem.getByItem(output)?.itemGroup?.key?.key ?: return CraftingBookCategory.MISC
        return when (groupKey) {
            "weapons", "tools", "armor", "magical_armor", "equipment" -> CraftingBookCategory.EQUIPMENT
            "electricity", "androids", "cargo", "gps", "technical" -> CraftingBookCategory.REDSTONE
            "resources", "misc", "food", "materials" -> CraftingBookCategory.BUILDING
            else -> CraftingBookCategory.MISC
        }
    }

    private fun translate() {
        val store = TranslationStore.messageFormat(NamespacedKey(this, "translation_store"))

        val bundle = ResourceBundle.getBundle("top.neila.slimevanilla.Bundle", Locale.SIMPLIFIED_CHINESE)
        store.registerAll(Locale.SIMPLIFIED_CHINESE, bundle, true)
        GlobalTranslator.translator().addSource(store)
    }

    override fun onEnable() {
        translate()
        SlimevanillaListener(this)

        /* Slimefun 的 multiblock 配方在其 SlimefunStartupTask（所有插件 onEnable 之后）才填充，
         * 并在填充完成后触发 SlimefunItemRegistryFinalizedEvent。必须在该事件之后才能拿到配方。
         * 注意：Slimefun 的 PostSetup.loadItems 在 finalize 事件【之后】才调用 loadSmelteryRecipes()，
         * 而该方法会把 Smeltery 的合金配方同时注入到 MakeshiftSmeltery（简易冶炼炉）。
         * 若同步在 finalize 事件中注册，简易冶炼炉的 recipes 尚为空 → 注册不到任何配方。
         * 因此延后到下一 tick 再注册，此时 loadSmelteryRecipes 已执行完毕。 */
        server.pluginManager.registerEvents(object : Listener {
            @EventHandler
            fun onRegistryFinalized(e: SlimefunItemRegistryFinalizedEvent) {
                object : org.bukkit.scheduler.BukkitRunnable() {
                    override fun run() {
                        addSlimefunRecipes()
                    }
                }.runTask(this@Slimevanilla)
            }
        }, this)
    }
}
