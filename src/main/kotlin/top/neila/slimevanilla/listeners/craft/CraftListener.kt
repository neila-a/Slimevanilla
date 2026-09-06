package top.neila.slimevanilla.listeners.craft

import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent
import io.github.thebusybiscuit.slimefun4.api.events.MultiBlockInteractEvent
import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlockMachine
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority.*
import org.bukkit.event.Listener
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem.getByItem
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import top.neila.slimevanilla.core.Slimevanilla
import top.neila.slimevanilla.defines.multiBlockTitleKeyMap
import top.neila.slimevanilla.defines.recipetypes.multiBlockToRecipeTypeMap
import top.neila.slimevanilla.listeners.craft.playerdata.opening.close
import top.neila.slimevanilla.listeners.craft.playerdata.opening.open
import top.neila.slimevanilla.listeners.craft.playerdata.opening.opening
import top.neila.slimevanilla.listeners.craft.playerdata.recipes.discoverSlimevanillaRecipes
import top.neila.slimevanilla.listeners.craft.playerdata.recipes.restoreSlimevanillaRecipes
import top.neila.slimevanilla.listeners.craft.playerdata.timetocraft.cancelTimeToCraftTask
import top.neila.slimevanilla.listeners.craft.runnables.craftitem.CraftItemRunnable
import top.neila.slimevanilla.listeners.craft.runnables.playerrecipebookclick.PlayerRecipeBookClickRunnable
import top.neila.slimevanilla.listeners.craft.runnables.prepareitemcraft.PrepareItemCraftRunnable
import top.neila.slimevanilla.listeners.recipeKeyAt
import top.neila.slimevanilla.listeners.recipeList

class CraftListener : Listener {
    init {
        Slimevanilla.server.pluginManager.registerEvents(this, Slimevanilla)
    }

    @EventHandler(priority = LOW)
    fun onMultiBlockInteract(event: MultiBlockInteractEvent) {
        val multiBlock = event.multiBlock
        val player = event.player
        val item = multiBlock.slimefunItem
        if (item !is MultiBlockMachine) return

        val type = multiBlockToRecipeTypeMap[item::class]

        player.toSlimefun { profile ->
            if (item.research != null && !profile.hasUnlocked(item.research)) return@toSlimefun

            if (type != null) {
                event.isCancelled = true

                /*
                 * 直接遍历该机器 getRecipes() 原始列表（[输入, 输出, ...]），
                 * 解锁其中「无科研」或「科研已解锁」的配方。
                 * 用原始输入索引 i 作为 key，与 addSlimefunRecipes 注册时完全一致，
                 * 且能正确解锁同一机器同一产物的多个不同输入配方（如压缩机：煤矿块×8→碳×9 与 煤炭×8→碳×1）。
                 */
                player.discoverSlimevanillaRecipes(buildList {
                    val recipes = item.recipeList
                    for (i in recipes.indices step 2) {
                        val output = recipes[i + 1].firstOrNull() ?: continue
                        /*
                         * 产出如果是原版物品（非 Slimefun 物品），其 SlimefunItem 为 null，
                         * 视为「无科研限制」直接解锁，而不是跳过——否则所有原版产出配方
                         * （如磨石：1 烈焰棒 -> 4 烈焰粉）都永远不被解锁、在配方书里不显示。
                         */
                        val outputItem = getByItem(output)
                        val research = outputItem?.research
                        if (research == null || profile.hasUnlocked(research)) {
                            add(item recipeKeyAt  i)
                        }
                    }
                })

                val view = player.openWorkbench(event.clickedBlock.location, true)
                val titleKey = "container.${multiBlockTitleKeyMap[item::class]}.title"
                val title = titleKey.translated
                if (title != titleKey)
                    view?.title = title
                if (view != null)
                    player.open(item, view)
            }
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player
        player.cancelTimeToCraftTask()
        if (player.opening == null) return
        player.restoreSlimevanillaRecipes()
        player.close()
    }

    /*
     * io.github.thebusybiscuit.slimefun4.implementation.listeners.crafting.CraftingTableListener#onPrepareCraft priority = NORMAL
     */
    @EventHandler(priority = HIGH)
    fun onPrepareItemCraft(event: PrepareItemCraftEvent) {
        PrepareItemCraftRunnable(event)
    }

    /*
     * io.github.thebusybiscuit.slimefun4.implementation.listeners.crafting.CraftingTableListener#onCraft priority = NORMAL
     */
    @EventHandler(priority = HIGH)
    fun onCraftItem(event: CraftItemEvent) {
        CraftItemRunnable(event)
    }

    @EventHandler
    fun onPlayerRecipeBookClick(event: PlayerRecipeBookClickEvent) {
        PlayerRecipeBookClickRunnable(event)
    }
}
