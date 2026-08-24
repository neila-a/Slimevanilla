package top.neila.slimevanilla

import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.ItemStack

val emptyMatrix = arrayOfNulls<ItemStack>(9)

val toFlatItemStacks = { itemStacks: Array<ItemStack?> ->
    itemStacks.map { stack ->
        if (stack == null) return@map null
        val flatStack = stack.clone()
        flatStack.amount = 1
        return@map flatStack
    }
}

class SlimevanillaListener : Listener {
    @EventHandler
    fun onPrepareItemCraft(event: PrepareItemCraftEvent) {
        val inventory = event.inventory
        val matrix = inventory.matrix

        if (matrix.contentEquals(emptyMatrix)) return

        val flatMatrix = toFlatItemStacks(matrix)
        Slimefun.getRegistry().enabledSlimefunItems.filterNotNull().forEach { item ->
            if (item.recipeType != RecipeType.ENHANCED_CRAFTING_TABLE) return@forEach

            if (toFlatItemStacks(item.recipe) == flatMatrix) {
                inventory.result = item.recipeOutput
            }
        }
    }
}
