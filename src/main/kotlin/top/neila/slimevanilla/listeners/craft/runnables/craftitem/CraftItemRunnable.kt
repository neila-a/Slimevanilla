package top.neila.slimevanilla.listeners.craft.runnables.craftitem

import org.bukkit.entity.Player
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.event.Event.Result.*
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem.getByItem
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.OreWasher
import top.neila.slimevanilla.defines.recipetypes.multiBlockToRecipeTypeMap
import top.neila.slimevanilla.listeners.craft.playerdata.opening.opening
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems.SIFTED_ORE
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils.isItemSimilar
import org.bukkit.inventory.*
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType.*
import org.bukkit.Sound.BLOCK_ANVIL_USE
import top.neila.slimevanilla.core.pluginInstance
import top.neila.slimevanilla.defines.recipetypes.lists.needTimeToCraftTypes
import top.neila.slimevanilla.defines.recipetypes.lists.needToCountRecipeTypes
import top.neila.slimevanilla.listeners.addrecipe.recipeInputMap
import top.neila.slimevanilla.listeners.craft.playerdata.timetocraft.isTimeToCrafting
import top.neila.slimevanilla.listeners.craft.runnables.craftitem.ignite.smelteryIgniteOnce
import top.neila.slimevanilla.listeners.craft.translated

class CraftItemRunnable(val event: CraftItemEvent) : BukkitRunnable() {
    var output: ItemStack? = null
    var player: Player? = null
    var n: Int? = null

    init {
        craftItem()
    }

    private fun craftItem() {
        val result = event.inventory.result
        if (result != null) {
            /*
             * If it's really using slimefun items to craft vanilla items
             * result will be null.
             * And if result isn't null
             * It's crafting with/to slimefun items.
             */
            event.result = ALLOW
            event.isCancelled = false
        }

        val item = getByItem(result) ?: return
        val opening = event.whoClicked.opening ?: return
        val machine = opening.first
        val type = multiBlockToRecipeTypeMap[machine::class] ?: return
        val player = event.whoClicked as? Player ?: return
        val grid = event.inventory.matrix
        val isShift = event.isShiftClick
        val sifted = machine is OreWasher
                && isItemSimilar(grid.firstOrNull { it != null }, SIFTED_ORE, true)

        val recipeKeyNs = when (val r = event.recipe) {
            is ShapedRecipe -> r.key
            is ShapelessRecipe -> r.key
            else -> null
        }
        val inputMatrix = recipeKeyNs?.let { recipeInputMap[it] }

        /*
         * time to craft（盔甲锻造台 / 魔法工作台）：还原原版「需等待一段时间后才能拿取」的特性。
         * onPrepareItemCraft 已把成品设入 inventory.result 并显示，但等待期间 pending 任务存在，
         * 此时玩家点击拿取应被拒绝（DENY + 提示）；等待结束（pending 已移除）后才允许真正拿取。
         */
        if (type in needTimeToCraftTypes) {
            if (player.isTimeToCrafting()) {
                /*
                 * 尚未到时间：禁止拿取并提示玩家等待。产物仍显示在合成台，但不会被取走，
                 * 也不会扣减材料（event 已 DENY）。
                 */
                event.result = DENY
                event.isCancelled = true
                player.sendMessage("message.time_to_craft.waiting".translated)
                return
            }
            /*
             * 已到时间：允许拿取。普通点击交给原版（开头已 ALLOW，扣 1 份材料给 1 份产物）；
             * Shift 合成则完全接管：取消即时单份，改为延迟一次性给 n 份（n = 可合成次数）
             * 并手动从网格扣除 n 份材料。
             */
            if (isShift) {
                val n = if (inputMatrix != null) craftableCount(inputMatrix, grid) else 1
                event.result = DENY
                event.isCancelled = true
                deductMaterials(inputMatrix, grid, n)
                /*
                 * 批量接管产出，清掉合成台残留的成品预览（网格已扣空）。
                 */
                event.inventory.result = null
                val output = result?.clone() ?: return
                output.amount = 1
                this.output = output
                this.player = player
                this.n = n
                runTaskLater(pluginInstance, 0L)
            }
            return
        }

        if (type == SMELTERY && !isShift) {
            if (!player.smelteryIgniteOnce()) {
                event.result = DENY
                event.isCancelled = true
                player.sendMessage("message.smeltery.need_flint".translated)
                return
            }
        }

        /*
         * 洗矿机（OreWasher）由 Sifted Ore 合成时，实际产物也随机给一种真矿石粉
         * （还原原版「随机产出 9 种矿石粉之一」特性；预览见 onPrepareItemCraft）。
         * 放在单格扣减逻辑之前，使最终放入背包的 finalResult 为本次随机的矿粉。
         */
        if (machine is OreWasher && sifted && !isShift) {
            event.inventory.result = machine.getRandomDust()
        }

        /*
         * 单格输入机器（amount 可能 >1，如硫酸盐需 16 下界岩）需要按完整配方数扣减。
         * 不能用 item.recipeType 判断：单格机器的 recipeType 字段恒为 MULTIBLOCK，
         * 会导致部分产物（如硫酸盐）误判而不扣减，原版只扣 1 个。这里改为依据当前机器
         * 是否属于单格机器类集合来判断（machine 来自 playerOpening，已确定是单格机器）。
         */
        val isSingleSlot = multiBlockToRecipeTypeMap[machine::class] in needToCountRecipeTypes
        if (isSingleSlot && !isShift) {
            /*
             * CraftItemEvent 触发时原版已按 recipe ingredient（amount=1）扣减了 1 个，
             * 因此 matrix 里剩余数量比完整配方数少 1，这里匹配时不再要求 amount 达标，
             * 只按类型相似找到实际用于合成的输入配方，再以其 amount 决定扣减数量。
             * 单格机器可能存在「多个不同输入产出同一产物」的配方（如压缩机：1 钻石 -> 1 碳、
             * 8 煤炭 -> 1 碳）。必须根据当前矩阵里实际匹配到的输入来扣减，不能固定取 item.recipe[0]。
             * 优先用点击/注册时记录的输入矩阵取该配方真正的输入（精确，避免同产物多配方取错）；
             * 找不到时再退回到按当前矩阵反查机器配方（并保留 null 防护，避免误扣背包成品）。
             */
            val fromMap = inputMatrix
                ?.firstOrNull {
                    it != null && grid.firstOrNull { m ->
                        m != null && isItemSimilar(
                            m,
                            it,
                            false
                        )
                    } != null
                }
            val matchedConvert: ItemStack? = fromMap
                ?: getRecipeInputs(machine).firstOrNull { c ->
                    c != null && grid.firstOrNull {
                        it != null && isItemSimilar(
                            it,
                            c,
                            false
                        )
                    } != null
                }
            /*
             * 找不到对应的输入配方时，绝不执行扣减：
             * 否则会回退到 item.recipe[0] 的数量，并扣减网格里第一个非空格
             * （可能是玩家误放的成品），从而把背包里已有的成品错误清除。
             */
            val recipeAmount = matchedConvert?.amount ?: return
            if (recipeAmount < 1) return
            val inventoryIngredient =
                grid.firstOrNull { it != null && isItemSimilar(it, matchedConvert, false) } ?: return
            inventoryIngredient.amount -= recipeAmount - 1
        }

        /*
         * Shift 合成（合成全部）：CraftItemEvent 在 Shift 下只触发一次，原版只产出 1 份。
         * 这里完全接管批量：取消即时产物，按可合成次数 n 循环产出 n 份（洗矿机每次随机、
         * 冶炼炉每次独立 34% 打火石），并从网格扣除 n 份材料。
         */
        if (isShift) {
            event.result = DENY
            event.isCancelled = true
            if (inputMatrix == null) return
            val n = craftableCount(inputMatrix, grid)
            if (n <= 0) return
            deductMaterials(inputMatrix, grid, n)
            /*
             * Shift 合成完全接管批量产出，已 DENY 原版产物并手动扣减/发放，
             * 必须清掉 inventory.result，否则成品会残留在合成台预览（网格已空却仍显示成品）。
             */
            event.inventory.result = null
            val baseOutput = result?.clone()?.apply { amount = 1 } ?: return
            repeat(n) {
                if (type == SMELTERY && !player.smelteryIgniteOnce()) {
                    player.sendMessage("message.smeltery.need_flint.skipped".translated)
                    return@repeat
                }
                val out = if (sifted) machine.getRandomDust() else baseOutput.clone()
                val leftover = player.inventory.addItem(out)
                if (leftover.isNotEmpty()) player.world.dropItemNaturally(player.location, out)
            }
        }
    }

    override fun run() {
        repeat(n!!) { player!!.inventory.addItem(output!!.clone()) }
        player!!.world.playSound(player!!.location, BLOCK_ANVIL_USE, 1.0f, 1.0f)
    }
}
