package top.neila.slimevanilla.listeners.addrecipe

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemRegistryFinalizedEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import top.neila.slimevanilla.items.registerOreRecipes

/**
 * Slimefun 的 multiblock 配方在其 SlimefunStartupTask（所有插件 onEnable 之后）才填充，  
 * 并在填充完成后触发 SlimefunItemRegistryFinalizedEvent。必须在该事件之后才能拿到配方。  
 * 注意：Slimefun 的 PostSetup.loadItems 在 finalize 事件【之后】才调用 loadSmelteryRecipes()，  
 * 而该方法会把 Smeltery 的合金配方同时注入到 MakeshiftSmeltery（简易冶炼炉）。  
 * 若同步在 finalize 事件中注册，简易冶炼炉的 recipes 尚为空 → 注册不到任何配方。  
 * 因此延后到下一 tick 再注册，此时 loadSmelteryRecipes 已执行完毕。   
 *
 * 本插件矿物的机器配方（如「矿石 → 金属粉」）不是新物品的配方，需要手动调用
 * RecipeType#register 注入；因为该方法要按 id 找机器，只能等到粘液注册完成的此刻才有效，
 * 且必须在 AddRecipeRunnable 之前完成，否则不会被转成原版配方书条目。
 */
class AddRecipeListener(private val plugin: JavaPlugin) : Listener {
    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onRegistryFinalized(event: SlimefunItemRegistryFinalizedEvent) {
        registerOreRecipes()
        AddRecipeRunnable(plugin)
    }
}
