package top.neila.slimevanilla.listeners.craft.playerdata.recipes

import org.bukkit.entity.HumanEntity

internal fun HumanEntity.undiscoverAllRecipes() = undiscoverRecipes(discoveredRecipes)
