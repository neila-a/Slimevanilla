package top.neila.slimevanilla.core

import top.neila.slimevanilla.listeners.craft.CraftListener
import top.neila.slimevanilla.listeners.addrecipe.AddRecipeListener
import org.bukkit.plugin.java.JavaPlugin

const val originalBugTrackerURL = "https://github.com/neila-a/Slimevanilla/issues"

class Slimevanilla : JavaPlugin(), SlimevanillaBase {
    companion object {
        var instance: Slimevanilla? = null
    }

    override fun getJavaPlugin() = this
    override fun getBugTrackerURL() = originalBugTrackerURL

    override fun onEnable() {
        Slimevanilla.instance = this

        translatePlugin()
        CraftListener()
        AddRecipeListener()
    }
}
