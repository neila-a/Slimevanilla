package top.neila.slimevanilla.listeners.craft

import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * 从玩家背包消耗 1 点打火石（FLINT_AND_STEEL）耐久，模仿原版冶炼炉「自动打火机」点火室的行为。
 * 仅在背包里有非 unbreakable 的打火石时消耗；打火石耐久耗尽则移除。
 * 返回是否成功消耗（无打火石返回 false）。
 */
fun Player.consumeFlintAndSteel(): Boolean {
    val idx = inventory.contents.indexOfFirst { it != null && it.type == Material.FLINT_AND_STEEL }
    if (idx < 0) return false
    val item = inventory.getItem(idx) ?: return false
    val meta = item.itemMeta
    if (meta == null || meta.isUnbreakable) return false
    val damageable = meta as? org.bukkit.inventory.meta.Damageable ?: return false
    val newDamage = damageable.damage + 1
    if (newDamage >= item.type.maxDurability) {
        inventory.clear(idx)
    } else {
        damageable.damage = newDamage
        item.itemMeta = damageable
    }
    return true
}
