package top.neila.slimevanilla

import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.ItemStack
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile

val emptyMatrix = arrayOfNulls<ItemStack>(9)

val Array<ItemStack?>.flat
    get() = map { stack ->
        if (stack == null) return@map null
        val flatStack = stack.clone()
        flatStack.amount = 1
        return@map flatStack
    }

class SlimevanillaListener : Listener {
    @EventHandler
    fun onPrepareItemCraft(event: PrepareItemCraftEvent) {
        val inventory = event.inventory

        val matrix = inventory.matrix
        if (matrix.contentEquals(emptyMatrix)) return

        val players = inventory.viewers.filterNotNull()
        if (players.isEmpty()) return
        val player = players.component1()
        PlayerProfile.fromUUID(player.uniqueId) { profile ->
            val flatMatrix = matrix.flat
            Slimefun.getRegistry().enabledSlimefunItems.filterNotNull().forEach { item ->
                if (item.recipeType != RecipeType.ENHANCED_CRAFTING_TABLE) return@forEach

                val research = item.research ?: return@forEach
                if (!profile.hasUnlocked(research)) return@forEach

                if (item.recipe.flat == flatMatrix) {
                    inventory.result = item.recipeOutput
                }
            }
        }
    }
}
