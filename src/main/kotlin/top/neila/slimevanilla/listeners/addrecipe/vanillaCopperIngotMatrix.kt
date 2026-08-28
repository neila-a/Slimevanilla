package top.neila.slimevanilla.listeners.addrecipe

import org.bukkit.inventory.ItemStack

fun Array<ItemStack?>.vanillaCopperIngotMatrix(): Array<ItemStack?> =
    map { it?.vanillaCopperIngot() }.toTypedArray()
