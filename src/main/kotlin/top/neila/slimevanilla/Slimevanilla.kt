package top.neila.slimevanilla

const val rowWidth = 3
const val startChar = 'A'
const val originalBugTrackerURL = "https://github.com/neila-a/Slimevanilla/issues"

class Slimevanilla : SlimevanillaBase() {
    override fun getJavaPlugin() = this
    override fun getBugTrackerURL() = originalBugTrackerURL

    override fun onEnable() {
        server.pluginManager.registerEvents(SlimevanillaListener(), this)
    }
}
