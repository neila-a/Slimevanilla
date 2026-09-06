package top.neila.slimevanilla.listeners.addrecipe

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem.getByItem
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.recipe.CraftingBookCategory
import org.bukkit.inventory.recipe.CraftingBookCategory.*

/**
 * 按产物所属 itemGroup 决定原版配方书的分类页，比「按机器」更准确  
 * （同一台机器可能产出不同分类的产物，如磨石既可产资源也可产建材）。  
 * 分类页（Equipment / Redstone / Building / Misc）是 1.19+ 原版配方书的概念，  
 * 只能映射到这四档，无法按每台机器单独成类。  
 */
val ItemStack.bookCategory
    get(): CraftingBookCategory {
        val groupKey = getByItem(this)?.itemGroup?.key?.key ?: return MISC
        return when (groupKey) {
            "weapons", "tools", "armor", "magical_armor", "equipment" -> EQUIPMENT
            "electricity", "androids", "cargo", "gps", "technical" -> REDSTONE
            "resources", "misc", "food", "materials" -> BUILDING
            else -> MISC
        }
    }
