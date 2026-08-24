package top.neila.slimevanilla

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareItemCraftEvent

class SlimevanillaListener : Listener {
    @EventHandler
    fun onPrepareItemCraft(event: PrepareItemCraftEvent) {
        val inventory = event.inventory
        val matrix = inventory.matrix
        Slimefun.getRegistry().enabledSlimefunItems.forEach { item ->
            if (item != null) {
                if (item.recipe.contentEquals(matrix)) {
                    inventory.result = item.item
                }
            }
        }
    }
}
