package top.neila.slimevanilla.listeners.addrecipe

import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * 把 Slimefun 自定义「铜锭」（SlimefunItems.COPPER_INGOT，材质为 BRICK）替换为原版铜锭
 * （Material.COPPER_INGOT），使这些配方在原版合成体系中以原版铜锭作为输入/产出，
 * 玩家在合成台放置原版铜锭即可匹配。
 */
fun ItemStack.vanillaCopperIngot(): ItemStack =
    if (SlimefunUtils.isItemSimilar(this, SlimefunItems.COPPER_INGOT, false)) ItemStack(Material.COPPER_INGOT)
    else this
