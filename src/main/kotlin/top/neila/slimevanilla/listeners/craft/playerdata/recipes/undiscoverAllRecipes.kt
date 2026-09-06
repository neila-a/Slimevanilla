package top.neila.slimevanilla.listeners.craft.playerdata.recipes

import org.bukkit.entity.HumanEntity

fun HumanEntity.undiscoverAllRecipes() = undiscoverRecipes(discoveredRecipes)
