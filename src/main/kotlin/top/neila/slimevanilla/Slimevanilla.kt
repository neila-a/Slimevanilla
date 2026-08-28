package top.neila.slimevanilla

import top.neila.slimevanilla.listeners.craft.CraftListener
import top.neila.slimevanilla.listeners.addrecipe.AddRecipeListener

const val originalBugTrackerURL = "https://github.com/neila-a/Slimevanilla/issues"

class Slimevanilla : SlimevanillaBase() {
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
