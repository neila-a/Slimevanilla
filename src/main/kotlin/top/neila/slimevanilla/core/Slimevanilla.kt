package top.neila.slimevanilla.core

import top.neila.slimevanilla.listeners.craft.CraftListener
import top.neila.slimevanilla.listeners.addrecipe.AddRecipeListener
import top.neila.slimevanilla.listeners.ore.OreGuardListener
import top.neila.slimevanilla.items.registerOres
import org.bukkit.plugin.java.JavaPlugin

object Slimevanilla : JavaPlugin(), SlimevanillaBase {
    override fun getJavaPlugin() = this
    override fun getBugTrackerURL() = "https://github.com/neila-a/Slimevanilla/issues"

    override fun onEnable() {
        translatePlugin()
        registerOres()
        CraftListener(this)
        AddRecipeListener(this)
        OreGuardListener(this)
    }
}
