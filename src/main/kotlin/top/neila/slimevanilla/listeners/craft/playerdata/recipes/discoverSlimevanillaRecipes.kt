package top.neila.slimevanilla.listeners.craft.playerdata.recipes

import org.bukkit.NamespacedKey
import org.bukkit.entity.HumanEntity

fun HumanEntity.discoverSlimevanillaRecipes(slimevanillaRecipes: Collection<NamespacedKey>) {
    playerVanillaDiscoveredRecipes[uniqueId] = discoveredRecipes
    undiscoverAllRecipes()
    discoverRecipes(slimevanillaRecipes)
}
