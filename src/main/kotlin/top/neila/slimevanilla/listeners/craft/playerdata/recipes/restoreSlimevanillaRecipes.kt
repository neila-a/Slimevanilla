package top.neila.slimevanilla.listeners.craft.playerdata.recipes

import org.bukkit.entity.HumanEntity

fun HumanEntity.restoreSlimevanillaRecipes() {
    val playerVanillaDiscoveredRecipe = playerVanillaDiscoveredRecipes[uniqueId] ?: return
    undiscoverAllRecipes()
    discoverRecipes(playerVanillaDiscoveredRecipe)
}
