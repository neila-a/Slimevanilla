package top.neila.slimevanilla

const val rowWidth = 3
const val startChar = 'A'

class Slimevanilla : SlimevanillaBase() {
    override fun getJavaPlugin() = this
    override fun getBugTrackerURL() = "https://github.com/neila-a/Slimevanilla/issues"

    override fun onEnable() {
        server.pluginManager.registerEvents(SlimevanillaListener(), this)
    }
}
