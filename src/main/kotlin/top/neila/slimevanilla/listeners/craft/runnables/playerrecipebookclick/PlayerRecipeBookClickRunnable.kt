package top.neila.slimevanilla.listeners.craft.runnables.playerrecipebookclick

import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent
import org.bukkit.scheduler.BukkitRunnable
import top.neila.slimevanilla.core.pluginInstance
import top.neila.slimevanilla.defines.recipetypes.lists.needToCountRecipeTypes
import top.neila.slimevanilla.defines.recipetypes.multiBlockToRecipeTypeMap
import top.neila.slimevanilla.listeners.addrecipe.recipeInputMap
import top.neila.slimevanilla.listeners.craft.playerdata.opening.opening
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem.getByItem
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.ItemStack
import kotlin.math.min

class PlayerRecipeBookClickRunnable(val event: PlayerRecipeBookClickEvent) : BukkitRunnable() {
    init {
        playerRecipeBookClick()
    }

    private fun playerRecipeBookClick() {
        if (event.isMakeAll) {
            // Used all items already
            return
        }
        /*
         * 单格机器判断不能用 result.recipeType（单格机器产物注册时 recipeType 恒为 MULTIBLOCK），
         * 改从 playerOpening 拿到当前机器类判断。
         */
        val opening = event.player.opening ?: return
        val machine = opening.first
        val isSingleSlot = multiBlockToRecipeTypeMap[machine::class] in needToCountRecipeTypes
        /*
         * 多格机器：原版配方书点击已按形状正确放置各材料（amount=1），
         * 数量匹配由 onPrepareItemCraft / onCraftItem 处理，此处无需补格。
         */
        if (isSingleSlot) {
            runTask(pluginInstance)
        }
    }

    /*
     * 原版配方书点击只会往网格放置 1 个（ShapedRecipe 的 ingredient amount 被忽略）。
     * PlayerRecipeBookClickEvent 在客户端实际把物品放入网格之前触发，此时直接修改网格会被覆盖，
     * 因此延后到下一 tick（主线程）再写回：此刻服务端库存已包含客户端放置的 1 个，
     * 将其数量补到完整 recipeAmount，并从背包扣除差额。
     */
    override fun run() {
        val recipeKey = event.recipe
        /*
         * 直接取注册时记录的输入矩阵，避免「按产物反查 getRecipeInputs」这种
         * 在同产物多配方（如压缩机：煤矿块×8→碳×9 与 煤炭×8→碳×1）时会取错输入的脆弱逻辑。
         */
        val inputMatrix = recipeInputMap[recipeKey] ?: return
        val result = getByItem(pluginInstance.server.getRecipe(recipeKey)?.result) ?: return
        /*
         * 取出该配方需要的材料（类型+数量）
         */
        val ingredients: List<ItemStack> = inputMatrix.filterNotNull().map { it.clone() }

        /*
         * 单格机器：配方只含一种输入，取第一个非空即可
         */
        val convert = ingredients.firstOrNull() ?: return
        if (convert.amount <= 1) return
        val requiredType = convert.type
        val needAmount = convert.amount
        val player = event.player

        val view = player.opening?.second ?: return
        val top = view.topInventory as CraftingInventory
        /*
         * 单格机器配方只含一种输入，pattern "A" 即第 0 格；找该材料所在格
         */
        val idx = (0 until top.size).firstOrNull { i ->
            val s = top.getItem(i)
            s != null && s.type == requiredType
        } ?: return
        val slot = top.getItem(idx) ?: return
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
    }
}
    