package top.neila.slimevanilla

import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.plugin.java.JavaPlugin

class Slimevanilla : JavaPlugin() {
    override fun onEnable() {
        val key = NamespacedKey(this, "television")

        val item = ItemStack.of(Material.BLACK_WOOL)
        item.setData(DataComponentTypes.ITEM_NAME, Component.text("Television"))

        val recipe = ShapedRecipe(key, item)
        recipe.shape("AAA", "ABA", "AAA")
        recipe.setIngredient('A', Material.WHITE_CONCRETE)
        recipe.setIngredient('B', Material.BLACK_STAINED_GLASS_PANE)

        server.addRecipe(recipe)
    }
}
