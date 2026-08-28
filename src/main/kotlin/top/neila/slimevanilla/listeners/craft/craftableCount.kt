package top.neila.slimevanilla.listeners.craft

import org.bukkit.inventory.ItemStack
import kotlin.math.min

/* 计算在当前网格下，按 inputMatrix（单份配方输入）最多可以合成多少次。 */
fun craftableCount(input: Array<ItemStack?>?, grid: Array<ItemStack?>): Int {
    if (input == null) return 0
    var n = Int.MAX_VALUE
    for (slot in grid.indices) {
        if (slot >= input.size) break
        val need = input[slot]?.amount ?: continue
        if (need <= 0) continue
        val have = grid[slot]?.amount ?: 0
        n = min(n, have / need)
        if (n == 0) return 0
    }
    return if (n == Int.MAX_VALUE) 0 else n
}
