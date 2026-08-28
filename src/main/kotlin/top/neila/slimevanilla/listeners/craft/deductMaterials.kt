package top.neila.slimevanilla.listeners.craft

import org.bukkit.inventory.ItemStack

/**
 * 从合成网格按 inputMatrix 扣除 count 份材料。
 */
fun deductMaterials(input: Array<ItemStack?>?, grid: Array<ItemStack?>, count: Int) {
    if (input == null) return
    for (slot in grid.indices) {
        if (slot >= input.size) break
        val need = input[slot]?.amount ?: continue
        if (need <= 0) continue
        val g = grid[slot] ?: continue
        val remaining = (g.amount - need * count).coerceAtLeast(0)
        g.amount = remaining
        if (remaining <= 0) grid[slot] = null
    }
}
