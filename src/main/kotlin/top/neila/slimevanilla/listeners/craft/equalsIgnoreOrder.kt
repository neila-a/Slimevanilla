package top.neila.slimevanilla.listeners.craft

import org.bukkit.inventory.ItemStack

fun Array<ItemStack?>.equalsIgnoreOrder(other: Array<ItemStack?>): Boolean {
    val realThis = filterNotNull()
    val realOther = other.filterNotNull()
    if (realThis.size != realOther.size) return false

    val otherSet = realOther.toSet()
    return realThis.toSet().all { thisStack ->
        otherSet.any { otherStack ->
            thisStack.amount >= otherStack.amount
                    && thisStack.isSimilar(otherStack)
        }
    }
}
