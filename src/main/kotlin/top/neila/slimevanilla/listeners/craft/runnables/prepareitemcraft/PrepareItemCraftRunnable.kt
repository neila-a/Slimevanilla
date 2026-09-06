package top.neila.slimevanilla.listeners.craft.runnables.prepareitemcraft

import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import org.bukkit.entity.HumanEntity
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem.getByItem
import org.bukkit.event.inventory.PrepareItemCraftEvent
import top.neila.slimevanilla.listeners.craft.playerdata.opening.*
import org.bukkit.Sound.BLOCK_ANVIL_USE
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.ItemStack
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils.isItemSimilar
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems.SIFTED_ORE
import org.bukkit.scheduler.BukkitRunnable
import top.neila.slimevanilla.defines.recipetypes.lists.needToCountRecipeTypes
import top.neila.slimevanilla.defines.recipetypes.multiBlockToRecipeTypeMap
import top.neila.slimevanilla.listeners.craft.toSlimefun
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType.*
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.OreWasher
import org.bukkit.entity.Player
import top.neila.slimevanilla.core.Slimevanilla
import top.neila.slimevanilla.defines.recipetypes.lists.needTimeToCraftTypes
import top.neila.slimevanilla.listeners.craft.playerdata.timetocraft.isTimeToCrafting
import top.neila.slimevanilla.listeners.craft.playerdata.timetocraft.pendTimeToCraftTask

class PrepareItemCraftRunnable(
    event: PrepareItemCraftEvent
) : BukkitRunnable() {
    val player: HumanEntity
    val inventory: CraftingInventory = event.inventory

    init {
        val players = inventory.viewers.filterNotNull()
        this.player = players.component1()
        prepareItemCraft()
    }

    private fun prepareItemCraft() {
        val matrix = inventory.matrix
        if (matrix.contentEquals(emptyMatrix)) return

        val opening = player.opening ?: return
        inventory.result = null

        val machine = opening.first
        /*
         * MultiBlockMachine.recipeType 恒为 MULTIBLOCK，需以具体类在映射中的分类标签判断
         */
        val type = multiBlockToRecipeTypeMap[machine::class] ?: return

        player.toSlimefun { profile ->
            /*
             * 参考各 MultiBlockMachine 在 onInteract 时的匹配逻辑
             */
            val output = if (needToCountRecipeTypes.contains(type)) {
                /*
                 * 单格输入机器（Compressor/GrindStone/Juicer/OreCrusher/OreWasher/PressureChamber）
                 * 使用 getRecipeInputs 遍历每个配方的首个输入格。
                 * 参考 Compressor.onInteract：遍历容器任意位置匹配，且需 amount 达到配方要求数。
                 * 因此这里也遍历整个合成矩阵（任意格），而非只看 matrix[0]。
                 */
                var matched: ItemStack? = null
                for (convert in RecipeType.getRecipeInputs(machine)) {
                    if (convert == null) continue
                    val slot = matrix.firstOrNull {
                        it != null && isItemSimilar(
                            it,
                            convert,
                            true
                        ) && it.amount >= convert.amount
                    }
                    if (slot != null) {
                        matched = getRecipeOutput(machine, convert)
                        break
                    }
                }
                matched
            } else {
                /*
                 * 多格输入机器：使用 getRecipeInputList 遍历完整输入矩阵
                 */
                var matched: ItemStack? = null
                for (input in getRecipeInputList(machine)) {
                    if (matchesWorkbench(machine, matrix, input, type)) {
                        matched = getRecipeOutputList(machine, input)
                        break
                    }
                }
                matched
            }

            if (output == null) return@toSlimefun

            /*
             * 洗矿机（OreWasher）由 Sifted Ore 合成时还原原版「随机产出 9 种矿石粉之一」特性：
             * 每次准备（预览）都随机给一种真矿石粉。
             */
            val effectiveOutput = if (machine is OreWasher
                && isItemSimilar(matrix.firstOrNull { it != null }, SIFTED_ORE, true)
            ) machine.getRandomDust() else output

            val research = getByItem(effectiveOutput)?.research
            if (research != null && !profile.hasUnlocked(research)) return@toSlimefun

            if (needTimeToCraftTypes.contains(type)) {
                /*
                 * time to craft：还原原版「需等待一段时间后才能拿取产物」的特性。
                 * 延迟到达前先把 inventory.result 设为最终成品（让玩家在合成台看到产物），
                 * 但此时尚未到时间，点击拿取会在 CraftItemEvent 中被 DENY 并提示；
                 * 到点（约 3 秒）后解除禁止，玩家才可真正拿取。
                 */
                inventory.result = effectiveOutput
                player pendTimeToCraftTask  runTaskLater(Slimevanilla, 60L)
            } else {
                inventory.result = effectiveOutput
            }
        }
    }

    /**
     * time to craft：延迟（约 3 秒）后解除「禁止拿取」状态。
     * onPrepareItemCraft 已提前把成品设入 inventory.result 并显示，但此时 pending 任务存在，
     * 玩家点击会在 CraftItemEvent 中被 DENY；本任务到点后移除 pending（解除禁止）并播放音效，
     * 玩家随后即可真正拿取已显示着的产物。
     * 去重：若已开始为某玩家计时（pending 任务存在）则不重复重置，否则 PrepareItemCraftEvent
     * 反复触发会不断取消旧任务、永远到不了 60 tick，导致产物始终处于「禁止拿取」。
     */
    override fun run() {
        val pid = player.uniqueId
        if (player.isTimeToCrafting()) return

        val p = player as? Player ?: return
        if (!p.isOnline) return
        if (player.opening == null) return
        p.world.playSound(p.location, BLOCK_ANVIL_USE, 1.0f, 1.0f)
    }
}
