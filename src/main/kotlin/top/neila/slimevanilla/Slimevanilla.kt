package top.neila.slimevanilla

import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun
import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.translation.TranslationStore
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.recipe.CraftingBookCategory
import java.util.Locale
import java.util.ResourceBundle

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

            recipe.group = item.itemGroup.unlocalizedName
            when (item.itemGroup.key.key) {
                "weapons", "tools", "armor", "magical_armor"
                    -> recipe.category = CraftingBookCategory.EQUIPMENT

                "electricity", "androids", "cargo", "gps"
                    -> recipe.category = CraftingBookCategory.REDSTONE
            }
            server.addRecipe(recipe)
        }
    }

    private fun translate() {
        val store = TranslationStore.messageFormat(NamespacedKey(this, "translation_store"))

        val bundle = ResourceBundle.getBundle("top.neila.slimevanilla.Bundle", Locale.SIMPLIFIED_CHINESE)
        store.registerAll(Locale.SIMPLIFIED_CHINESE, bundle, true)
        GlobalTranslator.translator().addSource(store)
    }

    override fun onEnable() {
        translate()
        addSlimefunRecipes()
        
        SlimevanillaListener(this)
    }
}
