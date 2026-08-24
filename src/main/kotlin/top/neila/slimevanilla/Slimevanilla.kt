package top.neila.slimevanilla

import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe

const val rowWidth = 3
const val startChar = 'A'
const val originalBugTrackerURL = "https://github.com/neila-a/Slimevanilla/issues"

class Slimevanilla : SlimevanillaBase() {
    override fun getJavaPlugin() = this
    override fun getBugTrackerURL() = originalBugTrackerURL

    private fun addSlimefunRecipes() = Slimefun.getRegistry().enabledSlimefunItems.filterNotNull().forEach { item ->
        if (item.recipeType == RecipeType.ENHANCED_CRAFTING_TABLE) {
            val key = NamespacedKey(this, item.id)
            val recipe = ShapedRecipe(key, item.recipeOutput)

            val ingredients = mutableSetOf<ItemStack>()
            val rows = arrayOf("", "", "")
            var currentRow = 0
            for (ingredient in item.recipe) {
                if (rows[currentRow].length == rowWidth) {
                    currentRow++
                }
                if (ingredient == null) {
                    rows[currentRow] += " "
                } else {
                    if (ingredients.contains(ingredient)) {
                        rows[currentRow] += startChar + ingredients.indexOf(ingredient)
                    } else {
                        ingredients.add(ingredient)
                        rows[currentRow] += startChar + ingredients.indexOf(ingredient)
                    }
                }
            }
            recipe.shape(rows[0], rows[1], rows[2])
            ingredients.forEachIndexed { index, ingredient ->
                recipe.setIngredient(startChar + index, ingredient)
            }

            server.addRecipe(recipe)
        }
    }

    override fun onEnable() {
        addSlimefunRecipes()

        server.pluginManager.registerEvents(SlimevanillaListener(this), this)
    }
}
