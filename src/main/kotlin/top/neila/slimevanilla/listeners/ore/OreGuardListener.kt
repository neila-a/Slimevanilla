package top.neila.slimevanilla.listeners.ore

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem.getByItem
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.FurnaceSmeltEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import top.neila.slimevanilla.defines.ores.oreItemIds

/**
 * 矿物的守卫：只拦原版熔炉冶炼，其它一概不管。
 *
 * 为什么要拦：矿物物品的外观复用原版材质（矿石是 IRON_ORE/COAL_ORE…，粗金属是 RAW_IRON…），
 * 而原版熔炉只按材质匹配、忽略 NBT，会把「粗锡」烧成铁锭、「锡矿石」也烧成铁锭，
 * 等于凭空产出原版矿物。粘液自己只拦工作台与锻造台
 * （CraftingTableListener / SmithingTableListener 用 isUseableInWorkbench 判断），不拦熔炉。
 *
 * 为什么不拦放置：物品是普通 SlimefunItem，粘液会在放置时把物品 id 写进 BlockStorage、
 * 破坏时关掉原版掉落并掉回物品本身，放下去再挖回来仍是同一个物品，不会刷出原版矿石
 * （充电台就能放能挖回来，走的是同一套机制）。
 */
class OreGuardListener(plugin: JavaPlugin) : Listener {
    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler(ignoreCancelled = true)
    fun onSmelt(event: FurnaceSmeltEvent) {
        if (getByItem(event.source)?.id !in oreItemIds) return

        event.isCancelled = true
        // 兜底：即便取消被忽略，也绝不能产出原版锭
        event.result = ItemStack(Material.AIR)
    }
}
