package top.neila.slimevanilla.listeners.addrecipe

import org.bukkit.inventory.Recipe
import top.neila.slimevanilla.core.Slimevanilla

/**
 * 注册原版配方；若 key 已存在（理论上每条配方 key 唯一，不应发生）则跳过，  
 * 避免 Bukkit 抛出 "Duplicate recipe" 异常中断整个注册流程。
 */
fun Recipe.safeAdd(): Boolean {
    return try {
        Slimevanilla.instance?.server?.addRecipe(this)
        true
    } catch (e: IllegalStateException) {
        false
    }
}
