package top.neila.slimevanilla.listeners.addrecipe

import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe

class ShapedRecipeBuilder(key: NamespacedKey, output: ItemStack, input: Array<ItemStack?>) : ShapedRecipe(key, output) {
    init {
        val rows = Array(3) { StringBuilder() }
        val ingredientIndex = linkedMapOf<String, ItemStack>()

        for (i in 0..8) {
            val slot = input[i]
            val r = i / 3
            if (slot == null) {
                rows[r].append(' ')
                continue
            }
            val ch = ingredientIndex.keys.firstOrNull { SlimefunUtils.isItemSimilar(ingredientIndex[it], slot, false) }
                ?: ('A' + ingredientIndex.size).toString().also { ingredientIndex[it] = slot }
            rows[r].append(ch)
        }

        shape(rows[0].toString(), rows[1].toString(), rows[2].toString())
        ingredientIndex.forEach { (ch, ing) -> setIngredient(ch[0], ing) }
    }
}