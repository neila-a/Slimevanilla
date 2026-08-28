package top.neila.slimevanilla.listeners.craft

import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent
import io.github.thebusybiscuit.slimefun4.api.events.MultiBlockInteractEvent
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlockMachine
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.OreWasher
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils
import kotlin.math.min
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.HumanEntity
import org.bukkit.event.Event.Result
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack
import top.neila.slimevanilla.Slimevanilla
import top.neila.slimevanilla.defines.recipetypes.multiBlockToRecipeTypeMap
import top.neila.slimevanilla.defines.multiBlockTitleKeyMap
import top.neila.slimevanilla.defines.recipetypes.lists.needTimeToCraftTypes
import top.neila.slimevanilla.defines.recipetypes.lists.needToCountRecipeTypes
import top.neila.slimevanilla.listeners.addrecipe.recipeInputMap
import top.neila.slimevanilla.listeners.getRecipeKey
import top.neila.slimevanilla.listeners.recipeList
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import java.util.function.Consumer

class CraftListener : Listener {
    init {
        val instance = Slimevanilla.instance
        instance?.server?.pluginManager?.registerEvents(this, instance)
    }

    private val playerVanillaDiscoveredRecipes = mutableMapOf<UUID, Collection<NamespacedKey>>()
    private val playerOpening = mutableMapOf<UUID, Pair<MultiBlockMachine, InventoryView>?>()

    /* time to craft：按玩家记录挂起的「延迟显示产物」任务，避免每次 PrepareItemCraft 重复调度 */
    private val pendingTimeToCraft = mutableMapOf<UUID, org.bukkit.scheduler.BukkitTask>()

    @EventHandler(priority = EventPriority.LOW)
    fun onMultiBlockInteract(event: MultiBlockInteractEvent) {
        val instance = Slimevanilla.instance ?: return

        val multiBlock = event.multiBlock
        val player = event.player
        val item = multiBlock.slimefunItem
        if (item !is MultiBlockMachine) return

        val type = multiBlockToRecipeTypeMap[item::class]

        player.toSlimefun { profile ->
            if (item.research != null && !profile.hasUnlocked(item.research)) return@toSlimefun

            if (type != null) {
                event.isCancelled = true

                playerVanillaDiscoveredRecipes[player.uniqueId] = player.discoveredRecipes
                player.undiscoverRecipes(player.discoveredRecipes)
                /* 直接遍历该机器 getRecipes() 原始列表（[输入, 输出, ...]），
                 * 解锁其中「无科研」或「科研已解锁」的配方。
                 * 用原始输入索引 i 作为 key，与 addSlimefunRecipes 注册时完全一致，
                 * 且能正确解锁同一机器同一产物的多个不同输入配方（如压缩机：煤矿块×8→碳×9 与 煤炭×8→碳×1）。 */
                player.discoverRecipes(buildList {
                    val recipes = item.recipeList
                    for (i in recipes.indices step 2) {
                        val output = recipes[i + 1].firstOrNull() ?: continue
                        /* 产出如果是原版物品（非 Slimefun 物品），其 SlimefunItem 为 null，
                         * 视为「无科研限制」直接解锁，而不是跳过——否则所有原版产出配方
                         * （如磨石：1 烈焰棒 -> 4 烈焰粉）都永远不被解锁、在配方书里不显示。 */
                        val outputItem = SlimefunItem.getByItem(output)
                        val research = outputItem?.research
                        if (research == null || profile.hasUnlocked(research)) {
                            add(item.getRecipeKey(i))
                        }
                    }
                })

                val view = player.openWorkbench(event.clickedBlock.location, true)

                val titleKey = "container.${multiBlockTitleKeyMap[item::class]}.title"
                val title = titleKey.translated
                if (title != titleKey)
                    view?.title = title
                if (view != null)
                    playerOpening[player.uniqueId] = item to view
            }
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player
        /* 关闭合成界面时取消挂起的 time to craft 延迟任务，避免对失效库存设 result */
        pendingTimeToCraft.remove(player.uniqueId)?.cancel()
        if (playerOpening[player.uniqueId] == null) return
        val playerVanillaDiscoveredRecipe = playerVanillaDiscoveredRecipes[player.uniqueId] ?: return
        player.undiscoverRecipes(player.discoveredRecipes)
        player.discoverRecipes(playerVanillaDiscoveredRecipe)
        playerOpening.remove(player.uniqueId)
    }

    /**
     * time to craft：延迟（约 3 秒）后解除「禁止拿取」状态。
     * onPrepareItemCraft 已提前把成品设入 inventory.result 并显示，但此时 pending 任务存在，
     * 玩家点击会在 CraftItemEvent 中被 DENY；本任务到点后移除 pending（解除禁止）并播放音效，
     * 玩家随后即可真正拿取已显示着的产物。
     * 去重：若已开始为某玩家计时（pending 任务存在）则不重复重置，否则 PrepareItemCraftEvent
     * 反复触发会不断取消旧任务、永远到不了 60 tick，导致产物始终处于「禁止拿取」。
     */
    private fun scheduleTimeToCraftResult(
        player: HumanEntity,
        inventory: CraftingInventory,
        output: ItemStack
    ) {
        val pid = player.uniqueId
        if (pendingTimeToCraft.containsKey(pid)) return
        val instance = Slimevanilla.instance ?: return
        val task = Bukkit.getScheduler().runTaskLater(instance, Runnable {
            pendingTimeToCraft.remove(pid)
            val p = player as? org.bukkit.entity.Player ?: return@Runnable
            if (!p.isOnline) return@Runnable
            if (playerOpening[pid] == null) return@Runnable
            p.world.playSound(p.location, Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f)
        }, 60L)
        pendingTimeToCraft[pid] = task
    }

    /* io.github.thebusybiscuit.slimefun4.implementation.listeners.crafting.CraftingTableListener#onPrepareCraft priority = NORMAL */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPrepareItemCraft(event: PrepareItemCraftEvent) {
        val inventory = event.inventory

        val matrix = inventory.matrix
        if (matrix.contentEquals(emptyMatrix)) return

        val players = inventory.viewers.filterNotNull()
        if (players.isEmpty()) return
        val player = players.component1()

        val opening = playerOpening[player.uniqueId] ?: return
        inventory.result = null

        val machine = opening.first
        /* MultiBlockMachine.recipeType 恒为 RecipeType.MULTIBLOCK，需以具体类在映射中的分类标签判断 */
        val type = multiBlockToRecipeTypeMap[machine::class] ?: return

        player.toSlimefun { profile ->
            /* 参考各 MultiBlockMachine 在 onInteract 时的匹配逻辑 */
            val output = if (needToCountRecipeTypes.contains(type)) {
                /* 单格输入机器（Compressor/GrindStone/Juicer/OreCrusher/OreWasher/PressureChamber）
                 * 使用 RecipeType.getRecipeInputs 遍历每个配方的首个输入格。
                 * 参考 Compressor.onInteract：遍历容器任意位置匹配，且需 amount 达到配方要求数。
                 * 因此这里也遍历整个合成矩阵（任意格），而非只看 matrix[0]。 */
                var matched: ItemStack? = null
                for (convert in RecipeType.getRecipeInputs(machine)) {
                    if (convert == null) continue
                    val slot = matrix.firstOrNull {
                        it != null && SlimefunUtils.isItemSimilar(
                            it,
                            convert,
                            true
                        ) && it.amount >= convert.amount
                    }
                    if (slot != null) {
                        matched = RecipeType.getRecipeOutput(machine, convert)
                        break
                    }
                }
                matched
            } else {
                /* 多格输入机器：使用 RecipeType.getRecipeInputList 遍历完整输入矩阵 */
                var matched: ItemStack? = null
                for (input in RecipeType.getRecipeInputList(machine)) {
                    if (matchesWorkbench(machine, matrix, input, type)) {
                        matched = RecipeType.getRecipeOutputList(machine, input)
                        break
                    }
                }
                matched
            }

            if (output == null) return@toSlimefun

            /* 洗矿机（OreWasher）由 Sifted Ore 合成时还原原版「随机产出 9 种矿石粉之一」特性：
             * 每次准备（预览）都随机给一种真矿石粉。 */
            val effectiveOutput = if (machine is OreWasher
                && SlimefunUtils.isItemSimilar(matrix.firstOrNull { it != null }, SlimefunItems.SIFTED_ORE, true)
            ) machine.getRandomDust() else output

            val research = SlimefunItem.getByItem(effectiveOutput)?.research
            if (research != null && !profile.hasUnlocked(research)) return@toSlimefun

            if (needTimeToCraftTypes.contains(type)) {
                /* time to craft：还原原版「需等待一段时间后才能拿取产物」的特性。
                 * 延迟到达前先把 inventory.result 设为最终成品（让玩家在合成台看到产物），
                 * 但此时尚未到时间，点击拿取会在 CraftItemEvent 中被 DENY 并提示；
                 * 到点（约 3 秒）后解除禁止，玩家才可真正拿取。 */
                inventory.result = effectiveOutput
                scheduleTimeToCraftResult(player, inventory, effectiveOutput)
            } else {
                inventory.result = effectiveOutput
            }
        }
    }

    /* io.github.thebusybiscuit.slimefun4.implementation.listeners.crafting.CraftingTableListener#onCraft priority = NORMAL */
    @EventHandler(priority = EventPriority.HIGH)
    fun onCraftItem(event: CraftItemEvent) {
        val result = event.inventory.result
        if (result != null) {
            /* If it's really using slimefun items to craft vanilla items
             * result will be null.
             * And if result isn't null
             * It's crafting with/to slimefun items. */
            event.result = Result.ALLOW
            event.isCancelled = false
        }

        val item = SlimefunItem.getByItem(result) ?: return
        val opening = playerOpening[event.whoClicked.uniqueId] ?: return
        val machine = opening.first
        val type = multiBlockToRecipeTypeMap[machine::class] ?: return
        val player = event.whoClicked as? org.bukkit.entity.Player ?: return
        val grid = event.inventory.matrix
        val isShift = event.isShiftClick
        val sifted = machine is OreWasher
                && SlimefunUtils.isItemSimilar(grid.firstOrNull { it != null }, SlimefunItems.SIFTED_ORE, true)

        val recipeKeyNs = when (val r = event.recipe) {
            is org.bukkit.inventory.ShapedRecipe -> r.key
            is org.bukkit.inventory.ShapelessRecipe -> r.key
            else -> null
        }
        val inputMatrix = recipeKeyNs?.let { recipeInputMap[it] }

        /* time to craft（盔甲锻造台 / 魔法工作台）：还原原版「需等待一段时间后才能拿取」的特性。
         * onPrepareItemCraft 已把成品设入 inventory.result 并显示，但等待期间 pending 任务存在，
         * 此时玩家点击拿取应被拒绝（DENY + 提示）；等待结束（pending 已移除）后才允许真正拿取。 */
        if (type in needTimeToCraftTypes) {
            if (pendingTimeToCraft.containsKey(player.uniqueId)) {
                /* 尚未到时间：禁止拿取并提示玩家等待。产物仍显示在合成台，但不会被取走，
                 * 也不会扣减材料（event 已 DENY）。 */
                event.result = Result.DENY
                event.isCancelled = true
                player.sendMessage("message.time_to_craft.waiting".translated)
                return
            }
            /* 已到时间：允许拿取。普通点击交给原版（开头已 ALLOW，扣 1 份材料给 1 份产物）；
             * Shift 合成则完全接管：取消即时单份，改为延迟一次性给 n 份（n = 可合成次数）
             * 并手动从网格扣除 n 份材料。 */
            if (isShift) {
                val n = if (inputMatrix != null) craftableCount(inputMatrix, grid) else 1
                event.result = Result.DENY
                event.isCancelled = true
                deductMaterials(inputMatrix, grid, n)
                /* 批量接管产出，清掉合成台残留的成品预览（网格已扣空）。 */
                event.inventory.result = null
                val output = result?.clone() ?: return
                output.amount = 1
                val instance = Slimevanilla.instance ?: return
                Bukkit.getScheduler().runTaskLater(instance, Runnable {
                    repeat(n) { player.inventory.addItem(output.clone()) }
                    player.world.playSound(player.location, org.bukkit.Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f)
                }, 0L)
            }
            return
        }

        if (type == RecipeType.SMELTERY && !isShift) {
            if (!player.smelteryIgniteOnce()) {
                event.result = Result.DENY
                event.isCancelled = true
                player.sendMessage("message.smeltery.need_flint".translated)
                return
            }
        }

        /* 洗矿机（OreWasher）由 Sifted Ore 合成时，实际产物也随机给一种真矿石粉
         * （还原原版「随机产出 9 种矿石粉之一」特性；预览见 onPrepareItemCraft）。
         * 放在单格扣减逻辑之前，使最终放入背包的 finalResult 为本次随机的矿粉。 */
        if (machine is OreWasher && sifted && !isShift) {
            event.inventory.result = machine.getRandomDust()
        }

        /* 单格输入机器（amount 可能 >1，如硫酸盐需 16 下界岩）需要按完整配方数扣减。
         * 不能用 item.recipeType 判断：单格机器的 recipeType 字段恒为 RecipeType.MULTIBLOCK，
         * 会导致部分产物（如硫酸盐）误判而不扣减，原版只扣 1 个。这里改为依据当前机器
         * 是否属于单格机器类集合来判断（machine 来自 playerOpening，已确定是单格机器）。 */
        val isSingleSlot = multiBlockToRecipeTypeMap[machine::class] in needToCountRecipeTypes
        if (isSingleSlot && !isShift) {
            /* CraftItemEvent 触发时原版已按 recipe ingredient（amount=1）扣减了 1 个，
             * 因此 matrix 里剩余数量比完整配方数少 1，这里匹配时不再要求 amount 达标，
             * 只按类型相似找到实际用于合成的输入配方，再以其 amount 决定扣减数量。
             * 单格机器可能存在「多个不同输入产出同一产物」的配方（如压缩机：1 钻石 -> 1 碳、
             * 8 煤炭 -> 1 碳）。必须根据当前矩阵里实际匹配到的输入来扣减，不能固定取 item.recipe[0]。
             * 优先用点击/注册时记录的输入矩阵取该配方真正的输入（精确，避免同产物多配方取错）；
             * 找不到时再退回到按当前矩阵反查机器配方（并保留 null 防护，避免误扣背包成品）。 */
            val fromMap = inputMatrix
                ?.firstOrNull {
                    it != null && grid.firstOrNull { m ->
                        m != null && SlimefunUtils.isItemSimilar(
                            m,
                            it,
                            false
                        )
                    } != null
                }
            val matchedConvert: ItemStack? = fromMap
                ?: RecipeType.getRecipeInputs(machine).firstOrNull { c ->
                    c != null && grid.firstOrNull { it != null && SlimefunUtils.isItemSimilar(it, c, false) } != null
                }
            /* 找不到对应的输入配方时，绝不执行扣减：
             * 否则会回退到 item.recipe[0] 的数量，并扣减网格里第一个非空格
             * （可能是玩家误放的成品），从而把背包里已有的成品错误清除。 */
            val recipeAmount = matchedConvert?.amount ?: return
            if (recipeAmount < 1) return
            val inventoryIngredient =
                grid.firstOrNull { it != null && SlimefunUtils.isItemSimilar(it, matchedConvert, false) } ?: return
            inventoryIngredient.amount -= recipeAmount - 1
        }

        /* Shift 合成（合成全部）：CraftItemEvent 在 Shift 下只触发一次，原版只产出 1 份。
         * 这里完全接管批量：取消即时产物，按可合成次数 n 循环产出 n 份（洗矿机每次随机、
         * 冶炼炉每次独立 34% 打火石），并从网格扣除 n 份材料。 */
        if (isShift) {
            event.result = Result.DENY
            event.isCancelled = true
            if (inputMatrix == null) return
            val n = craftableCount(inputMatrix, grid)
            if (n <= 0) return
            deductMaterials(inputMatrix, grid, n)
            /* Shift 合成完全接管批量产出，已 DENY 原版产物并手动扣减/发放，
             * 必须清掉 inventory.result，否则成品会残留在合成台预览（网格已空却仍显示成品）。 */
            event.inventory.result = null
            val baseOutput = result?.clone()?.apply { amount = 1 } ?: return
            repeat(n) {
                if (type == RecipeType.SMELTERY && !player.smelteryIgniteOnce()) {
                    player.sendMessage("message.smeltery.need_flint.skipped".translated)
                    return@repeat
                }
                val out = if (sifted) machine.getRandomDust() else baseOutput.clone()
                val leftover = player.inventory.addItem(out)
                if (leftover.isNotEmpty()) player.world.dropItemNaturally(player.location, out)
            }
        }
    }

    @EventHandler
    fun onPlayerRecipeBookClick(event: PlayerRecipeBookClickEvent) {
        if (event.isMakeAll) {
            // Used all items already
            return
        }
        val recipeKey = event.recipe
        /* 直接取注册时记录的输入矩阵，避免「按产物反查 getRecipeInputs」这种
         * 在同产物多配方（如压缩机：煤矿块×8→碳×9 与 煤炭×8→碳×1）时会取错输入的脆弱逻辑。 */
        val instance = Slimevanilla.instance ?: return
        val inputMatrix = recipeInputMap[recipeKey] ?: return
        val result = SlimefunItem.getByItem(instance.server.getRecipe(recipeKey)?.result) ?: return
        /* 单格机器判断不能用 result.recipeType（单格机器产物注册时 recipeType 恒为 MULTIBLOCK），
         * 改从 playerOpening 拿到当前机器类判断。 */
        val opening = playerOpening[event.player.uniqueId] ?: return
        val machine = opening.first
        val isSingleSlot = multiBlockToRecipeTypeMap[machine::class] in needToCountRecipeTypes
        /* 取出该配方需要的材料（类型+数量） */
        val ingredients: List<ItemStack> = inputMatrix.filterNotNull().map { it.clone() }
        if (isSingleSlot) {
            /* 单格机器：配方只含一种输入，取第一个非空即可 */
            val convert = ingredients.firstOrNull() ?: return
            if (convert.amount <= 1) return
            val requiredType = convert.type
            val needAmount = convert.amount
            val player = event.player
            /* 原版配方书点击只会往网格放置 1 个（ShapedRecipe 的 ingredient amount 被忽略）。
             * PlayerRecipeBookClickEvent 在客户端实际把物品放入网格之前触发，此时直接修改网格会被覆盖，
             * 因此延后到下一 tick（主线程）再写回：此刻服务端库存已包含客户端放置的 1 个，
             * 将其数量补到完整 recipeAmount，并从背包扣除差额。 */
            val instance = Slimevanilla.instance ?: return
            Bukkit.getScheduler().runTask(instance, Runnable {
                val view = playerOpening[player.uniqueId]?.second ?: return@Runnable
                val top = view.topInventory as CraftingInventory
                /* 单格机器配方只含一种输入，pattern "A" 即第 0 格；找该材料所在格 */
                val idx = (0 until top.size).firstOrNull { i ->
                    val s = top.getItem(i)
                    s != null && s.type == requiredType
                } ?: return@Runnable
                val slot = top.getItem(idx) ?: return@Runnable
                var extra = needAmount - slot.amount
                if (extra > 0) {
                    for (b in view.bottomInventory.contents) {
                        if (extra <= 0) break
                        if (b != null && b.type == requiredType && b.amount > 0) {
                            val take = min(extra, b.amount)
                            b.amount -= take
                            extra -= take
                        }
                    }
                }
                val placed = slot.clone()
                placed.amount = needAmount - extra
                top.setItem(idx, placed)
            })
        }
        /* 多格机器：原版配方书点击已按形状正确放置各材料（amount=1），
         * 数量匹配由 onPrepareItemCraft / onCraftItem 处理，此处无需补格。 */
    }
}
