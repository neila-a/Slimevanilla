package top.neila.slimevanilla.listeners.addrecipe

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemRegistryFinalizedEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import top.neila.slimevanilla.Slimevanilla

/**
 * Slimefun 的 multiblock 配方在其 SlimefunStartupTask（所有插件 onEnable 之后）才填充，  
 * 并在填充完成后触发 SlimefunItemRegistryFinalizedEvent。必须在该事件之后才能拿到配方。  
 * 注意：Slimefun 的 PostSetup.loadItems 在 finalize 事件【之后】才调用 loadSmelteryRecipes()，  
 * 而该方法会把 Smeltery 的合金配方同时注入到 MakeshiftSmeltery（简易冶炼炉）。  
 * 若同步在 finalize 事件中注册，简易冶炼炉的 recipes 尚为空 → 注册不到任何配方。  
 * 因此延后到下一 tick 再注册，此时 loadSmelteryRecipes 已执行完毕。   
 */
class AddRecipeListener : Listener {
    init {
        Slimevanilla.instance?.server?.pluginManager?.registerEvents(this, Slimevanilla.instance!!)
    }

    @EventHandler
    fun onRegistryFinalized(event: SlimefunItemRegistryFinalizedEvent) {
        val runnable = AddRecipeRunnable()
        runnable.runTask(Slimevanilla.instance!!)
    }
}