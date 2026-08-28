package top.neila.slimevanilla.listeners.craft

import org.bukkit.inventory.ItemStack

val Array<ItemStack?>.flat
    get() = map { stack ->
        if (stack == null) return@map null
        val flatStack = stack.clone()
        flatStack.amount = 1
        return@map flatStack
    }
